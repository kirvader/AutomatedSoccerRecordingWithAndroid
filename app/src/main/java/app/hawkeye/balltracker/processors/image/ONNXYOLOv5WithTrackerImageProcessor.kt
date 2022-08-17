package app.hawkeye.balltracker.processors.image

import android.content.Context
import androidx.camera.core.ImageProxy
import app.hawkeye.balltracker.R
import app.hawkeye.balltracker.TrackingStrategyChoice
import app.hawkeye.balltracker.processors.interfaces.ModelImageProcessor
import app.hawkeye.balltracker.processors.interfaces.SegmentProcessor
import app.hawkeye.balltracker.processors.interfaces.TilingStrategy
import app.hawkeye.balltracker.processors.interfaces.TrackingStrategy
import app.hawkeye.balltracker.processors.segment.ONNXYOLOSegmentProcessor_NoScaling
import app.hawkeye.balltracker.processors.tiling_strategies.SingleRectTiling
import app.hawkeye.balltracker.processors.tracking_strategies.SingleRectFollower_StaticCamera
import app.hawkeye.balltracker.processors.utils.SegmentProcessorConfig
import app.hawkeye.balltracker.utils.AdaptiveRect
import app.hawkeye.balltracker.utils.ClassifiedBox
import app.hawkeye.balltracker.utils.AdaptiveScreenPoint
import app.hawkeye.balltracker.utils.createLogger

private val LOG = createLogger<ONNXYOLOv5WithTrackerImageProcessor>()

class ONNXYOLOv5WithTrackerImageProcessor(
    context: Context,
    getBallScreenPositionAtTime: (Long) -> AdaptiveScreenPoint?,
    private val getCurrentImageProcessingStart: () -> Long,
    private val updateUIAreaOfDetectionWithNewArea: (List<AdaptiveRect>) -> Unit
) : ModelImageProcessor {
    private var segProcessor: SegmentProcessor

    private var trackingStrategy: TrackingStrategy

    private lateinit var tilingStrategy: TilingStrategy

    init {
        segProcessor = ONNXYOLOSegmentProcessor_NoScaling(context, R.raw.yolov5s_640, 640)

        trackingStrategy = SingleRectFollower_StaticCamera(getBallScreenPositionAtTime)

        tilingStrategy = SingleRectTiling(listOf(640))
    }



    override fun processImageProxy(imageProxy: ImageProxy): ClassifiedBox? {
        val wholeImageWidth = if ((imageProxy.imageInfo.rotationDegrees / 90) % 2 == 0) imageProxy.width else imageProxy.height
        val wholeImageHeight = if ((imageProxy.imageInfo.rotationDegrees / 90) % 2 == 0) imageProxy.height else imageProxy.width

        val areaOfDetection = trackingStrategy.getAreaOfDetectionAtTime(getCurrentImageProcessingStart())

        val tiling = mutableListOf<SegmentProcessorConfig>()
        areaOfDetection.map {
            val curTiling = tilingStrategy.tileRect(it, wholeImageWidth, wholeImageHeight)
            tiling.addAll(curTiling)
            LOG.i(curTiling[0].screenPartToHandle.center.y)

        }

        updateUIAreaOfDetectionWithNewArea(tiling.map { it.screenPartToHandle.toAdaptive(wholeImageWidth, wholeImageHeight) })

        val allDetectingResults = mutableListOf<ClassifiedBox>()
        tiling.map {
            val result = segProcessor.processImageSegment(imageProxy, it.screenPartToHandle)
            if (result != null)
                allDetectingResults.add(result)
        }
        val result = allDetectingResults.maxByOrNull{ it.confidence }

        trackingStrategy.updateLastDetectedObjectPositionAtTime(result?.adaptiveRect, getCurrentImageProcessingStart())

        return result
    }

    override fun getAreaOfDetection(): List<AdaptiveRect> {
        return listOf(
            AdaptiveRect(
                AdaptiveScreenPoint(0.5f, 0.5f),
                640.0f / 720.0f,
                640.0f / 1280.0f
            )
        )
    }
}