package app.hawkeye.balltracker.processors

import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import androidx.camera.core.ImageProxy
import app.hawkeye.balltracker.utils.ClassifiedBox
import app.hawkeye.balltracker.utils.createLogger
import kotlinx.coroutines.android.awaitFrame
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.nnapi.NnApiDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.Rot90Op

private val LOG = createLogger<TFliteYOLOv5ProcessorModel>()

internal class TFliteYOLOv5ProcessorModel(private val context: Context) : ModelImageProcessor {

    private lateinit var bitmapBuffer: Bitmap

    private val nnApiDelegate by lazy  {
        NnApiDelegate()
    }

    private val tfImageBuffer = TensorImage(DataType.UINT8)
    private var imageRotationDegrees: Int = 0


    private val tflite by lazy {
        Interpreter(
            FileUtil.loadMappedFile(context, MODEL_PATH),
            Interpreter.Options().addDelegate(nnApiDelegate))
    }

    private val tfInputSize by lazy {
        val inputIndex = 0
        val inputShape = tflite.getInputTensor(inputIndex).shape()
        Size(inputShape[2], inputShape[1]) // Order of axis is: {1, height, width, 3}
    }

    private val tfImageProcessor = ImageProcessor.Builder()

        .add(ResizeOp(
            tfInputSize.height, tfInputSize.width, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
        .add(Rot90Op(-imageRotationDegrees / 90))
        .add(NormalizeOp(0f, 1f))
            .build()

    private val CONFIDENCE_THRESHOLD: Float = 0.3F
    private val SCORE_THRESHOLD: Float = 0.2F
    private val IMAGE_WIDTH: Int = 640
    private val IMAGE_HEIGHT: Int = 640

    private fun getTop3(foundObjects: List<ClassifiedBox>): List<ClassifiedBox> {
        return foundObjects.sortedByDescending { it.confidence }.take(3)
    }




    override fun processImageProxy(imageProxy: ImageProxy): List<ClassifiedBox> {
        if (!::bitmapBuffer.isInitialized) {
            // The image rotation and RGB image buffer are initialized only once
            // the analyzer has started running

            imageRotationDegrees = imageProxy.imageInfo.rotationDegrees
            bitmapBuffer = Bitmap.createBitmap(
                imageProxy.width, imageProxy.height, Bitmap.Config.RGBA_F16)
        }

        val imgBitmap = imageProxy.toBitmap()
        val rawBitmap = imgBitmap?.let { Bitmap.createScaledBitmap(it, IMAGE_WIDTH, IMAGE_HEIGHT, false) }
        val bitmap = rawBitmap?.rotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            ?: return listOf()
        val imgData = preProcess(bitmap, IMAGE_WIDTH, IMAGE_HEIGHT)


        imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer)  }

        // Process the image in Tensorflow
        val tfImage =  tfImageProcessor.process(tfImageBuffer.apply { load(bitmapBuffer) })


        var outputBuffer: Array<Array<FloatArray>> = arrayOf(arrayOf(floatArrayOf()))

        tflite.run(arrayOf(imgData), outputBuffer)

        val result = getAllObjectsByClassFromYOLO(outputBuffer[0], -1, CONFIDENCE_THRESHOLD, SCORE_THRESHOLD, IMAGE_WIDTH, IMAGE_HEIGHT)

        return getTop3(result)
    }

    companion object {
        private const val MODEL_PATH = "yolov5s-fp16.tflite"
        private const val LABELS_PATH = "coco_ssd_mobilenet_v1_1.0_labels.txt"
    }
}