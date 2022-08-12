package com.hawkeye.movement

import com.hawkeye.movement.interfaces.RotatableDevice
import com.hawkeye.movement.interfaces.RotatableDeviceControllerBase
import com.hawkeye.movement.utils.Point
import java.util.*
import kotlin.math.abs

class RotatableDeviceController(
    private val device: RotatableDevice,
    private val relevanceDeltaTime: Long
) :
    RotatableDeviceControllerBase {
    /**
     * Stores information about one rotatable device state in grads and ms
     */
    private data class State(
        val direction: Float,
        val absTime: Long,
        val speed: Float,
        val rotationLeftover: Float
    )

    private val allStoredStates: Deque<State> = LinkedList()

    private var lastUpdatedState = State(0.0f, 0L, 0.0f, 0.0f)

    private fun updateRelevantStatesFor(time_ms: Long) {
        while (allStoredStates.isNotEmpty() && allStoredStates.first().absTime < time_ms - relevanceDeltaTime) {
            allStoredStates.removeFirst()
        }
    }

    private fun updateStateAtTime(time_ms: Long) {
        val deltaTime = time_ms - lastUpdatedState.absTime

        var newSpeed = 0.0f
        var newRotationLeftover = 0.0f
        var newDirection = 0.0f


        if (abs(lastUpdatedState.speed * deltaTime) < abs(lastUpdatedState.rotationLeftover)) {
            newSpeed = lastUpdatedState.speed
            newRotationLeftover =
                lastUpdatedState.rotationLeftover - lastUpdatedState.speed * deltaTime
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
        allStoredStates.addLast(lastUpdatedState)
    }

    override fun rotateByAngleAtTime(angle: Float, speed: Float, currentTime_ms: Long) {
        if (currentTime_ms < lastUpdatedState.absTime) {
            print("Can't change device state in the past.")
            return
        }

        updateRelevantStatesFor(currentTime_ms)

        updateStateAtTime(currentTime_ms)

        val checkedDeltaAngle =
            getDeviceDirectionWithConstraints(lastUpdatedState.direction + angle) - lastUpdatedState.direction

        val availableSpeed = device.getTheMostAppropriateSpeedFromAvailable(speed)

        device.rotateBy(availableSpeed, checkedDeltaAngle)

        lastUpdatedState = State(
            lastUpdatedState.direction,
            currentTime_ms,
            device.getGradPerSecSpeedFromAvailable(availableSpeed),
            checkedDeltaAngle
        )
        allStoredStates.addLast(lastUpdatedState)
    }

    override fun smoothlyDirectDeviceAt(
        position: Point,
        currentTime_ms: Long,
        targetTime_ms: Long
    ) {
        if (currentTime_ms < lastUpdatedState.absTime) {
            print("Can't change device state in the past.")
            return
        }

        updateRelevantStatesFor(currentTime_ms)

        updateStateAtTime(currentTime_ms)

        val checkedDeltaAngle =
            getDeviceDirectionWithConstraints(position.getAngle()) - lastUpdatedState.direction
        val deltaTime = targetTime_ms - currentTime_ms

        val availableSpeed =
            device.getTheMostAppropriateSpeedFromAvailable(checkedDeltaAngle / deltaTime)

        device.rotateBy(availableSpeed, checkedDeltaAngle)

        lastUpdatedState = State(
            lastUpdatedState.direction,
            currentTime_ms,
            device.getGradPerSecSpeedFromAvailable(availableSpeed),
            checkedDeltaAngle
        )
        allStoredStates.addLast(lastUpdatedState)

    }

    override fun getDirectionAtTime(absTime: Long): Float {
        val stack = Stack<State>()
        while (allStoredStates.isNotEmpty() && allStoredStates.last.absTime > absTime) {
            stack.push(allStoredStates.pollLast())
        }
        if (allStoredStates.isEmpty()) {
            print("Irrelevant time record")
            return lastUpdatedState.direction
        }

        val previousState = allStoredStates.last
        val deltaTimeFromPreviousState = absTime - previousState.absTime

        if (stack.isEmpty()) {

            val deltaAngleFromLastDirection =
                if (abs(previousState.speed * deltaTimeFromPreviousState) < abs(previousState.rotationLeftover))
                    previousState.speed * deltaTimeFromPreviousState
                else
                    previousState.rotationLeftover

            return previousState.direction + deltaAngleFromLastDirection
        }

        val nextState = stack.peek()
        while (stack.isNotEmpty()) {
            allStoredStates.addLast(stack.pop())
        }

        if (absTime == previousState.absTime) {
            return previousState.direction
        }

        if (absTime == nextState.absTime) {
            return nextState.direction
        }

        return previousState.direction + (nextState.direction - previousState.direction) * deltaTimeFromPreviousState / (nextState.absTime - previousState.absTime)
    }
}