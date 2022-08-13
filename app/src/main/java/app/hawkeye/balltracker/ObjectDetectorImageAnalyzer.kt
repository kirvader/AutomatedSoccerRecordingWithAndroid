package app.hawkeye.balltracker

import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import app.hawkeye.balltracker.processors.image.GoogleMLkitModelImageProcessor
import app.hawkeye.balltracker.processors.image.ORTModelImageProcessor
import app.hawkeye.balltracker.processors.image.ORTModelImageProcessorFastestDet
import app.hawkeye.balltracker.processors.image.TFliteYOLOv5ProcessorModel
import app.hawkeye.balltracker.processors.interfaces.ModelImageProcessor
import app.hawkeye.balltracker.utils.ClassifiedBox
import app.hawkeye.balltracker.utils.createLogger


private val LOG = createLogger<ObjectDetectorImageAnalyzer>()

enum class ImageProcessorsChoice(private val index: Int) {
    None(0),
    ONNX_YOLO_V5(1),
    ONNX_FASTEST_DET(2),
    GoogleML(3),
    TFLITE_YOLO_V5_SMALL(4);

    companion object {
        private val VALUES = values()
        fun getByValue(value: Int) = VALUES.firstOrNull { it.index == value }
    }
}

class ObjectDetectorImageAnalyzer(
    context: Context,
    val onResultsReady: (ClassifiedBox?) -> Unit
) : ImageAnalysis.Analyzer {
    private var currentImageProcessorsChoice: ImageProcessorsChoice = ImageProcessorsChoice.None
    private var modelImageProcessors: Map<ImageProcessorsChoice, ModelImageProcessor> = mapOf()


    init {
        modelImageProcessors = mapOf(
            ImageProcessorsChoice.None to ModelImageProcessor.Default,
            ImageProcessorsChoice.GoogleML to GoogleMLkitModelImageProcessor(),
            ImageProcessorsChoice.ONNX_YOLO_V5 to ORTModelImageProcessor(context),
            ImageProcessorsChoice.ONNX_FASTEST_DET to ORTModelImageProcessorFastestDet(context),
            ImageProcessorsChoice.TFLITE_YOLO_V5_SMALL to TFliteYOLOv5ProcessorModel(context)
        )
    }

    fun setCurrentImageProcessor(choice: ImageProcessorsChoice) {
        currentImageProcessorsChoice = choice
        LOG.i(currentImageProcessorsChoice)
    }

    override fun analyze(imageProxy: ImageProxy) {
        LOG.i(currentImageProcessorsChoice)
        val result =
            modelImageProcessors[currentImageProcessorsChoice]?.processImageProxy(imageProxy = imageProxy)

        imageProxy.close()

        onResultsReady(result)
    }
}