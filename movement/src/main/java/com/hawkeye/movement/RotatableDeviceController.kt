package com.hawkeye.movement

import com.hawkeye.movement.interfaces.RotatableDevice
import com.hawkeye.movement.interfaces.RotatableDeviceControllerBase
import com.hawkeye.movement.utils.Point
import kotlin.math.abs

class RotatableDeviceController(private val device: RotatableDevice) :
    RotatableDeviceControllerBase {

    /**
     * Stores information about rotatable device state in grads and ms
     */
    private data class State(
        val direction: Float,
        val absTime: Long,
        val speed: Float,
        val rotationLeftover: Float
    )

    private var lastUpdatedState = State(0.0f, 0L, 0.0f, 0.0f)

    private fun updateStateAtTime(time_ms: Long) {
        val deltaTime = time_ms - lastUpdatedState.absTime
        if (deltaTime <= 0) {
            return
        }

        var newSpeed = 0.0f
        var newRotationLeftover = 0.0f
        var newDirection = 0.0f


        if (abs(lastUpdatedState.speed * deltaTime) < abs(lastUpdatedState.rotationLeftover)) {
            newSpeed = lastUpdatedState.speed
            newRotationLeftover = lastUpdatedState.rotationLeftover - lastUpdatedState.speed * deltaTime
            newDirection = lastUpdatedState.direction + lastUpdatedState.speed * deltaTime
        } else {
            newSpeed = 0.0f
            newRotationLeftover = 0.0f
            newDirection = lastUpdatedState.direction + lastUpdatedState.rotationLeftover
        }

        lastUpdatedState = State(
            newDirection,
            time_ms,
            newSpeed,
            newRotationLeftover
        )
    }

    override fun rotateByAngleAtTime(angle: Float, speed: Float, currentTime_ms: Long) {
        updateStateAtTime(currentTime_ms)

        val checkedDeltaAngle = getDeviceDirectionWithConstraints(lastUpdatedState.direction + angle) - lastUpdatedState.direction

        val availableSpeed = device.getTheMostAppropriateSpeedFromAvailable(speed)

        device.rotateBy(availableSpeed, checkedDeltaAngle)

        lastUpdatedState = State(
            lastUpdatedState.direction,
            currentTime_ms,
            device.getGradPerSecSpeedFromAvailable(availableSpeed),
            checkedDeltaAngle
        )
    }

    override fun smoothlyDirectDeviceAt(position: Point, currentTime_ms: Long, targetTime_ms: Long) {
        updateStateAtTime(currentTime_ms)

        val checkedDeltaAngle = getDeviceDirectionWithConstraints(position.getAngle()) - lastUpdatedState.direction
        val deltaTime = targetTime_ms - currentTime_ms

        val availableSpeed = device.getTheMostAppropriateSpeedFromAvailable(checkedDeltaAngle / deltaTime)

        device.rotateBy(availableSpeed, checkedDeltaAngle)

        lastUpdatedState = State(
            lastUpdatedState.direction,
            currentTime_ms,
            device.getGradPerSecSpeedFromAvailable(availableSpeed),
            checkedDeltaAngle
        )

    }


}