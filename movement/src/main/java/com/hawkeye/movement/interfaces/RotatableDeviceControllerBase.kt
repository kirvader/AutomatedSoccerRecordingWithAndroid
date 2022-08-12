package com.hawkeye.movement.interfaces

import com.hawkeye.movement.utils.Point

interface RotatableDeviceControllerBase {
    fun rotateByAngleAtTime(angle: Float, speed: Float, currentTime_ms: Long)

    /**
     * rotating device evenly to position, so it will reach needed direction at `time_ms`
     */
    fun smoothlyDirectDeviceAt(position: Point, currentTime_ms: Long, targetTime_ms: Long)

    fun getDeviceDirectionWithConstraints(direction: Float) = direction

    fun getDirectionAtTime(absTime: Long): Float
}