package app.hawkeye.balltracker.processors.tiling_strategies

import app.hawkeye.balltracker.processors.utils.AdaptiveScreenRect
import app.hawkeye.balltracker.processors.utils.ScreenRect
import app.hawkeye.balltracker.processors.utils.ScreenVector
import app.hawkeye.balltracker.utils.createLogger
import kotlin.math.max

private val LOG = createLogger<DynamicScaleSingleSquareTiling>()

class DynamicScaleSingleSquareTiling(private val sortedAvailableSquareSideSizes: List<Int>) :
        SquareTilingStrategy {

    private fun ceilBy(number: Int, base: Int): Int {
        if (number % base == 0)
            return (number.div(base) + 1) * base
        return number.floorDiv(base) * base
    }

    private fun floorBy(number: Int, base: Int): Int {
        return number.floorDiv(base) * base
    }

    private fun getSideSizeFittingModelSideSize(
        wholeImageSideSize: Int,
        modelSideSize: Int,
        rectSideSize: Int
    ): Int {
        if (modelSideSize >= rectSideSize) {
            return wholeImageSideSize
        }
        val hypothesis = wholeImageSideSize * modelSideSize / rectSideSize

        return floorBy(hypothesis, 32)
    }

    private fun convertScreenVectorToNewSurface(
        screenVector: ScreenVector,
        oldSurfaceSize: ScreenVector,
        newSurfaceSize: ScreenVector
    ): ScreenVector {
        return screenVector.toAdaptiveScreenVector(oldSurfaceSize.x, oldSurfaceSize.y)
            .toScreenVector(newSurfaceSize.x, newSurfaceSize.y)
    }

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

        val modelSideSize = sortedAvailableSquareSideSizes.lastOrNull() {
            it <= max(rectSize.x, rectSize.y)
        } ?: sortedAvailableSquareSideSizes.last()

        val topLeftCalibrated =
            topLeftPoint.getOnSurface(imageWidth - rectSize.x, imageHeight - rectSize.y)

        val oldResolution = ScreenVector(imageWidth, imageHeight)
        val newResolution = ScreenVector(
            getSideSizeFittingModelSideSize(imageWidth, modelSideSize, rectSize.x),
            getSideSizeFittingModelSideSize(imageHeight, modelSideSize, rectSize.y)
        )

        val newResolutionTopLeft = convertScreenVectorToNewSurface(
            topLeftCalibrated,
            oldResolution,
            newResolution
        ).getOnSurface(newResolution.x - modelSideSize, newResolution.y - modelSideSize)

        return Pair(
            newResolution,
            listOf(
                ScreenRect(
                    newResolutionTopLeft,
                    modelSideSize
                )
            )
        )
    }

}