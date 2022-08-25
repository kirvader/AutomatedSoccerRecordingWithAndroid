package app.hawkeye.balltracker.processors.image

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import app.hawkeye.balltracker.processors.imageObjectExtractors.ImageObjectsExtractor
import app.hawkeye.balltracker.processors.imageObjectExtractors.YOLOObjectExtractor
import app.hawkeye.balltracker.processors.rotate
import app.hawkeye.balltracker.processors.tiling_strategies.SquareTilingStrategy
import app.hawkeye.balltracker.processors.toBitmap
import app.hawkeye.balltracker.processors.tracking_strategies.SingleObjectTrackingStrategy
import app.hawkeye.balltracker.processors.utils.AdaptiveScreenRect
import app.hawkeye.balltracker.processors.utils.ClassifiedBox

abstract class OnnxYoloV5WithTrackerImageProcessor(
    context: Context,
    private val getCurrentImageProcessingStart: () -> Long,
    private val updateUIAreaOfDetectionWithNewArea: (List<AdaptiveScreenRect>) -> Unit) : ModelImageProcessor {

    abstract var singleObjectTrackingStrategy: SingleObjectTrackingStrategy

    abstract var squareTilingStrategy: SquareTilingStrategy

    protected var objectExtractor: ImageObjectsExtractor

    init {
        objectExtractor = YOLOObjectExtractor(context)
    }

    override suspend fun processImageProxy(imageProxy: ImageProxy): ClassifiedBox? {
        val imgBitmap = imageProxy.toBitmap()
        val normalImageBitmap = imgBitmap?.rotate(imageProxy.imageInfo.rotationDegrees.toFloat()) ?: return null // image bitmap when it is vertical

        val areaOfDetection = singleObjectTrackingStrategy.getAreaOfDetectionAtTime(getCurrentImageProcessingStart())

        val (newResolution, tiling) = squareTilingStrategy.tileRect(areaOfDetection, normalImageBitmap.width, normalImageBitmap.height)

        updateUIAreaOfDetectionWithNewArea(tiling.map { it.toAdaptiveScreenRect(newResolution.x, newResolution.y) })

        val resultBitmap = normalImageBitmap.let { Bitmap.createScaledBitmap(it, newResolution.x, newResolution.y, false) } ?: return null

        val result = objectExtractor.extractObjects(resultBitmap, tiling, 0)

        singleObjectTrackingStrategy.updateLastDetectedObjectPositionAtTime(result?.adaptiveRect, getCurrentImageProcessingStart())

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