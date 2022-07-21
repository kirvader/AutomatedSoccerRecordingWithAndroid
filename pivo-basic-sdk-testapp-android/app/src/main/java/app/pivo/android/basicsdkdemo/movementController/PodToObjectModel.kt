package app.pivo.android.basicsdkdemo.movementController

import app.pivo.android.basicsdkdemo.movementController.utils.Point

class DeviceToObjectModel {

    protected val ballModel: BallModel = BallModel()
    protected val deviceRotatingController: DeviceRotatingControllerBase = DeviceRotatingControllerBase()

    fun initializeRotationDevice(rotateDeviceImplementation: RotateDeviceInterface) {
        deviceRotatingController.initializeRotateDevice(rotateDeviceImplementation)
    }

    fun updateTargetPosition(point: Point?, timeFromLastSegmentUpdate: Float) {

        ballModel.updateModelState(point, timeFromLastSegmentUpdate)

        val targetPosition = ballModel.getApproximatedBallPosition(2) ?: return

        deviceRotatingController.updateAndMovePodToTargetPosition(targetPosition, ballModel.getAverageSegmentTime())
    }
}