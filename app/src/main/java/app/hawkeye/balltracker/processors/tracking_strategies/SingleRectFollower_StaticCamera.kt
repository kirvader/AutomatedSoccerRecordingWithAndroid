package app.hawkeye.balltracker.processors.tracking_strategies

import app.hawkeye.balltracker.processors.utils.*
import app.hawkeye.balltracker.utils.createLogger

private val LOG = createLogger<SingleRectFollower_StaticCamera>()

class SingleRectFollower_StaticCamera : ExpGrowingArea() {
    override fun guessDetectionAreaCenterIfChanging(absTime_ms: Long): AdaptiveScreenVector? {
        return super.guessDetectionAreaCenterIfChanging(absTime_ms)
    }
}