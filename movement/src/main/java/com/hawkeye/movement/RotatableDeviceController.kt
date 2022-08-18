package com.hawkeye.movement

import com.hawkeye.movement.interfaces.RotatableDevice
import com.hawkeye.movement.interfaces.RotatableDeviceControllerBase
import com.hawkeye.movement.utils.AngleMeasure
import com.hawkeye.movement.utils.Degree
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
        val direction: AngleMeasure,
        val absTime: Long,
        val speed: AngleMeasure,
        val rotationLeftover: AngleMeasure
    )

    private val allStoredStates: Deque<State> = LinkedList()

    private var lastUpdatedState = State(Degree(0.0f), 0L, Degree(0.0f), Degree(0.0f))

    private fun updateRelevantStatesFor(time_ms: Long) {
        while (allStoredStates.isNotEmpty() && allStoredStates.first().absTime < time_ms - relevanceDeltaTime) {
            allStoredStates.removeFirst()
        }
    }

    private fun updateStateAtTime(time_ms: Long) {
        val deltaTime = (time_ms - lastUpdatedState.absTime) / 1000.0f

        val newSpeed: AngleMeasure
        val newRotationLeftover: AngleMeasure
        val newDirection: AngleMeasure


        if (abs((lastUpdatedState.speed * deltaTime).degree()) < abs(lastUpdatedState.rotationLeftover.degree())) {
            newSpeed = lastUpdatedState.speed
            newRotationLeftover =
                lastUpdatedState.rotationLeftover - lastUpdatedState.speed * deltaTime
            newDirection = lastUpdatedState.direction + lastUpdatedState.speed * deltaTime
        } else {
            newSpeed = Degree(0.0f)
            newRotationLeftover = Degree(0.0f)
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

    override fun rotateByAngleAtTime(
        angle: AngleMeasure,
        speed: AngleMeasure,
        currentTime_ms: Long
    ) {
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
            getDeviceDirectionWithConstraints(
                position.getAngle()
            ) - lastUpdatedState.direction
        val deltaTime = (targetTime_ms - currentTime_ms) / 1000.0f

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

    private fun lerpAngle(
        startAngle: AngleMeasure,
        finishAngle: AngleMeasure,
        startTime: Long,
        finishTime: Long,
        currentTime: Long
    ): AngleMeasure {
        if (currentTime == startTime) {
            return startAngle
        }

        if (currentTime == finishTime) {
            return finishAngle
        }

        val currentTimePartBetweenStartFinish = (currentTime - startTime).toFloat() / (finishTime - startTime)

        return startAngle + (finishAngle - startAngle) * currentTimePartBetweenStartFinish
    }

    override fun getDirectionAtTime(absTime: Long): AngleMeasure {
        val stack = Stack<State>()
        while (allStoredStates.isNotEmpty() && allStoredStates.last.absTime > absTime) {
            stack.push(allStoredStates.pollLast())
        }
        if (allStoredStates.isEmpty()) {
            print("Irrelevant time record")
            return lastUpdatedState.direction
        }

        val previousState = allStoredStates.last
        val deltaTimeFromPreviousState = (absTime - previousState.absTime) / 1000.0f

        if (stack.isEmpty()) {

            val deltaAngleFromLastDirection =
                if (abs((previousState.speed * deltaTimeFromPreviousState).degree()) < abs((previousState.rotationLeftover).degree()))
                    previousState.speed * deltaTimeFromPreviousState
                else
                    previousState.rotationLeftover

            return previousState.direction + deltaAngleFromLastDirection
        }

        val nextState = stack.peek()
        while (stack.isNotEmpty()) {
            allStoredStates.addLast(stack.pop())
        }

        return lerpAngle(previousState.direction, nextState.direction, previousState.absTime, nextState.absTime, absTime)
    }
}