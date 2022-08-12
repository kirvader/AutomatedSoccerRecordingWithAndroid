package com.hawkeye.movement

import com.hawkeye.movement.interfaces.PhysicalObjectMovementModel
import com.hawkeye.movement.utils.Point
import java.util.*


class PhysicalObjectMovement : PhysicalObjectMovementModel {
    private data class State(
        val position: Point,
        val time_ms: Long
    )

    private var states: Deque<State> = LinkedList()

    override fun updatePositionAtTime(position: Point?, time_ms: Long) {
        if (position == null) {
            return
        }

        if (states.first().time_ms >= time_ms) {
            return
        }

        states.addLast(State(position, time_ms))
    }

    private fun updateRelevantStatesFor(time_ms: Long) {
        if (states.isEmpty()) return
        while (states.first().time_ms < time_ms - relevanceTimeMs) {
            states.removeFirst()
        }
    }

    override fun getApproximatePositionAtTime(time_ms: Long): Point? {
        updateRelevantStatesFor(time_ms)

        val lastState = states.pollLast() ?: return null

        val prelastState = states.pollLast() ?: return lastState.position

        val speedVector = (lastState.position - prelastState.position) /
                (lastState.time_ms - prelastState.time_ms)
        val deltaTime = time_ms - states.first().time_ms

        return lastState.position + speedVector * deltaTime.toFloat()
    }

    companion object {
        private const val relevanceTimeMs = 1000
    }
}