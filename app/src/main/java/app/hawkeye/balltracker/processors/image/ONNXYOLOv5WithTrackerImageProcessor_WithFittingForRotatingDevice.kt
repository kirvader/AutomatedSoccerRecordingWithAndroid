package app.hawkeye.balltracker.processors.image

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import app.hawkeye.balltracker.processors.imageObjectExtractors.ImageObjectsExtractor
import app.hawkeye.balltracker.processors.imageObjectExtractors.YOLOObjectExtractor
import app.hawkeye.balltracker.processors.rotate
import app.hawkeye.balltracker.processors.tiling_strategies.SquareTilingStrategy
import app.hawkeye.balltracker.processors.tracking_strategies.TrackingStrategy
import app.hawkeye.balltracker.processors.tiling_strategies.SingleSquareTiling
import app.hawkeye.balltracker.processors.toBitmap
import app.hawkeye.balltracker.processors.tracking_strategies.SingleRectFollower_RotatingCamera
import app.hawkeye.balltracker.processors.tracking_strategies.SingleRectFollower_StaticCamera
import app.hawkeye.balltracker.processors.utils.*
import app.hawkeye.balltracker.utils.createLogger

private val LOG = createLogger<ONNXYOLOv5WithTrackerImageProcessor_NoFittingForRotatingDevice>()

class ONNXYOLOv5WithTrackerImageProcessor_WithFittingForRotatingDevice(
    context: Context,
    private val getBallPositionAtTime: (Long) -> AdaptiveScreenVector?,
    private val getCurrentImageProcessingStart: () -> Long,
    private val updateUIAreaOfDetectionWithNewArea: (List<AdaptiveScreenRect>) -> Unit
) : ModelImageProcessor {

    private var trackingStrategy: TrackingStrategy

    private var squareTilingStrategy: SquareTilingStrategy

    private var objectExtractor: ImageObjectsExtractor

    private var resolution = ScreenVector(720, 1280)

    init {


        trackingStrategy = SingleRectFollower_RotatingCamera(getBallPositionAtTime)

//        trackingStrategy = SingleRectFollower_RotatingCamera(getBallScreenPositionAtTime)

        squareTilingStrategy = SingleSquareTiling()

        objectExtractor = YOLOObjectExtractor(context)
    }



    override suspend fun processImageProxy(imageProxy: ImageProxy): ClassifiedBox? {
        val wholeImageWidth = if ((imageProxy.imageInfo.rotationDegrees / 90) % 2 == 0) imageProxy.width else imageProxy.height
        val wholeImageHeight = if ((imageProxy.imageInfo.rotationDegrees / 90) % 2 == 0) imageProxy.height else imageProxy.width

        val imgBitmap = imageProxy.toBitmap()
        val rawBitmap = imgBitmap?.rotate(imageProxy.imageInfo.rotationDegrees.toFloat())
        val resultBitmap = rawBitmap?.let { Bitmap.createScaledBitmap(it, wholeImageWidth, wholeImageHeight, false) }


        val areaOfDetection = trackingStrategy.getAreaOfDetectionAtTime(getCurrentImageProcessingStart())

        val tiling = mutableListOf<ScreenRect>()
        areaOfDetection.map {
            val curTiling = squareTilingStrategy.tileRect(it, wholeImageWidth, wholeImageHeight)
            tiling.addAll(curTiling)

        }
        updateUIAreaOfDetectionWithNewArea(tiling.map { it.toAdaptiveScreenRect(wholeImageWidth, wholeImageHeight) })

        if (resultBitmap == null) {
            return null
        }
        val result = objectExtractor.extractObjects(resultBitmap, tiling, 0)

        trackingStrategy.updateLastDetectedObjectPositionAtTime(result?.adaptiveRect, getCurrentImageProcessingStart())

        return result
    }

    override fun getAreaOfDetection(): List<AdaptiveScreenRect> {
        return listOf(
            AdaptiveScreenRect(
                0f, 0f,
                640.0f / 720.0f,
                640.0f / 1280.0f
            )
        )
    }
}