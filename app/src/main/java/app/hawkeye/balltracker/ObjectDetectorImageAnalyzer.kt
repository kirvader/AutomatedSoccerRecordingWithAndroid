package app.hawkeye.balltracker

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import app.hawkeye.balltracker.configs.objects.TrackingSystemConfigObject
import app.hawkeye.balltracker.controllers.FootballTrackingSystemController
import app.hawkeye.balltracker.controllers.UIController
import app.hawkeye.balltracker.processors.image.*
import app.hawkeye.balltracker.processors.utils.*
import app.hawkeye.balltracker.utils.createLogger
import com.hawkeye.movement.interfaces.TrackingSystemControllerBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


private val LOG = createLogger<ObjectDetectorImageAnalyzer>()


class ObjectDetectorImageAnalyzer() : ImageAnalysis.Analyzer {
    private var modelImageProcessor: ModelImageProcessor = OnnxYoloV5WithTrackerImageProcessor()

    private var movementControllerSystem =
        FootballTrackingSystemController(
            App.getRotatableDevice()
        )

    fun getTrackingSystemController(): TrackingSystemControllerBase {
        return movementControllerSystem
    }


    private fun updateTrackingSystemState(result: ClassifiedBox?) {
        if (result == null) {
            LOG.i("No appropriate objects found")
            movementControllerSystem.updateBallModelWithClassifiedBox(
                null,
                TrackingSystemConfigObject.timeKeeper.getCurrentCircleStartTime()
            )
            TrackingSystemConfigObject.timeKeeper.registerCircle()

            UIController.updateUIWhenImageAnalyzerFinished(
                null,
                TrackingSystemConfigObject.timeKeeper.getInfoAboutLastCircle()
            )
            return
        }
        movementControllerSystem.updateBallModelWithClassifiedBox(
            result,
            TrackingSystemConfigObject.timeKeeper.getCurrentCircleStartTime()
        )

        TrackingSystemConfigObject.timeKeeper.registerCircle()

        LOG.i(movementControllerSystem.getBallPositionOnScreenAtTime(TrackingSystemConfigObject.timeKeeper.getCurrentCircleStartTime()))

        movementControllerSystem.directDeviceAtObjectAtTime(TrackingSystemConfigObject.timeKeeper.getCurrentCircleStartTime(), TrackingSystemConfigObject.timeKeeper.getCurrentCircleStartTime())

        UIController.updateUIWhenImageAnalyzerFinished(
            result.adaptiveRect,
            TrackingSystemConfigObject.timeKeeper.getInfoAboutLastCircle()
        )
    }

    override fun analyze(imageProxy: ImageProxy) {
        var result: ClassifiedBox?
        CoroutineScope(Dispatchers.Default).launch {
            result = modelImageProcessor.processImageProxy(imageProxy = imageProxy)
            updateTrackingSystemState(result)

            imageProxy.close()
        }
    }
}