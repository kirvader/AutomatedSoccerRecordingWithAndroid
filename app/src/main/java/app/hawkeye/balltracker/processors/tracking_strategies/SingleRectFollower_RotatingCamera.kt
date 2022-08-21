package app.hawkeye.balltracker.processors.tracking_strategies


import app.hawkeye.balltracker.processors.utils.AdaptiveScreenRect
import app.hawkeye.balltracker.processors.utils.AdaptiveScreenVector
import app.hawkeye.balltracker.utils.createLogger
import kotlin.math.*

private val LOG = createLogger<SingleRectFollower_RotatingCamera>()

class SingleRectFollower_RotatingCamera(
    private val getBallScreenPositionAtTime: (Long) -> AdaptiveScreenVector?
) : ExpGrowingArea() {
    override fun guessDetectionAreaCenterIfChanging(absTime_ms: Long): AdaptiveScreenVector? {
        return getBallScreenPositionAtTime(absTime_ms)
    }
}