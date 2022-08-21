package app.hawkeye.balltracker.processors.tiling_strategies

import app.hawkeye.balltracker.processors.utils.AdaptiveScreenRect
import app.hawkeye.balltracker.processors.utils.ScreenRect
import app.hawkeye.balltracker.utils.createLogger
import kotlin.math.max

private val LOG = createLogger<SingleSquareTiling>()

class SingleSquareTiling : SquareTilingStrategy {
    private var sortedAvailableSquareSideSizes: List<Int> = listOf(64, 128, 256, 512, 640)

    override fun tileRect(
        adaptiveRect: AdaptiveScreenRect,
        imageWidth: Int,
        imageHeight: Int
    ): List<ScreenRect> {
        val rectSize = adaptiveRect.size.toScreenVector(imageWidth, imageHeight)

        val topLeftPoint = adaptiveRect.topLeftPoint.toScreenVector(
            imageWidth,
            imageHeight
        )

        return listOf(
            ScreenRect(
                topLeftPoint,
                sortedAvailableSquareSideSizes.firstOrNull {
                    it >= max(rectSize.x, rectSize.y)
                } ?: sortedAvailableSquareSideSizes.last()
            )
        )
    }
}