package com.hawkeye.movement.interfaces

import com.hawkeye.movement.utils.AngleMeasure
import com.hawkeye.movement.utils.Degree

interface RotatableDevice {
    /**
     * rotates device with available speed by specified angle
     *
     * @param speed chosen speed, grad/second
     * @param orientedAngle chosen angle to rotate device, grad
     */
    fun rotateBy(speed: Float, orientedAngle: AngleMeasure)

    fun stop()

    /**
     * @return the most appropriate available speed for this device
     * @param speed grad/sec
     */
    fun getTheMostAppropriateSpeedFromAvailable(speed: AngleMeasure): Float

    /**
     * @return speed in grad/sec
     * @param availableDeviceSpeed one of the devices speed
     */
    fun getGradPerSecSpeedFromAvailable(availableDeviceSpeed: Float): AngleMeasure

    object Dummy : RotatableDevice {
        override fun rotateBy(speed: Float, orientedAngle: AngleMeasure) {}

        override fun stop() {}

        override fun getTheMostAppropriateSpeedFromAvailable(speed: AngleMeasure): Float = speed.degree()

        override fun getGradPerSecSpeedFromAvailable(availableDeviceSpeed: Float): AngleMeasure =
            Degree(availableDeviceSpeed)
    }
}