package app.hawkeye.balltracker.processors.utils

import kotlin.math.max
import kotlin.math.min


class AdaptiveScreenRect(left: Float, top: Float, width: Float, height: Float) {
    val topLeftPoint: AdaptiveScreenVector
    val size: AdaptiveScreenVector

    init {
        topLeftPoint = AdaptiveScreenVector(left, top)
        size = AdaptiveScreenVector(width, height)
    }

    constructor(topLeftPoint: AdaptiveScreenVector, size: AdaptiveScreenVector): this(topLeftPoint.x, topLeftPoint.y, size.x, size.y)

    fun toScreenRect(resolution: Pair<Int, Int>): ScreenRect {
        return this.toScreenRect(resolution.first, resolution.second)
    }

    fun toScreenRect(resolution: ScreenVector): ScreenRect {
        return this.toScreenRect(resolution.x, resolution.y)
    }

    fun toScreenRect(resolutionX: Int, resolutionY: Int): ScreenRect {
        return ScreenRect(
            topLeftPoint.toScreenVector(resolutionX, resolutionY),
            size.toScreenVector(resolutionX, resolutionY)
        )
    }

    fun getCenter(): AdaptiveScreenVector {
        return topLeftPoint + size * 0.5f
    }

    fun getCropped(): AdaptiveScreenRect {
        val topLeftOntoSurface = topLeftPoint.getOntoSurface()
        val bottomRightOntoSurface = (topLeftPoint + size).getOntoSurface()

        val sizeOnSurface = bottomRightOntoSurface - topLeftOntoSurface
        return AdaptiveScreenRect(
            left = topLeftOntoSurface.x,
            top = topLeftOntoSurface.y,
            width = sizeOnSurface.x,
            height = sizeOnSurface.y
        )
    }
}
