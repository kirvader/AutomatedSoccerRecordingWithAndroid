package com.example.movementcontrollingmodule.movementController

import com.example.movementcontrollingmodule.movementController.utils.Point

open class DeviceToObjectControllerBase {

    protected val ballModel: BallModel = BallModel()
    protected val deviceRotatingController: DeviceRotatingControllerBase = DeviceRotatingControllerBase()

    fun setRotationDevice(rotateDeviceImplementation: RotateDeviceInterface) {
        deviceRotatingController.setRotationDevice(rotateDeviceImplementation)
    }

    fun initRotationDevice() {
        deviceRotatingController.initRotationDevice()
    }

    fun updateTargetPosition(point: Point?, timeFromLastSegmentUpdate: Float) {

        ballModel.updateModelState(point, timeFromLastSegmentUpdate)

        val targetPosition = ballModel.getApproximatedBallPosition(2) ?: return

        deviceRotatingController.updateAndMovePodToTargetPosition(targetPosition, ballModel.getAverageSegmentTime())
    }
}