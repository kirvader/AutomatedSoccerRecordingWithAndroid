package app.hawkeye.balltracker.utils

import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import app.hawkeye.balltracker.processors.*
import app.hawkeye.balltracker.processors.ORTModelImageProcessor


private val LOG = createLogger<ObjectDetectorImageAnalyzer>()

enum class ImageProcessorsChoice(private val index: Int) {
    None(0),
    ONNX_YOLO_V5(1),
    TFLITE_YOLO_V5_SMALL(2);

    companion object {
        private val VALUES = values()
        fun getByValue(value: Int) = VALUES.firstOrNull { it.index == value }
    }
}

class ObjectDetectorImageAnalyzer(
    context: Context,
    val onUpdateUI: (List<ClassifiedBox>) -> Unit,
    val onUpdateCameraState: (List<ClassifiedBox>) -> Unit
) : ImageAnalysis.Analyzer {
    private var currentImageProcessorsChoice: ImageProcessorsChoice = ImageProcessorsChoice.None
    private var modelImageProcessors: Map<ImageProcessorsChoice, ModelImageProcessor> = mapOf()


    init {
        modelImageProcessors = mapOf(
            ImageProcessorsChoice.None to ModelImageProcessor.Default,
            ImageProcessorsChoice.ONNX_YOLO_V5 to ORTModelImageProcessor(context),
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

        if (result != null) {
            onUpdateUI(result)
        }
        if (result != null) {
            onUpdateCameraState(result)
        }
    }
}