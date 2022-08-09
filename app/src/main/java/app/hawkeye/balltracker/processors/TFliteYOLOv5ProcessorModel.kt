package app.hawkeye.balltracker.processors

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.util.Size
import androidx.camera.core.ImageProxy
import app.hawkeye.balltracker.utils.ClassifiedBox
import app.hawkeye.balltracker.utils.createLogger
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.nnapi.NnApiDelegate
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.ObjectDetector

import java.nio.ByteBuffer

private val LOG = createLogger<TFliteYOLOv5ProcessorModel>()

internal class TFliteYOLOv5ProcessorModel(
    private val context: Context) : ModelImageProcessor {




    private val CONFIDENCE_THRESHOLD: Float = 0.3F
    private val SCORE_THRESHOLD: Float = 0.2F
    private val IMAGE_WIDTH: Int = 640
    private val IMAGE_HEIGHT: Int = 640

    private val numThreads = 8

    // For this example this needs to be a var so it can be reset on changes. If the ObjectDetector
    // will not change, a lazy val would be preferable.
    private var objectDetector: ObjectDetector? = null

    init {
        setupObjectDetector()
    }

    fun clearObjectDetector() {
        objectDetector = null
    }

    // Initialize the object detector using current settings on the
    // thread that is using it. CPU and NNAPI delegates can be used with detectors
    // that are created on the main thread and used on a background thread, but
    // the GPU delegate needs to be used on the thread that initialized the detector
    private fun setupObjectDetector() {
        // Create the base options for the detector using specifies max results and score threshold
        val optionsBuilder =
            ObjectDetector.ObjectDetectorOptions.builder()
                .setScoreThreshold(SCORE_THRESHOLD)

        // Set general detection options, including number of used threads
        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(numThreads)

        // Use the specified hardware for running the model. Default to CPU
        baseOptionsBuilder.useGpu()

        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())


        val modelName = MODEL_PATH

        try {
            objectDetector =
                ObjectDetector.createFromFileAndOptions(context, modelName, optionsBuilder.build())
        } catch (e: IllegalStateException) {
            LOG.e("TFLite failed to load model with error: ", e)
        }
    }

    override fun processImageProxy(imageProxy: ImageProxy): List<ClassifiedBox> {
        if (objectDetector == null) {
            setupObjectDetector()
        }

        // Inference time is the difference between the system time at the start and finish of the
        // process
        var inferenceTime = SystemClock.uptimeMillis()

        // Create preprocessor for the image.
        // See https://www.tensorflow.org/lite/inference_with_metadata/
        //            lite_support#imageprocessor_architecture

        val imageProcessor =
            ImageProcessor.Builder()
                .add(Rot90Op(-imageProxy.imageInfo.rotationDegrees / 90))
                .build()

        val imgBitmap = imageProxy.toBitmap()
        val rawBitmap =
            imgBitmap?.let { Bitmap.createScaledBitmap(it, IMAGE_WIDTH, IMAGE_HEIGHT, false) }
//        val bitmap = rawBitmap?.rotate(imageProxy.imageInfo.rotationDegrees.toFloat())
//            ?: return listOf()
//        val imgData = preProcess(bitmap, IMAGE_WIDTH, IMAGE_HEIGHT)

        // Preprocess the image and convert it into a TensorImage for detection.
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(rawBitmap))

        val results = objectDetector?.detect(tensorImage)

        print(results)
        return listOf()

    //        if (!::bitmapBuffer.isInitialized) {
//            // The image rotation and RGB image buffer are initialized only once
//            // the analyzer has started running
//
//            imageRotationDegrees = imageProxy.imageInfo.rotationDegrees
//            bitmapBuffer = Bitmap.createBitmap(
//                imageProxy.width * 2, imageProxy.height * 2, Bitmap.Config.RGBA_F16
//            )
//        }
//
//        val imgBitmap = imageProxy.toBitmap()
//        val rawBitmap =
//            imgBitmap?.let { Bitmap.createScaledBitmap(it, IMAGE_WIDTH, IMAGE_HEIGHT, false) }
//        val bitmap = rawBitmap?.rotate(imageProxy.imageInfo.rotationDegrees.toFloat())
//            ?: return listOf()
//        val imgData = preProcess(bitmap, IMAGE_WIDTH, IMAGE_HEIGHT)
//
//        val input: ByteBuffer = floatToByteBuffer(imgData)
//
//        val output = ByteBuffer.allocateDirect(8568000)
//
//        if (tflite == null) {
//            return listOf()
//        }
//        tflite!!.run(input, output)
//        output.rewind()
//
//        val floatOutput = output.asFloatBuffer()
//        floatOutput.rewind()
//        val floatBuf = floatOutput.duplicate()
//        floatBuf.rewind()
//        val floatArr = floatBuf.array()
//        val res = floatArr.toMutableList().chunked(85)
//
//        return getAllObjectsByClassForYOLOFromFloatList(
//            res,
//            -1,
//            CONFIDENCE_THRESHOLD,
//            SCORE_THRESHOLD,
//            IMAGE_WIDTH,
//            IMAGE_HEIGHT
//        )
    }

    companion object {
        private const val MODEL_PATH = "yolov5s-fp16.tflite"
        private const val LABELS_PATH = "coco_ssd_mobilenet_v1_1.0_labels.txt"
    }
}