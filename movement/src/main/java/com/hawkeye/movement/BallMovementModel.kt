package com.hawkeye.movement

import com.hawkeye.movement.utils.Point


/**
 * @param endPoint - point in which ball was found
 * @param time - time spent from last segment ending
 */
class SegmentInfo(
    val endPoint: Point? = null,
    val startPoint: Point? = null,
    val time: Float = 1.0f
) {
    fun getAverageSpeed() =
        if (endPoint != null && startPoint != null) {
            (endPoint - startPoint) / time
        } else {
            null
        }


}


class BallMovementModel {
    private var lastSegments: MutableList<SegmentInfo> =
        mutableListOf(SegmentInfo(), SegmentInfo())
    private var lastVelocities: MutableList<Point?> = mutableListOf(null, null)
    private var lastAcceleration: Point? = null

    fun updateModelState(point: Point?, segmentTime: Float) {
        lastSegments[0] = lastSegments[1]
        lastSegments.add(
            SegmentInfo(
                endPoint = point,
                startPoint = lastSegments.last().endPoint,
                time = segmentTime
            )
        )

        lastVelocities[0] = lastVelocities[1]
        lastVelocities.add(lastSegments.last().getAverageSpeed())

        if (lastVelocities.fold(true) { acc, p -> acc && (p != null) }) {
            lastAcceleration =
                (lastVelocities[1]!! - lastVelocities[0]!!) / getAverageSegmentTime()
        } else {
            lastAcceleration = null
        }
    }

    fun getAverageSegmentTime(): Float = (lastSegments[0].time + lastSegments[1].time) / 2.0f

    /**
     * @return returns approximated ball position after segmentsQuantity of frames
     */
    fun getApproximatedBallPosition(segmentsQuantity: Int = 2): Point? {

        val segmentAverageTime = getAverageSegmentTime()

        val averageTime = segmentAverageTime * segmentsQuantity
        if (lastAcceleration != null) {

            // if we were able to count acceleration then it means that we know all the
            // parameters for 2 consecutive segments including velocity and last position
            return lastSegments.last().endPoint!! +
                    lastVelocities.last()!! * averageTime +
                    lastAcceleration!! * averageTime * averageTime / 2.0f
        }
        if (lastVelocities[1] != null) {

            // if we only know data about last segment then we can
            // try to predict next position based only on velocity
            // (Maybe it will be good to add some fitting coefficient here)
            return lastSegments[1].endPoint!! +
                    lastVelocities[1]!! * averageTime
        }
        if (lastVelocities[0] != null) {

            // There I'm hencing that ball continued it's movement with it's speed
            return lastSegments[0].endPoint!! +
                    lastVelocities[0]!! * (averageTime + segmentAverageTime)
        }
        // after this point we can only rely on point and move to it
        if (lastSegments[1].endPoint != null) {
            return lastSegments[1].endPoint
        } else if (lastSegments[1].startPoint != null) {
            return lastSegments[1].startPoint
        } else if (lastSegments[0].startPoint != null) {
            return lastSegments[0].startPoint
        } else {
            return null
        }
    }
}