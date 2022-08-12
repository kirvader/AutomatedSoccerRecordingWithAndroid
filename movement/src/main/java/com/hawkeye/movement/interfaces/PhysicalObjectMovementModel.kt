package com.hawkeye.movement.interfaces

import com.hawkeye.movement.utils.Point

interface PhysicalObjectMovementModel {
    fun updatePositionAtTime(position: Point?, time_ms: Long)

    fun getApproximatePositionAtTime(time_ms: Long): Point?

}