package app.pivo.android.basicsdkdemo.movementController

import app.pivo.android.basicsdkdemo.movementController.utils.Point
import app.pivo.android.basicsdkdemo.movementController.utils.convertRadianToGrad
import kotlin.math.abs
import kotlin.math.min


/**
 * @param PODSpeeds - keys: subset of available speeds, sec/ 1 rotation
 */
open class PodMovementModel(PODSpeeds: List<Int>) {

    private class PodSpeedPair(var secPerRotation: Int) {
        var gradPerSecond: Float = 1.0f

        init {
            this.gradPerSecond = 1.0f / secPerRotation
        }
    }

    private var lastDirection: Float = 0.0f
    private var lastFOV: Float = 90.0f
    private var currentPODRotationSpeed: Float = 0.0f
    private var lastRotationLeftover: Float = 0.0f
    private var podSpeedsMapping: MutableList<PodSpeedPair> = mutableListOf()

    private var lastUpdateTime = System.currentTimeMillis()

    init {
        for (speedSecPerRotation in PODSpeeds) {
            podSpeedsMapping.add(PodSpeedPair(speedSecPerRotation))
        }
    }

    fun getLastDirection() = lastDirection

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

    open fun moveBy(speed: Int, orientedAngle: Int) {
        println("With speed = $speed, pod is rotating by the angle = $orientedAngle")
    }

    private fun updateCurrentState() {
        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - lastUpdateTime) / 1000.0f
        lastUpdateTime = currentTime

        val podTraveledRotation = min(currentPODRotationSpeed * deltaTime, lastRotationLeftover)
        lastRotationLeftover -= podTraveledRotation
        lastDirection += podTraveledRotation
    }

    private fun movePodToSeeTargetPosition(targetPosition: Point, averageSegmentTime: Float) {
        val deltaGradAngle = convertRadianToGrad(targetPosition.getAngle()) - lastDirection
        lastRotationLeftover = deltaGradAngle

        val newPODSpeedSecPerRotation = getTheMostAppropriateSpeed(deltaGradAngle, averageSegmentTime)
        moveBy(newPODSpeedSecPerRotation, lastRotationLeftover.toInt())

        currentPODRotationSpeed = 1.0f / newPODSpeedSecPerRotation
    }

    fun updateAndMovePodToTargetPosition(targetPosition: Point, averageSegmentTime: Float) {
        updateCurrentState()
        movePodToSeeTargetPosition(targetPosition, averageSegmentTime)
    }

    fun updateAndMovePodBy(speed: Int, orientedAngle: Int) { // if we will want to control it manually
        updateCurrentState()
        moveBy(speed, orientedAngle)

        lastRotationLeftover = orientedAngle.toFloat()
        currentPODRotationSpeed = 1.0f / speed
    }
}