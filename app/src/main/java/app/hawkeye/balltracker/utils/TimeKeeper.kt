package app.hawkeye.balltracker.utils

class TimeKeeper {
    private var currentTime = 0L
    private var lastDelta = 0L
    private var averageDeltaTimeBetweenCalls = 0L

    init {
        currentTime = System.currentTimeMillis()
    }

    fun getCurrentCircleStartTime() = currentTime

    fun registerCircle() {
        val newCurrentTime = System.currentTimeMillis()

        lastDelta = newCurrentTime - currentTime
        currentTime = newCurrentTime
        averageDeltaTimeBetweenCalls = (lastDelta + averageDeltaTimeBetweenCalls) / 2
    }

    fun getInfoAboutLastCircle() : String {
        return "avg = ${averageDeltaTimeBetweenCalls}ms; ${lastDelta}ms"
    }
}