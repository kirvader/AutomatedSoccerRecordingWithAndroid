package com.example.movementcontrollingmodule.movementController

interface RotatableDevice {
    /**
     * rotates device with available speed by specified angle
     *
     * @param speed chosen speed, grad/second
     * @param orientedAngle chosen angle to rotate device, grad
     */
    fun rotateBy(speed: Float, orientedAngle: Float)

    fun stop()

    /**
     * @return the most appropriate available speed for this device
     * @param speed grad/sec
     */
    fun getTheMostAppropriateSpeedFromAvailable(speed: Float): Float

    /**
     * @return speed in grad/sec
     * @param availableDeviceSpeed one of the devices speed
     */
    fun getGradPerSecSpeedFromAvailable(availableDeviceSpeed: Float): Float
}