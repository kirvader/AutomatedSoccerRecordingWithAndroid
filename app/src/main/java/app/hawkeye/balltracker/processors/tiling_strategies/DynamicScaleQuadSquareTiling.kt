package app.hawkeye.balltracker.processors.tiling_strategies

import app.hawkeye.balltracker.processors.utils.AdaptiveScreenRect
import app.hawkeye.balltracker.processors.utils.ScreenRect
import app.hawkeye.balltracker.processors.utils.ScreenVector

class DynamicScaleQuadSquareTiling: SquareTilingStrategy {
    private val singleSquareTiler: SquareTilingStrategy

    init {
        singleSquareTiler = DynamicScaleSingleSquareTiling()
    }

    override fun tileRect(
        adaptiveRect: AdaptiveScreenRect,
        imageWidth: Int,
        imageHeight: Int
    ): Pair<ScreenVector, List<ScreenRect>> {

        val (newResolution, tiling) = singleSquareTiler.tileRect(adaptiveRect, imageWidth, imageHeight)

        if (tiling.isEmpty()) {
            return Pair(ScreenVector(imageWidth, imageHeight), listOf())
        }
        val tilingScreenRect = tiling[0]

        if (!tilingScreenRect.isSquare()) {
            return Pair(ScreenVector(imageWidth, imageHeight), listOf())
        }

        if (tilingScreenRect.size.x == 64) {
            return Pair(newResolution, tiling)
        }
        val newRectSize = tilingScreenRect.size.x / 2
        val topLeftRect = ScreenRect(tilingScreenRect.topLeftPoint, newRectSize)
        val topRightRect = ScreenRect(tilingScreenRect.topLeftPoint + ScreenVector(tilingScreenRect.size.x - newRectSize, 0), newRectSize)
        val bottomLeftRect = ScreenRect(tilingScreenRect.topLeftPoint + ScreenVector(0, tilingScreenRect.size.y - newRectSize), newRectSize)
        val bottomRightRect = ScreenRect(tilingScreenRect.topLeftPoint + ScreenVector(tilingScreenRect.size.x - newRectSize, tilingScreenRect.size.y - newRectSize), newRectSize)

        return Pair(newResolution, listOf(topLeftRect, topRightRect, bottomLeftRect, bottomRightRect))
    }

}