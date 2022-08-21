package app.hawkeye.balltracker.processors.tiling_strategies

import app.hawkeye.balltracker.processors.utils.AdaptiveScreenRect
import app.hawkeye.balltracker.processors.utils.ScreenRect

interface SquareTilingStrategy {

    fun tileRect(adaptiveRect: AdaptiveScreenRect, imageWidth: Int, imageHeight: Int): List<ScreenRect>
}