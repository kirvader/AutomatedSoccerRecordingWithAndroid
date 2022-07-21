package app.pivo.android.basicsdkdemo.movementController

import app.pivo.android.basicsdkdemo.movementController.utils.Point

open class PodToObjectModel(protected val podMovementModel: PodMovementModel) {

    protected val ballModel: BallModel = BallModel()

    fun updateTargetPosition(point: Point?, timeFromLastSegmentUpdate: Float) {

        ballModel.updateModelState(point, timeFromLastSegmentUpdate)

        val targetPosition = ballModel.getApproximatedBallPosition(2) ?: return

        podMovementModel.updateAndMovePodToTargetPosition(targetPosition, ballModel.getAverageSegmentTime())
    }
}