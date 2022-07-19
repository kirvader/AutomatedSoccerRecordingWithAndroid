package app.pivo.android.basicsdkdemo.utils

//import com.elvishew.xlog.XLog
import app.pivo.android.basicsdk.PivoSdk
import app.pivo.android.basicsdkdemo.movementController.PodMovementModel
import app.pivo.android.basicsdkdemo.movementController.utils.Point
import app.pivo.android.basicsdkdemo.movementController.utils.PolarPoint
import kotlin.math.abs

class PodMovementController(PODSpeeds: List<Int>) : PodMovementModel(PODSpeeds) {
    companion object {
        private const val TAG = "PODMovementController"
        private const val ballDiam = 0.23f // ball diameter in meters
        private const val ballPlaneWidthToDistance = 16.0f / 17.0f // it is probably should be some trigonometrical formula but we haven't got zoom control for now, sooooo...
    }
    override fun onMove(speed: Int, orientedAngle: Int) {
//        XLog.tag(TAG).i("Pod should turn by angle = $orientedAngle with speed = $speed")
        PivoSdk.getInstance().setSpeed(speed)
        if (orientedAngle > 0) {
            PivoSdk.getInstance().turnRight(abs(orientedAngle))
        } else if (orientedAngle < 0) {
            PivoSdk.getInstance().turnLeft(abs(orientedAngle))
        }
    }

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

        updateTargetPosition(Point(PolarPoint(distance, lastDirection + deltaAngle, height)), timeFromLastSegmentUpdate)
    }
}