package app.pivo.android.basicsdkdemo.utils

import app.pivo.android.basicsdk.PivoSdk
import app.pivo.android.basicsdkdemo.movementController.PodMovementModel
import kotlin.math.abs

class PodMovementController(PODSpeeds: List<Int>) : PodMovementModel(PODSpeeds) {
    override fun moveBy(speed: Int, orientedAngle: Int) {
//        XLog.tag(TAG).i("Pod should turn by angle = $orientedAngle with speed = $speed")
        PivoSdk.getInstance().setSpeed(speed)
        if (orientedAngle > 0) {
            PivoSdk.getInstance().turnRight(abs(orientedAngle))
        } else if (orientedAngle < 0) {
            PivoSdk.getInstance().turnLeft(abs(orientedAngle))
        }
    }
}