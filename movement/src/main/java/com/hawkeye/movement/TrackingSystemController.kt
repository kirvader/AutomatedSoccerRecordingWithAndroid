package com.hawkeye.movement

import com.hawkeye.movement.interfaces.PhysicalObjectMovementModel
import com.hawkeye.movement.interfaces.RotatableDevice
import com.hawkeye.movement.interfaces.RotatableDeviceControllerBase
import com.hawkeye.movement.interfaces.TrackingSystemControllerBase
import com.hawkeye.movement.utils.Point

open class TrackingSystemController(rotatableDevice: RotatableDevice) : TrackingSystemControllerBase {
    protected val ballMovementModel: PhysicalObjectMovementModel = PhysicalObjectMovement()
    protected var rotatableDeviceController: RotatableDeviceControllerBase


    init {
        rotatableDeviceController = RotatableDeviceController(rotatableDevice)
    }

    override fun updateTargetPosition(point: Point?, currentAbsTime_ms: Long) {
        ballMovementModel.updatePositionAtTime(point, currentAbsTime_ms)
    }

    override fun directDeviceAtObjectAtTime(currentAbsTime_ms: Long, targetAbsTime_ms: Long) {
        val targetPosition = ballMovementModel.getApproximatePositionAtTime(targetAbsTime_ms) ?: return

        rotatableDeviceController.smoothlyDirectDeviceAt(targetPosition, currentAbsTime_ms, targetAbsTime_ms)

    }
}