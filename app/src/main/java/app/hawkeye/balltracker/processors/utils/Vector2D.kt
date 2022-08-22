package app.hawkeye.balltracker.processors.utils

import kotlin.math.max
import kotlin.math.min

data class AdaptiveScreenVector(
    val x: Float,
    val y: Float
) {
    fun toScreenVector(resolutionX: Int, resolutionY: Int): ScreenVector {
        return ScreenVector((resolutionX * x).toInt(), (resolutionY * y).toInt())
    }

    fun getScaled(scaleFactorX: Float, scaleFactorY: Float): AdaptiveScreenVector {
        return AdaptiveScreenVector(x * scaleFactorX, y * scaleFactorY)
    }

    fun getScaled(scaleFactorPair: Pair<Float, Float>): AdaptiveScreenVector {
        return getScaled(scaleFactorPair.first, scaleFactorPair.second)
    }

    fun getOntoSurface(): AdaptiveScreenVector {
        return AdaptiveScreenVector(
            max(0f, min(x, 1f)),
            max(0f, min(y, 1f))
        )
    }

    operator fun times(float: Float): AdaptiveScreenVector {
        return AdaptiveScreenVector(x * float, y * float)
    }

    operator fun times(multipliers: Pair<Float, Float>): AdaptiveScreenVector {
        return AdaptiveScreenVector(x * multipliers.first, y * multipliers.second)
    }

    operator fun plus(other: AdaptiveScreenVector): AdaptiveScreenVector {
        return AdaptiveScreenVector(x + other.x, y + other.y)
    }

    operator fun minus(other: AdaptiveScreenVector): AdaptiveScreenVector {
        return AdaptiveScreenVector(x - other.x, y - other.y)
    }
}

data class ScreenVector(
    val x: Int = 0,
    val y: Int = 0
) {
    fun toAdaptiveScreenVector(resolutionX: Int, resolutionY: Int): AdaptiveScreenVector {
        return AdaptiveScreenVector(x.toFloat() / resolutionX, y.toFloat() / resolutionY)
    }

    fun getScaled(scaleFactorX: Float, scaleFactorY: Float): ScreenVector {
        return ScreenVector((x * scaleFactorX).toInt(), (y * scaleFactorY).toInt())
    }

    fun getScaled(scaleFactorPair: Pair<Float, Float>): ScreenVector {
        return getScaled(scaleFactorPair.first, scaleFactorPair.second)
    }

    fun getOnSurface(width: Int, height: Int): ScreenVector {
        return ScreenVector(
            max(0, min(width - 1, x)),
            max(0, min(height - 1, y)),
        )
    }


    operator fun times(float: Float): ScreenVector {
        return ScreenVector((x * float).toInt(), (y * float).toInt())
    }

    operator fun times(multipliers: Pair<Float, Float>): ScreenVector {
        return ScreenVector((x * multipliers.first).toInt(), (y * multipliers.second).toInt())
    }

    operator fun plus(other: ScreenVector): ScreenVector {
        return ScreenVector(x + other.x, y + other.y)
    }

    operator fun minus(other: ScreenVector): ScreenVector {
        return ScreenVector(x - other.x, y - other.y)
    }
}
