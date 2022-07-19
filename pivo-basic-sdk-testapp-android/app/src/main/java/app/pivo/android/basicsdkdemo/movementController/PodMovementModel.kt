package app.pivo.android.basicsdkdemo.movementController

import app.pivo.android.basicsdkdemo.movementController.utils.Point
import app.pivo.android.basicsdkdemo.movementController.utils.convertRadianToGrad
import kotlin.math.abs
import kotlin.math.min


/**
 * @param PODSpeeds - keys: subset of available speeds, sec/ 1 rotation
 */
open class PodMovementModel(PODSpeeds: List<Int>) {
    protected var lastDirection: Float = 0.0f
    protected var lastFOV: Float = 90.0f
    protected var currentPODRotationSpeed: Float = 0.0f
    protected var lastRotationLeftover: Float = 0.0f

    private val ballModel: BallModel = BallModel()

    class PodSpeedPair(var secPerRotation: Int) {
        var gradPerSecond: Float = 1.0f

        init {
            this.gradPerSecond = 1.0f / secPerRotation
        }
    }
    private var podSpeedsMapping: MutableList<PodSpeedPair> = mutableListOf()

    init {
        for (speedSecPerRotation in PODSpeeds) {
            podSpeedsMapping.add(PodSpeedPair(speedSecPerRotation))
        }
    }

    private fun getTheMostAppropriateSpeed(deltaGradAngle: Float, averageSegmentTime: Float): Int {
        val absDeltaAngle = abs(deltaGradAngle)

        var speedWithLeastLeftover = podSpeedsMapping[0].secPerRotation
        var leftoverOfBestResult = abs(absDeltaAngle - averageSegmentTime * podSpeedsMapping[0].gradPerSecond)

        for (i in 1..podSpeedsMapping.lastIndex) {
            val curSpeedLeftover = abs(absDeltaAngle - averageSegmentTime * podSpeedsMapping[i].gradPerSecond)
            if (curSpeedLeftover < leftoverOfBestResult) {
                leftoverOfBestResult = curSpeedLeftover
                speedWithLeastLeftover = podSpeedsMapping[i].secPerRotation
            }
        }
        return speedWithLeastLeftover
    }

    fun updateTargetPosition(point: Point?, timeFromLastSegmentUpdate: Float) {

        val podTraveledRotation = min(currentPODRotationSpeed * timeFromLastSegmentUpdate, lastRotationLeftover)
        lastRotationLeftover -= podTraveledRotation
        lastDirection += podTraveledRotation

        ballModel.updateModelState(point, timeFromLastSegmentUpdate)

        val targetPosition = ballModel.getApproximatedBallPosition(2) ?: return

        val deltaGradAngle = convertRadianToGrad(targetPosition.getAngle()) - lastDirection
        lastRotationLeftover = deltaGradAngle

        val newPODSpeedSecPerRotation = getTheMostAppropriateSpeed(deltaGradAngle, ballModel.getAverageSegmentTime())
        onMove(newPODSpeedSecPerRotation, lastRotationLeftover.toInt())

        currentPODRotationSpeed = 1.0f / newPODSpeedSecPerRotation

    }

    open fun onMove(speed: Int, orientedAngle: Int) {
        println("With speed = $speed, pod is rotating by the angle = $orientedAngle")
    }
}