package app.pivo.android.basicsdkdemo.utils

import android.content.Context
import app.pivo.android.basicsdk.PivoSdk
import app.pivo.android.basicsdkdemo.movementController.RotateDeviceInterface


class PodRotatingImplementation : RotateDeviceInterface {
    private fun getLicenseContent(context: Context): String = context.assets.open("licenceKey.json").bufferedReader().use { it.readText() }

    override fun init(context: Context) {
        super.init(context)
        PivoSdk.init(context)
        PivoSdk.getInstance().unlockWithLicenseKey(getLicenseContent(context))
    }

    override fun rotateBy(speed: Float, orientedAngle: Float) {
        super.rotateBy(speed, orientedAngle)

    }

    override fun stop() {
        super.stop()
    }

    override fun getTheMostAppropriateSpeedFromAvailable(speed: Float): Float {
        return super.getTheMostAppropriateSpeedFromAvailable(speed)
    }

    override fun getGradPerSecSpeedFromAvailable(availableDeviceSpeed: Float): Float {
        return super.getGradPerSecSpeedFromAvailable(availableDeviceSpeed)
    }

//    override fun moveBy(speed: Int, orientedAngle: Int) {
////        XLog.tag(TAG).i("Pod should turn by angle = $orientedAngle with speed = $speed")
//        PivoSdk.getInstance().setSpeed(speed)
//        if (orientedAngle > 0) {
//            PivoSdk.getInstance().turnRight(abs(orientedAngle))
//        } else if (orientedAngle < 0) {
//            PivoSdk.getInstance().turnLeft(abs(orientedAngle))
//        }
//    }
}