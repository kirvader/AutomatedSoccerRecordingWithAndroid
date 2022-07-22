package app.pivo.android.basicsdkdemo.utils

import android.content.Context
import app.pivo.android.basicsdk.PivoSdk
import com.example.movementcontrollingmodule.movementController.RotateDeviceInterface
import kotlin.math.abs


class PivoPodRotatingImplementation(private val context: Context) : RotateDeviceInterface {
    private lateinit var availableSpeeds: List<Int>

    private fun getLicenseContent(context: Context): String = context.assets.open("licenceKey.json").bufferedReader().use { it.readText() }

    override fun init() {
        super.init()
        PivoSdk.init(context)
        PivoSdk.getInstance().unlockWithLicenseKey(getLicenseContent(context))

        availableSpeeds = PivoSdk.getInstance().supportedSpeeds.filter { it in 20..200 }
    }

    override fun rotateBy(speed: Float, orientedAngle: Float) {
        super.rotateBy(speed, orientedAngle)
        if (orientedAngle > 0) {
            PivoSdk.getInstance().turnRight(abs(orientedAngle.toInt()))
        } else if (orientedAngle < 0) {
            PivoSdk.getInstance().turnLeft(abs(orientedAngle.toInt()))
        } else {
            PivoSdk.getInstance().stop()
        }
    }

    override fun stop() {
        super.stop()
        PivoSdk.getInstance().stop()
    }

    override fun getTheMostAppropriateSpeedFromAvailable(speed: Float): Float {
        return availableSpeeds[availableSpeeds.binarySearch((360.0f / speed).toInt())].toFloat()
    }

    override fun getGradPerSecSpeedFromAvailable(availableDeviceSpeed: Float): Float {
        return 1.0f / 360.0f / availableDeviceSpeed
    }
}