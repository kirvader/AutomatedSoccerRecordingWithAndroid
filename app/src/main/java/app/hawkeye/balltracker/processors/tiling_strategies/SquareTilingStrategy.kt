package app.hawkeye.balltracker.processors.tiling_strategies

import app.hawkeye.balltracker.processors.utils.AdaptiveScreenRect
import app.hawkeye.balltracker.processors.utils.ScreenRect
import app.hawkeye.balltracker.processors.utils.ScreenVector

interface SquareTilingStrategy {
    fun tileRect(adaptiveRect: AdaptiveScreenRect, imageWidth: Int, imageHeight: Int): Pair<ScreenVector, List<ScreenRect>>
}