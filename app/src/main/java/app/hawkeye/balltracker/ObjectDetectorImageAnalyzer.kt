package app.hawkeye.balltracker

import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import app.hawkeye.balltracker.processors.image.ONNXYOLOv5ImageProcessor
import app.hawkeye.balltracker.processors.image.ONNXYOLOv5WithTrackerImageProcessor
import app.hawkeye.balltracker.processors.interfaces.ModelImageProcessor
import app.hawkeye.balltracker.utils.ClassifiedBox
import app.hawkeye.balltracker.utils.createLogger


private val LOG = createLogger<ObjectDetectorImageAnalyzer>()

enum class ImageProcessorsChoice(private val index: Int) {
    None(0),
    ONNX_YOLO_V5(1),
    ONNX_YOLO_V5_TRACKER(2);

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
            ImageProcessorsChoice.ONNX_YOLO_V5 to ONNXYOLOv5ImageProcessor(context),
            ImageProcessorsChoice.ONNX_YOLO_V5_TRACKER to ONNXYOLOv5WithTrackerImageProcessor(context)
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