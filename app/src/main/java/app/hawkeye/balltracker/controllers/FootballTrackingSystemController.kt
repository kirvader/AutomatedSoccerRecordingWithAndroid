package app.hawkeye.balltracker.controllers

import app.hawkeye.balltracker.utils.ClassifiedBox
import com.hawkeye.movement.RotatableDevice
import com.hawkeye.movement.TrackingSystemControllerBase
import com.hawkeye.movement.utils.Point
import com.hawkeye.movement.utils.PolarPoint


class FootballTrackingSystemController(rotatableDevice: RotatableDevice) : TrackingSystemControllerBase(
    rotatableDevice
) {

    private val averageDistance = 15.0f

    fun updateTargetWithClassifiedBox(box: ClassifiedBox?, timeFromLastSegmentUpdate: Float) {
        if (box == null)
        {
            updateTargetPosition(null, timeFromLastSegmentUpdate)
            return
        }
        val deltaAngle = box.adaptiveRect.center.x * 90.0f - 45.0f  // relative to lastDirection

        val height = box.adaptiveRect.center.y

        updateTargetPosition(Point(PolarPoint(averageDistance, deviceRotatableController.getLastDirection() + deltaAngle, height)), timeFromLastSegmentUpdate)
    }

}