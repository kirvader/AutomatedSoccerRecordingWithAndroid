package com.hawkeye.movement.interfaces

import com.hawkeye.movement.utils.Point

interface TrackingSystemControllerBase {
    fun updateTargetPosition(point: Point?, currentAbsTime_ms: Long)

    fun directDeviceAtObjectAtTime(currentAbsTime_ms: Long, targetAbsTime_ms: Long)
}