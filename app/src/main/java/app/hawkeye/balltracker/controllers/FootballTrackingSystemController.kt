package app.hawkeye.balltracker.controllers

import app.hawkeye.balltracker.processors.utils.ClassifiedBox
import app.hawkeye.balltracker.processors.utils.AdaptiveScreenVector
import app.hawkeye.balltracker.utils.createLogger
import com.hawkeye.movement.TrackingSystemController
import com.hawkeye.movement.interfaces.RotatableDevice
import com.hawkeye.movement.utils.AngleMeasure
import com.hawkeye.movement.utils.Degree
import com.hawkeye.movement.utils.Point
import com.hawkeye.movement.utils.PolarPoint

private val LOG = createLogger<FootballTrackingSystemController>()


class FootballTrackingSystemController(rotatableDevice: RotatableDevice) : TrackingSystemController(
    rotatableDevice
) {

    private class ScreenPart(private val screenPart: Float, private val cameraFOV_degree: Float): AngleMeasure {
        override fun degree(): Float {
            return (screenPart - 0.5f) * cameraFOV_degree
        }
    }

    private val averageDistance = 10.0f

    fun getBallPositionOnScreenAtTime(absTime_ms: Long) : AdaptiveScreenVector? {
        val currentBallPosition = ballMovementModel.getApproximatePositionAtTime(absTime_ms) ?: return null

        val ballDirection = currentBallPosition.getAngle()

        val currentCameraDirection = rotatableDeviceController.getDirectionAtTime(absTime_ms)

        val deltaAngle = ballDirection - currentCameraDirection

        return AdaptiveScreenVector(0.5f + deltaAngle.degree() / cameraFOV, currentBallPosition.getHeight())
    }

    fun updateBallModelWithClassifiedBox(box: ClassifiedBox?, absTime_ms: Long) {
        if (box == null)
        {
            updateTargetPosition(null, absTime_ms)
            return
        }
        val centerX = box.adaptiveRect.topLeftPoint.x + box.adaptiveRect.size.x / 2

        val deltaAngle = ScreenPart(centerX, cameraFOV)
        val height = box.adaptiveRect.topLeftPoint.y + box.adaptiveRect.size.y / 2

        LOG.i(box.adaptiveRect.topLeftPoint + box.adaptiveRect.size)

        LOG.i("grad = ${rotatableDeviceController.getDirectionAtTime(absTime_ms).degree()} + ${deltaAngle.degree()} = ${rotatableDeviceController.getDirectionAtTime(absTime_ms).degree() + deltaAngle.degree()}")

        updateTargetPosition(Point(PolarPoint(averageDistance, rotatableDeviceController.getDirectionAtTime(absTime_ms) + deltaAngle, height)), absTime_ms)
    }

    companion object {
        private const val cameraFOV = 90.0f
    }
}