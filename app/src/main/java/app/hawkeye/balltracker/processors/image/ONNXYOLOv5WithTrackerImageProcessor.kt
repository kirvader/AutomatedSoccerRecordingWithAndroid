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

    private var segmentProcessors: Map<Int, SegmentProcessor> = mapOf()

    private var trackingStrategy: TrackingStrategy

    private var tilingStrategy: TilingStrategy

    init {
        segmentProcessors = mapOf(
            64 to ONNXYOLOSegmentProcessor_NoScaling(context, R.raw.yolov5s_64, 64),
            128 to ONNXYOLOSegmentProcessor_NoScaling(context, R.raw.yolov5s_128, 128),
            256 to ONNXYOLOSegmentProcessor_NoScaling(context, R.raw.yolov5s_256, 256),
            512 to ONNXYOLOSegmentProcessor_NoScaling(context, R.raw.yolov5s_512, 512),
            640 to ONNXYOLOSegmentProcessor_NoScaling(context, R.raw.yolov5s_640, 640)
        )

        trackingStrategy = SingleRectFollower_StaticCamera(getBallScreenPositionAtTime)

        tilingStrategy = SingleRectTiling(segmentProcessors.keys.toList())
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
            val result = segmentProcessors[it.inputImageSize]?.processImageSegment(imageProxy, it.screenPartToHandle)
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