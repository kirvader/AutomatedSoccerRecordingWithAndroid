package app.hawkeye.balltracker.processors.interfaces

import app.hawkeye.balltracker.processors.utils.SegmentProcessorConfig
import app.hawkeye.balltracker.utils.AdaptiveRect

interface TilingStrategy {

    fun tileRect(adaptiveRect: AdaptiveRect, imageWidth: Int, imageHeight: Int): List<SegmentProcessorConfig>
}