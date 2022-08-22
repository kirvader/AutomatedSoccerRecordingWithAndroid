package com.hawkeye.movement

import com.hawkeye.movement.interfaces.PhysicalObjectMovementModel
import com.hawkeye.movement.utils.AngleMeasure
import com.hawkeye.movement.utils.Point
import java.util.*


class PhysicalObjectMovement(private val relevanceDeltaTime: Long) : PhysicalObjectMovementModel {
    private data class State(
        val position: Point,
        val time_ms: Long
    )

    private var states: Deque<State> = LinkedList()

    override fun updatePositionAtTime(position: Point?, time_ms: Long) {
        if (position == null) {
            return
        }

        if (states.isNotEmpty() && states.first().time_ms >= time_ms) {
            return
        }

        states.addLast(State(position, time_ms))
    }

    private fun updateRelevantStatesFor(time_ms: Long) {
        while (states.isNotEmpty() && states.first().time_ms < time_ms - relevanceDeltaTime) {
            states.removeFirst()
        }
    }

    private fun lerpPoint(
        startPoint: Point,
        finishPoint: Point,
        startTime: Long,
        finishTime: Long,
        currentTime: Long
    ): Point {
        if (currentTime == startTime) {
            return startPoint
        }

        if (currentTime == finishTime) {
            return finishPoint
        }

        val currentTimePartBetweenStartFinish = (currentTime - startTime).toFloat() / (finishTime - startTime)

        return startPoint + (finishPoint - startPoint) * currentTimePartBetweenStartFinish
    }

    override fun getApproximatePositionAtTime(time_ms: Long): Point? {
        updateRelevantStatesFor(time_ms)

        val futureStates = Stack<State>()

        while (states.isNotEmpty() && states.last().time_ms > time_ms) {
            futureStates.add(states.pollLast())
        }

        if (states.isEmpty()) {
            while (futureStates.isNotEmpty()) {
                states.addLast(futureStates.pop())
            }
            return null
        }
        if (futureStates.isEmpty()) {
            if (states.size == 1) {
                return states.last.position
            }
            val lastState = states.pollLast()
            val prelastState = states.pollLast()

            states.add(prelastState)
            states.add(lastState)

            while (futureStates.isNotEmpty()) {
                states.addLast(futureStates.pop())
            }

            val speedVector = (lastState.position - prelastState.position) /
                    (lastState.time_ms - prelastState.time_ms)
            val deltaTime = time_ms - states.last().time_ms

            return lastState.position + speedVector * deltaTime.toFloat()
        }

        val result = lerpPoint(states.last.position, futureStates.peek().position, states.last.time_ms, futureStates.peek().time_ms, time_ms)
        while (futureStates.isNotEmpty()) {
            states.addLast(futureStates.pop())
        }

        return result
    }
}