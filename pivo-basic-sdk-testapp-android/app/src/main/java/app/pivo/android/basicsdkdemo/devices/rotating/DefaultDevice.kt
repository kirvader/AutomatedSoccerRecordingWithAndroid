package app.pivo.android.basicsdkdemo.devices.rotating

import app.pivo.android.basicsdkdemo.utils.createLogger
import com.example.movementcontrollingmodule.movementController.RotatableDevice

private val LOG = createLogger<DefaultDevice>()


class DefaultDevice : RotatableDevice {
    init {

    }
    override fun rotateBy(speed: Float, orientedAngle: Float) {
        LOG.i("Default device rotateBy(speed = $speed, orientedAngle = $orientedAngle)")
    }

    override fun stop() {
        LOG.i("Default device stopped")
    }

    override fun getTheMostAppropriateSpeedFromAvailable(speed: Float): Float = speed

    override fun getGradPerSecSpeedFromAvailable(availableDeviceSpeed: Float): Float =
        availableDeviceSpeed
}