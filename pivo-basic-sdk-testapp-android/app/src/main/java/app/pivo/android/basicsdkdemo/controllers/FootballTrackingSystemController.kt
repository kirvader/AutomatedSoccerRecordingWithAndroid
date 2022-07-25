package app.pivo.android.basicsdkdemo.utils

import com.example.movementcontrollingmodule.movementController.RotatableDevice
import com.example.movementcontrollingmodule.movementController.TrackingSystemControllerBase
import com.example.movementcontrollingmodule.movementController.utils.Point
import com.example.movementcontrollingmodule.movementController.utils.PolarPoint


class FootballTrackingSystemController(rotatableDevice: RotatableDevice) : TrackingSystemControllerBase(
    rotatableDevice
) {

    fun updateTargetWithClassifiedBox(box: ClassifiedBox?, timeFromLastSegmentUpdate: Float) {
        if (box == null)
        {
            updateTargetPosition(null, timeFromLastSegmentUpdate)
            return
        }
        val deltaAngle = box.center.x * 90.0f - 45.0f  // relative to lastDirection

        val ballPlaneWidth = ballDiam * 1.0f / box.width
        val distance = ballPlaneWidthToDistance * ballPlaneWidth

        val ballPlaneHeight = ballDiam * 1.0f / box.height
        val height = box.center.y * ballPlaneHeight

        updateTargetPosition(Point(PolarPoint(distance, deviceRotatableController.getLastDirection() + deltaAngle, height)), timeFromLastSegmentUpdate)
    }

    companion object {
        private const val ballDiam = 0.23f // ball diameter in meters
        private const val ballPlaneWidthToDistance = 16.0f / 17.0f // it is probably should be some trigonometrical formula but we haven't got zoom control for now, sooooo...
    }
}