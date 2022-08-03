package app.hawkeye.balltracker.utils

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import app.hawkeye.balltracker.utils.image_processors.ImageProcessor


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
    private val imageProcessors: Map<ImageProcessorsChoice, ImageProcessor>,
    val onUpdateUI: (List<ClassifiedBox>) -> Unit,
    val onUpdateCameraFOV: (List<ClassifiedBox>) -> Unit
) : ImageAnalysis.Analyzer {
    private var currentImageProcessorsChoice: ImageProcessorsChoice = ImageProcessorsChoice.None


    fun setCurrentImageProcessor(choice: ImageProcessorsChoice) {
        currentImageProcessorsChoice = choice
        LOG.i(currentImageProcessorsChoice)
        if (imageProcessors[currentImageProcessorsChoice] != null) {
            LOG.i("image proc is not null")
        }
    }

    override fun analyze(imageProxy: ImageProxy) {
        LOG.i(currentImageProcessorsChoice)
        val result = imageProcessors[currentImageProcessorsChoice]?.processAndCloseImageProxy(imageProxy = imageProxy)

        if (result != null) {
            onUpdateUI(result)
        }
        if (result != null) {
            onUpdateCameraFOV(result)
        }
    }
}