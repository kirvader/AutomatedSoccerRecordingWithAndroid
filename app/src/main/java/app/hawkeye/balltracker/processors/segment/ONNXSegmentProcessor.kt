package app.hawkeye.balltracker.processors.segment

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import app.hawkeye.balltracker.processors.interfaces.SegmentProcessor
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


}