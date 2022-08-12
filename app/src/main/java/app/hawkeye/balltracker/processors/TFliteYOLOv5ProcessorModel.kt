package app.hawkeye.balltracker.processors

import android.content.Context
import android.graphics.*
import android.util.Half
import android.util.Size
import androidx.camera.core.ImageProxy
import androidx.core.util.toHalf
import app.hawkeye.balltracker.processors.interfaces.ModelImageProcessor
import app.hawkeye.balltracker.utils.ClassifiedBox
import app.hawkeye.balltracker.utils.createLogger
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.nnapi.NnApiDelegate

import java.nio.ByteBuffer

private val LOG = createLogger<TFliteYOLOv5ProcessorModel>()

internal class TFliteYOLOv5ProcessorModel(private val context: Context) : ModelImageProcessor {

    private lateinit var bitmapBuffer: Bitmap

    private val nnApiDelegate by lazy  {
        NnApiDelegate()
    }

    private var imageRotationDegrees: Int = 0


    private val tflite by lazy {
        loadModelFile(context.assets, MODEL_PATH)?.let {
            Interpreter(
                it,
                Interpreter.Options().addDelegate(nnApiDelegate))
        }
    }

    private val tfInputSize by lazy {
        val inputIndex = 0
        val inputShape = tflite?.getInputTensor(inputIndex)?.shape()
        Size(inputShape?.get(2) ?: 0, inputShape?.get(1) ?: 0) // Order of axis is: {1, height, width, 3}
    }


    private val CONFIDENCE_THRESHOLD: Float = 0.3F
    private val SCORE_THRESHOLD: Float = 0.2F
    private val IMAGE_WIDTH: Int = 640
    private val IMAGE_HEIGHT: Int = 640

    override fun processImageProxy(imageProxy: ImageProxy): List<ClassifiedBox> {
        if (!::bitmapBuffer.isInitialized) {
            // The image rotation and RGB image buffer are initialized only once
            // the analyzer has started running

            imageRotationDegrees = imageProxy.imageInfo.rotationDegrees
            bitmapBuffer = Bitmap.createBitmap(
                imageProxy.width * 2, imageProxy.height * 2, Bitmap.Config.RGBA_F16
            )
        }
        LOG.i(imageProxy.format)

        val imgBitmap = imageProxy.toBitmap()
        val rawBitmap =
            imgBitmap?.let { Bitmap.createScaledBitmap(it, IMAGE_WIDTH, IMAGE_HEIGHT, false) }
        val bitmap = rawBitmap?.rotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            ?: return listOf()
        val imgData = preProcessForTFLite(bitmap, IMAGE_WIDTH, IMAGE_HEIGHT)

        val input: ByteBuffer = floatToByteBuffer(imgData)

        val output = ByteBuffer.allocateDirect(4 * 25200 * 85)

        if (tflite == null) {
            return listOf()
        }
        tflite!!.run(input, output)

        val floatArr = mutableListOf<Half>()
        for (i in 0 until 25200 * 85) {
            floatArr.add((output.getShort(i * 2)).toHalf())
        }

        val res = floatArr.toMutableList().chunked(85)

        return getAllObjectsByClassForYOLOFromHalfList(
            res,
            -1,
            CONFIDENCE_THRESHOLD,
            SCORE_THRESHOLD,
            IMAGE_WIDTH,
            IMAGE_HEIGHT
        )
    }

    companion object {
        private const val MODEL_PATH = "yolov5s-fp16.tflite"
        private const val LABELS_PATH = "coco_ssd_mobilenet_v1_1.0_labels.txt"
    }
}