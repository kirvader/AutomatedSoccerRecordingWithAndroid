package app.hawkeye.balltracker

import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import app.hawkeye.balltracker.controllers.FootballTrackingSystemController
import app.hawkeye.balltracker.controllers.time.TimeKeeper
import app.hawkeye.balltracker.controllers.time.interfaces.TimeKeeperBase
import app.hawkeye.balltracker.processors.image.*
import app.hawkeye.balltracker.processors.utils.*
import app.hawkeye.balltracker.utils.createLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


private val LOG = createLogger<ObjectDetectorImageAnalyzer>()

enum class ImageProcessorsChoice(private val index: Int) {
    None(0),
    ONNX_YOLO_V5(1),
    ONNX_YOLO_V5_TRACKER_NOROTATIONFIT_QUAD(2),
    ONNX_YOLO_V5_TRACKER_ROTATIONFIT_QUAD(3),
    ONNX_YOLO_V5_TRACKER_NOROTATIONFIT_SINGLE(4);

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
    private val updateUIonResultsReady: (AdaptiveScreenRect?, String) -> Unit,
    private val updateUIAreaOfDetectionWithNewArea: (List<AdaptiveScreenRect>) -> Unit
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
            ImageProcessorsChoice.ONNX_YOLO_V5_TRACKER_NOROTATIONFIT_QUAD to ONNXYOLOv5WithTrackerImageProcessor_NoRotatingFit_QuadSquare(
                context,
                ::getCurrentImageProcessingStart,
                updateUIAreaOfDetectionWithNewArea
            ),
            // crutch about switching between static and rotating camera
            ImageProcessorsChoice.ONNX_YOLO_V5_TRACKER_ROTATIONFIT_QUAD to ONNXYOLOv5WithTrackerImageProcessor_WithRotationFit_QuadSquare(
                context,
                ::getCurrentImageProcessingStart,
                updateUIAreaOfDetectionWithNewArea,
                ::getBallPositionAtTime
            ),
            // crutch about switching between static and rotating camera
            ImageProcessorsChoice.ONNX_YOLO_V5_TRACKER_NOROTATIONFIT_SINGLE to OnnxYoloV5WithTrackerImageProcessor_NoRotatingFit_SingleSquare(
                context,
                ::getCurrentImageProcessingStart,
                updateUIAreaOfDetectionWithNewArea
            ),


        )
    }

    private fun getBallPositionAtTime(absTime_ms: Long): AdaptiveScreenVector? {
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

        LOG.i(movementControllerSystem.getBallPositionOnScreenAtTime(timeKeeper.getCurrentCircleStartTime()))

        movementControllerSystem.directDeviceAtObjectAtTime(timeKeeper.getCurrentCircleStartTime(), timeKeeper.getCurrentCircleStartTime())

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
        var result: ClassifiedBox?
        CoroutineScope(Dispatchers.Default).launch {
            result = modelImageProcessors[currentImageProcessorsChoice]?.processImageProxy(imageProxy = imageProxy)
            updateTrackingSystemState(result)

            imageProxy.close()
        }
    }
}