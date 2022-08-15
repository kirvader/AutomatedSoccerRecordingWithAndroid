package app.hawkeye.balltracker.processors.segment

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import app.hawkeye.balltracker.processors.interfaces.SegmentProcessor
import app.hawkeye.balltracker.utils.AdaptiveRect
import app.hawkeye.balltracker.utils.ClassifiedBox
import app.hawkeye.balltracker.utils.ScreenPoint
import app.hawkeye.balltracker.utils.createLogger

private val LOG = createLogger<ONNXSegmentProcessor>()

abstract class ONNXSegmentProcessor(context: Context, modelId: Int, protected val inputImageSize: Int) : SegmentProcessor {
    protected lateinit var ortSession: OrtSession

    private fun readYoloModel(context: Context, modelId: Int): ByteArray {
        return context.resources.openRawResource(modelId).readBytes()
    }

    init {
        ortSession = OrtEnvironment.getEnvironment().createSession(readYoloModel(context, modelId))
    }

    protected fun getAbsoluteClassifiedBoxFromRelative(relativeClassifiedBox: ClassifiedBox?, relativeRectCenterPoint: ScreenPoint, imageWidth: Int, imageHeight: Int): ClassifiedBox? {
        if (relativeClassifiedBox == null) {
            return null
        }
        val absoluteRectTopLeftPoint = relativeRectCenterPoint - ScreenPoint(inputImageSize.toFloat() / imageWidth / 2, inputImageSize.toFloat() / imageHeight / 2)

        val absoluteBoxWidth = relativeClassifiedBox.adaptiveRect.width * inputImageSize / imageWidth
        val absoluteBoxHeight = relativeClassifiedBox.adaptiveRect.height * inputImageSize / imageHeight
        val absoluteBoxCenter = relativeClassifiedBox.adaptiveRect.center * Pair(inputImageSize.toFloat() / imageWidth, inputImageSize.toFloat() / imageHeight) + absoluteRectTopLeftPoint

        return ClassifiedBox(
            adaptiveRect = AdaptiveRect(absoluteBoxCenter, absoluteBoxWidth, absoluteBoxHeight),
            relativeClassifiedBox.classId, relativeClassifiedBox.confidence
        )
    }
}