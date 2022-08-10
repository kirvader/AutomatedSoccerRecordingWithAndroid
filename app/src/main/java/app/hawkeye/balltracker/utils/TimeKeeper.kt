package app.hawkeye.balltracker.utils

class TimeKeeper {
    private var currentTime = 0L
    private var averageDeltaTimeBetweenCalls = 0.0f

    init {
        currentTime = System.currentTimeMillis()
    }

    private fun getDeltaTimeFromLastCall(): Float {
        val newTime = System.currentTimeMillis()
        val deltaTime = newTime - currentTime
        currentTime = newTime
        return deltaTime.toFloat() / 1000
    }

    fun getInfo() : String {
        val lastDelta = getDeltaTimeFromLastCall()
        averageDeltaTimeBetweenCalls = (lastDelta + averageDeltaTimeBetweenCalls) / 2
        return "avg = $averageDeltaTimeBetweenCalls; $lastDelta"
    }
}