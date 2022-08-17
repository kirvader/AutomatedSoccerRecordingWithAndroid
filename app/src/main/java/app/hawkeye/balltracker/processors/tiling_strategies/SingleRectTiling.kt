package app.hawkeye.balltracker.processors.tiling_strategies

import app.hawkeye.balltracker.processors.interfaces.TilingStrategy
import app.hawkeye.balltracker.processors.utils.ScreenRect
import app.hawkeye.balltracker.processors.utils.SegmentProcessorConfig
import app.hawkeye.balltracker.utils.AdaptiveRect
import app.hawkeye.balltracker.utils.createLogger
import kotlin.math.max

private val LOG = createLogger<SingleRectTiling>()

class SingleRectTiling(segmentProcessorInputImageSizes: List<Int>) : TilingStrategy {
    private lateinit var sortedAvailableSquareSideSizes: List<Int>

    init {
        sortedAvailableSquareSideSizes = segmentProcessorInputImageSizes.sorted()
    }

    override fun tileRect(
        adaptiveRect: AdaptiveRect,
        imageWidth: Int,
        imageHeight: Int
    ): List<SegmentProcessorConfig> {
        if (sortedAvailableSquareSideSizes.isEmpty()) {
            return listOf()
        }
        val rectWidth = adaptiveRect.width * imageWidth
        val rectHeight = adaptiveRect.height * imageHeight

        val center = adaptiveRect.center.toScreenPoint(imageWidth, imageHeight)

        LOG.i("tiling the adaptive rect with center (${adaptiveRect.center.x}; ${adaptiveRect.center.y}), width=${adaptiveRect.width}, height=${adaptiveRect.height}")
        LOG.i("tiling the rect with center (${center.x}; ${center.y}), width=$rectWidth, height=$rectHeight")

        val minSideSize = (minSideSizeFactor * max(rectHeight, rectWidth)).toInt()

        val availableAppropriateSize =
            sortedAvailableSquareSideSizes.firstOrNull { it >= minSideSize }
                ?: sortedAvailableSquareSideSizes.last()

        val newScreenRect = ScreenRect(
            center,
            availableAppropriateSize,
            availableAppropriateSize
        ).moveAllToImagePlace(imageWidth, imageHeight)

        LOG.i("${newScreenRect.center.x} ${newScreenRect.center.y}")

        return listOf(
            SegmentProcessorConfig(
                availableAppropriateSize,
                newScreenRect
            )
        )


    }

    companion object {
        private const val minSideSizeFactor = 1.0f
    }
}