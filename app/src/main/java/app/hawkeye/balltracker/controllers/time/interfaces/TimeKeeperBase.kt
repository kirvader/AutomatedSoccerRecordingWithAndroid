package app.hawkeye.balltracker.controllers.time.interfaces

interface TimeKeeperBase {

    fun getCurrentCircleStartTime(): Long

    fun registerCircle()

    fun getInfoAboutLastCircle() : String
}