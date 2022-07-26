package com.example.movementcontrollingmodule.movement

import com.example.movementcontrollingmodule.movement.utils.Point

open class TrackingSystemControllerBase(rotatableDevice: RotatableDevice) {
    protected val ballMovementModel: BallMovementModel = BallMovementModel()
    protected val deviceRotatableController: DeviceRotatingControllerBase = DeviceRotatingControllerBase()


    init {
        deviceRotatableController.setRotatableDevice(rotatableDevice)
    }

    fun updateTargetPosition(point: Point?, timeFromLastSegmentUpdate: Float) {

        ballMovementModel.updateModelState(point, timeFromLastSegmentUpdate)

        val targetPosition = ballMovementModel.getApproximatedBallPosition(2) ?: return

        deviceRotatableController.updateAndMovePodToTargetPosition(targetPosition, ballMovementModel.getAverageSegmentTime())
    }
}