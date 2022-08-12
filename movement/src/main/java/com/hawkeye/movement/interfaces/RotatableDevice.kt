package com.hawkeye.movement.interfaces

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

    object Dummy : RotatableDevice {
        override fun rotateBy(speed: Float, orientedAngle: Float) {}

        override fun stop() {}

        override fun getTheMostAppropriateSpeedFromAvailable(speed: Float): Float = speed

        override fun getGradPerSecSpeedFromAvailable(availableDeviceSpeed: Float): Float =
            availableDeviceSpeed
    }
}