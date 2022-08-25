package app.hawkeye.balltracker.controllers.time

interface TimeKeeperBase {

    fun getCurrentCircleStartTime(): Long

    fun registerCircle()

    fun getInfoAboutLastCircle() : String

    fun getApproximateCircleTime(): Long
}