package app.hawkeye.balltracker.utils

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import app.hawkeye.balltracker.R
import app.hawkeye.balltracker.utils.image_processors.GoogleMLkitImageProcessor
import app.hawkeye.balltracker.utils.image_processors.ImageProcessor
import app.hawkeye.balltracker.utils.image_processors.ORTImageProcessor
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions


private val LOG = createLogger<ObjectDetectorImageAnalyzer>()

enum class ImageProcessorsChoice(private val index: Int) {
    None(0),
    ORT_YOLO_V5(1),
    GoogleML(2);

    companion object {
        private val VALUES = values()
        fun getByValue(value: Int) = VALUES.firstOrNull { it.index == value }
    }
}

class ObjectDetectorImageAnalyzer(
    context: Context,
    val onUpdateUI: (List<ClassifiedBox>) -> Unit,
    val onUpdateCameraFOV: (List<ClassifiedBox>) -> Unit
) : ImageAnalysis.Analyzer {
    private var currentImageProcessorsChoice: ImageProcessorsChoice = ImageProcessorsChoice.None
    private var imageProcessors: Map<ImageProcessorsChoice, ImageProcessor> = mapOf()

    init {
        val objectDetectorOptions = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableClassification()
            .build()
        val objectDetector = ObjectDetection.getClient(objectDetectorOptions)

        val ortSession = createOrtSession(context)

        imageProcessors = mapOf(
            ImageProcessorsChoice.None to ImageProcessor.Default,
            ImageProcessorsChoice.GoogleML to GoogleMLkitImageProcessor(objectDetector),
            ImageProcessorsChoice.ORT_YOLO_V5 to ORTImageProcessor(ortSession)
        )
    }

    private fun readModel(context: Context): ByteArray {
        val modelID = R.raw.yolov5s
        return context.resources.openRawResource(modelID).readBytes()
    }

    private fun createOrtSession(context: Context): OrtSession? =
        OrtEnvironment.getEnvironment()?.createSession(readModel(context))

    fun setCurrentImageProcessor(choice: ImageProcessorsChoice) {
        currentImageProcessorsChoice = choice
        LOG.i(currentImageProcessorsChoice)
    }

    override fun analyze(imageProxy: ImageProxy) {
        LOG.i(currentImageProcessorsChoice)
        val result =
            imageProcessors[currentImageProcessorsChoice]?.processAndCloseImageProxy(imageProxy = imageProxy)

        if (result != null) {
            onUpdateUI(result)
        }
        if (result != null) {
            onUpdateCameraFOV(result)
        }
    }
}