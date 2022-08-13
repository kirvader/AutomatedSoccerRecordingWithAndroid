package app.hawkeye.balltracker.processors.image

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import app.hawkeye.balltracker.R
import app.hawkeye.balltracker.processors.interfaces.ModelImageProcessor
import app.hawkeye.balltracker.processors.interfaces.SegmentProcessor
import app.hawkeye.balltracker.processors.segment.ONNXYOLOSegmentProcessor
import app.hawkeye.balltracker.utils.ClassifiedBox
import app.hawkeye.balltracker.utils.ScreenPoint
import java.util.*

class ONNXYOLOv5WithTrackerImageProcessor (
    context: Context
) : ModelImageProcessor {
    private lateinit var segProcessor: SegmentProcessor

    init {
        segProcessor = ONNXYOLOSegmentProcessor(context, R.raw.yolov5s, 640)
    }
    // Get index of top 3 values
    // This is for demo purpose only, there are more efficient algorithms for topK problems
    private fun getTopDetectedObject(foundObjects: List<ClassifiedBox>): ClassifiedBox? {
        return foundObjects.maxByOrNull { it.confidence }
    }

    private val CONFIDENCE_THRESHOLD: Float = 0.3F
    private val SCORE_THRESHOLD: Float = 0.2F
    private val IMAGE_WIDTH: Int = 640
    private val IMAGE_HEIGHT: Int = 640


    override fun processImageProxy(imageProxy: ImageProxy): ClassifiedBox? {
        segProcessor.processImageSegment(imageProxy, ScreenPoint(0.5f, 0.5f))

        return null
    }
}