package com.example.movementcontrollingmodule.movementController

import com.example.movementcontrollingmodule.movementController.utils.Point

open class TrackingSystemControllerBase {

    protected val ballMovementModel: BallMovementModel = BallMovementModel()
    protected val deviceRotatingController: DeviceRotatingControllerBase = DeviceRotatingControllerBase()

    fun setRotationDevice(rotateDeviceImplementation: RotatingDevice) {
        deviceRotatingController.setRotationDevice(rotateDeviceImplementation)
    }

    fun initRotationDevice() {
        deviceRotatingController.initRotationDevice()
    }

    fun updateTargetPosition(point: Point?, timeFromLastSegmentUpdate: Float) {

        ballMovementModel.updateModelState(point, timeFromLastSegmentUpdate)

        val targetPosition = ballMovementModel.getApproximatedBallPosition(2) ?: return

        deviceRotatingController.updateAndMovePodToTargetPosition(targetPosition, ballMovementModel.getAverageSegmentTime())
    }
}