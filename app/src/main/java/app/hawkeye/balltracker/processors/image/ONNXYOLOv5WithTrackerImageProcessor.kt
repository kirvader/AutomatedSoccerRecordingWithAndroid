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
        segProcessor = ONNXYOLOSegmentProcessor(context, R.raw.yolov5n6_128, 128)
    }



    override fun processImageProxy(imageProxy: ImageProxy): ClassifiedBox? {
        return segProcessor.processImageSegment(imageProxy, ScreenPoint(0.5f, 0.5f))
    }
}