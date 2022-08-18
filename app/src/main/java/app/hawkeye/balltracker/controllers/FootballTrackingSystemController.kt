package app.hawkeye.balltracker.controllers

import app.hawkeye.balltracker.utils.ClassifiedBox
import app.hawkeye.balltracker.utils.AdaptiveScreenPoint
import com.hawkeye.movement.TrackingSystemController
import com.hawkeye.movement.interfaces.RotatableDevice
import com.hawkeye.movement.utils.AngleMeasure
import com.hawkeye.movement.utils.Degree
import com.hawkeye.movement.utils.Point
import com.hawkeye.movement.utils.PolarPoint


class FootballTrackingSystemController(rotatableDevice: RotatableDevice) : TrackingSystemController(
    rotatableDevice
) {

    private class ScreenPart(private val screenPart: Float, private val cameraFOV_degree: Float): AngleMeasure {
        override fun degree(): Float {
            return (screenPart - 0.5f) * cameraFOV_degree
        }

        override fun radian(): Float {
            return Degree(this.degree()).radian()
        }

    }

    private val averageDistance = 10.0f

    fun getBallPositionOnScreenAtTime(absTime_ms: Long) : AdaptiveScreenPoint? {
        val currentBallPosition = ballMovementModel.getApproximatePositionAtTime(absTime_ms) ?: return null

        val ballDirection = currentBallPosition.getAngle()

        val currentCameraDirection = rotatableDeviceController.getDirectionAtTime(absTime_ms)

        val deltaAngle = ballDirection - currentCameraDirection

        return AdaptiveScreenPoint(0.5f + deltaAngle.degree() / cameraFOV, currentBallPosition.getHeight())
    }

    fun updateBallModelWithClassifiedBox(box: ClassifiedBox?, absTime_ms: Long) {
        if (box == null)
        {
            updateTargetPosition(null, absTime_ms)
            return
        }

        val deltaAngle = ScreenPart(box.adaptiveRect.center.x, cameraFOV)
        val height = box.adaptiveRect.center.y

        updateTargetPosition(Point(PolarPoint(averageDistance, rotatableDeviceController.getDirectionAtTime(absTime_ms) + deltaAngle, height)), absTime_ms)
    }

    companion object {
        private const val cameraFOV = 90.0f
    }
}