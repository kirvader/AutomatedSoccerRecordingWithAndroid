package app.hawkeye.balltracker.processors.tiling_strategies

import app.hawkeye.balltracker.configs.objects.TrackingSystemConfigObject
import app.hawkeye.balltracker.processors.utils.AdaptiveScreenRect
import app.hawkeye.balltracker.processors.utils.ScreenRect
import app.hawkeye.balltracker.processors.utils.ScreenVector
import app.hawkeye.balltracker.utils.createLogger
import kotlin.math.max

private val LOG = createLogger<StaticScaleSingleSquareTiling>()

class StaticScaleSingleSquareTiling: SquareTilingStrategy {
    override fun tileRect(
        adaptiveRect: AdaptiveScreenRect,
        imageWidth: Int,
        imageHeight: Int
    ): Pair<ScreenVector, List<ScreenRect>> {
        val croppedBySurface = adaptiveRect.getCropped()

        val rectSize = croppedBySurface.size.toScreenVector(imageWidth, imageHeight)

        val topLeftPoint = croppedBySurface.topLeftPoint.toScreenVector(
            imageWidth,
            imageHeight
        )

        val tilingSideSize = TrackingSystemConfigObject.availableModelSideSizes.firstOrNull {
            it >= max(rectSize.x, rectSize.y)
        } ?: TrackingSystemConfigObject.availableModelSideSizes.last()

        val topLeftCalibrated = topLeftPoint.getOnSurface(imageWidth - tilingSideSize, imageHeight - tilingSideSize)

        return Pair(
            ScreenVector(imageWidth, imageHeight), listOf(
            ScreenRect(
                topLeftCalibrated,
                tilingSideSize
            )
        ))
    }
}