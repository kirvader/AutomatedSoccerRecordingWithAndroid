package app.hawkeye.balltracker

import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import app.hawkeye.balltracker.controllers.FootballTrackingSystemController
import app.hawkeye.balltracker.controllers.time.TimeKeeper
import app.hawkeye.balltracker.controllers.time.interfaces.TimeKeeperBase
import app.hawkeye.balltracker.processors.image.ONNXYOLOv5ImageProcessor
import app.hawkeye.balltracker.processors.image.ONNXYOLOv5WithTrackerImageProcessor
import app.hawkeye.balltracker.processors.interfaces.ModelImageProcessor
import app.hawkeye.balltracker.utils.AdaptiveRect
import app.hawkeye.balltracker.utils.ClassifiedBox
import app.hawkeye.balltracker.utils.AdaptiveScreenPoint
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

enum class TrackingStrategyChoice(private val index: Int) {
    None(0),
    SingleRectFollower(1);

    companion object {
        private val VALUES = values()
        fun getByValue(value: Int) = VALUES.firstOrNull { it.index == value }
    }
}

class ObjectDetectorImageAnalyzer(
    context: Context,
    private val updateUIonResultsReady: (AdaptiveRect?, String) -> Unit,
    private val updateUIAreaOfDetectionWithNewArea: (List<AdaptiveRect>) -> Unit
) : ImageAnalysis.Analyzer {
    private var currentImageProcessorsChoice: ImageProcessorsChoice = ImageProcessorsChoice.None
    private var modelImageProcessors: Map<ImageProcessorsChoice, ModelImageProcessor> = mapOf()

    private var timeKeeper: TimeKeeperBase = TimeKeeper()
    private var movementControllerSystem =
        FootballTrackingSystemController(
            App.getRotatableDevice()
        )

    init {
        modelImageProcessors = mapOf(
            ImageProcessorsChoice.None to ModelImageProcessor.Default,
            ImageProcessorsChoice.ONNX_YOLO_V5 to ONNXYOLOv5ImageProcessor(context),
            ImageProcessorsChoice.ONNX_YOLO_V5_TRACKER to ONNXYOLOv5WithTrackerImageProcessor(
                context,
                ::getBallPositionAtTime,
                ::getCurrentImageProcessingStart,
                updateUIAreaOfDetectionWithNewArea
            )
        )
    }

    private fun getBallPositionAtTime(absTime_ms: Long): AdaptiveScreenPoint? {
        return movementControllerSystem.getBallPositionOnScreenAtTime(absTime_ms)
    }

    private fun getCurrentImageProcessingStart() = timeKeeper.getCurrentCircleStartTime()

    private fun updateTrackingSystemState(result: ClassifiedBox?) {
        if (result == null) {
            LOG.i("No appropriate objects found")
            movementControllerSystem.updateBallModelWithClassifiedBox(
                null,
                timeKeeper.getCurrentCircleStartTime()
            )
            timeKeeper.registerCircle()
            updateUIonResultsReady(
                null,
                timeKeeper.getInfoAboutLastCircle()
            )
            return
        }
        movementControllerSystem.updateBallModelWithClassifiedBox(
            result,
            timeKeeper.getCurrentCircleStartTime()
        )
        timeKeeper.registerCircle()
        updateUIonResultsReady(
            result.adaptiveRect,
            timeKeeper.getInfoAboutLastCircle()
        )
    }

    fun setCurrentImageProcessor(choice: ImageProcessorsChoice) {
        currentImageProcessorsChoice = choice
        LOG.i(currentImageProcessorsChoice)
        modelImageProcessors[currentImageProcessorsChoice]?.getAreaOfDetection()
            ?.let { updateUIAreaOfDetectionWithNewArea(it) }
    }

    override fun analyze(imageProxy: ImageProxy) {
        LOG.i(currentImageProcessorsChoice)
        val result =
            modelImageProcessors[currentImageProcessorsChoice]?.processImageProxy(imageProxy = imageProxy)

        imageProxy.close()

        updateTrackingSystemState(result)
    }
}