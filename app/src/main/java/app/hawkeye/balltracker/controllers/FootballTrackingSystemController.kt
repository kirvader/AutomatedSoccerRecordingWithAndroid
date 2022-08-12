package app.hawkeye.balltracker.controllers

import app.hawkeye.balltracker.utils.ClassifiedBox
import com.hawkeye.movement.TrackingSystemController
import com.hawkeye.movement.interfaces.RotatableDevice
import com.hawkeye.movement.utils.Point
import com.hawkeye.movement.utils.PolarPoint


class FootballTrackingSystemController(rotatableDevice: RotatableDevice) : TrackingSystemController(
    rotatableDevice
) {

    private val averageDistance = 15.0f

    fun updateBallModelWithClassifiedBox(box: ClassifiedBox?, absTime_ms: Long) {
        if (box == null)
        {
            updateTargetPosition(null, absTime_ms)
            return
        }

        val deltaAngle = (box.adaptiveRect.center.x - 0.5f) * cameraFOV
        val height = box.adaptiveRect.center.y

        updateTargetPosition(Point(PolarPoint(averageDistance, rotatableDeviceController.getDirectionAtTime(absTime_ms) + deltaAngle, height)), absTime_ms)
    }

    companion object {
        private const val cameraFOV = 90.0f
    }
}