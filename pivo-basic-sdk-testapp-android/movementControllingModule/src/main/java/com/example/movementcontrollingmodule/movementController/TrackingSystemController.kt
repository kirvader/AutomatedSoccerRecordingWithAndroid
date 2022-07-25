package com.example.movementcontrollingmodule.movementController

import com.example.movementcontrollingmodule.movementController.utils.Point

open class TrackingSystemControllerBase(rotatableDevice: RotatableDevice) {
    init {
        setRotatableDevice(rotatableDevice)
    }

    protected val ballMovementModel: BallMovementModel = BallMovementModel()
    protected val deviceRotatableController: DeviceRotatingControllerBase = DeviceRotatingControllerBase()

    fun setRotatableDevice(rotatableDevice: RotatableDevice) {
        deviceRotatableController.setRotatableDevice(rotatableDevice)
    }

    fun updateTargetPosition(point: Point?, timeFromLastSegmentUpdate: Float) {

        ballMovementModel.updateModelState(point, timeFromLastSegmentUpdate)

        val targetPosition = ballMovementModel.getApproximatedBallPosition(2) ?: return

        deviceRotatableController.updateAndMovePodToTargetPosition(targetPosition, ballMovementModel.getAverageSegmentTime())
    }
}