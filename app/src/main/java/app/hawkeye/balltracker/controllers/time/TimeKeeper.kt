package app.hawkeye.balltracker.controllers.time

import java.util.*

class TimeKeeper : TimeKeeperBase {
    private var currentTime = 0L
    private var lastDelta = 0L
    private val circleStarts: Queue<Long> = LinkedList()

    private val consecutiveCirclesQuantity = 5


    init {
        currentTime = System.currentTimeMillis()
        circleStarts.add(currentTime)
    }

    override fun getCurrentCircleStartTime() = currentTime

    override fun registerCircle() {
        val newCurrentTime = System.currentTimeMillis()

        lastDelta = newCurrentTime - currentTime
        currentTime = newCurrentTime
        circleStarts.add(currentTime)
        if (circleStarts.size > consecutiveCirclesQuantity) {
            circleStarts.remove()
        }

    }

    override fun getInfoAboutLastCircle() : String {
        return "avg = ${getApproximateCircleTime()}ms; ${lastDelta}ms"
    }

    override fun getApproximateCircleTime(): Long {
        return (circleStarts.last() - circleStarts.first()) / circleStarts.size
    }
}