package app.hawkeye.balltracker.controllers.time

import app.hawkeye.balltracker.controllers.time.interfaces.TimeKeeperBase

class TimeKeeper : TimeKeeperBase {
    private var currentTime = 0L
    private var lastDelta = 0L
    private var averageDeltaTimeBetweenCalls = 0L

    init {
        currentTime = System.currentTimeMillis()
    }

    override fun getCurrentCircleStartTime() = currentTime

    override fun registerCircle() {
        val newCurrentTime = System.currentTimeMillis()

        lastDelta = newCurrentTime - currentTime
        currentTime = newCurrentTime
        averageDeltaTimeBetweenCalls = (lastDelta + averageDeltaTimeBetweenCalls) / 2
    }

    override fun getInfoAboutLastCircle() : String {
        return "avg = ${averageDeltaTimeBetweenCalls}ms; ${lastDelta}ms"
    }
}