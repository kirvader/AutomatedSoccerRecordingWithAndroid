package app.hawkeye.balltracker.processors.utils

import android.graphics.Rect


class ScreenRect(left: Int, top: Int, width: Int, height: Int) {
    val topLeftPoint: ScreenVector
    val size: ScreenVector

    init {
        topLeftPoint = ScreenVector(left, top)
        size = ScreenVector(width, height)
    }

    constructor(left: Int, top: Int, sideSize: Int): this(left, top, sideSize, sideSize)

    constructor(topLeftPoint: ScreenVector, sideSize: Int): this(topLeftPoint.x, topLeftPoint.y, sideSize, sideSize)

    constructor(topLeftPoint: ScreenVector, size: ScreenVector): this(topLeftPoint.x, topLeftPoint.y, size.x, size.y)

    fun scaleBy(scalePair: Pair<Float, Float>): ScreenRect {
        return ScreenRect(
            topLeftPoint.getScaled(scalePair),
            size.getScaled(scalePair)
        )
    }

    fun toAdaptiveScreenRect(resolution: Pair<Int, Int>): AdaptiveScreenRect {
        return this.toAdaptiveScreenRect(resolution.first, resolution.second)
    }

    fun toAdaptiveScreenRect(resolution: ScreenVector): AdaptiveScreenRect {
        return this.toAdaptiveScreenRect(resolution.x, resolution.y)
    }

    fun toAdaptiveScreenRect(resolutionX: Int, resolutionY: Int): AdaptiveScreenRect {
        return AdaptiveScreenRect(
            topLeftPoint.toAdaptiveScreenVector(resolutionX, resolutionY),
            size.toAdaptiveScreenVector(resolutionX, resolutionY)
        )
    }

    fun isSquare() = (size.x == size.y)

    fun toRect(): Rect {
        val bottomRight = topLeftPoint + size
        return Rect(topLeftPoint.x, topLeftPoint.y, bottomRight.x, bottomRight.y)
    }
}
