package app.hawkeye.balltracker.utils

import android.graphics.Rect
import app.hawkeye.balltracker.processors.utils.ScreenPoint
import kotlin.math.pow

data class AdaptiveScreenPoint(
    val x: Float,
    val y: Float
) {
    fun toScreenPoint(screenWidth: Int, screenHeight: Int): ScreenPoint {
        return ScreenPoint(
            (x * screenWidth).toInt(),
            (y * screenHeight).toInt()
        )
    }

    operator fun times(float: Float): AdaptiveScreenPoint {
        return AdaptiveScreenPoint(x * float, y * float)
    }

    operator fun times(multipliers: Pair<Float, Float>): AdaptiveScreenPoint {
        return AdaptiveScreenPoint(x * multipliers.first, y * multipliers.second)
    }

    operator fun plus(other: AdaptiveScreenPoint): AdaptiveScreenPoint {
        return AdaptiveScreenPoint(x + other.x, y + other.y)
    }

    operator fun minus(other: AdaptiveScreenPoint): AdaptiveScreenPoint {
        return AdaptiveScreenPoint(x - other.x, y - other.y)
    }
}

// coordinates here are parts of current screen \in [0, 1]
data class AdaptiveRect(
    val center: AdaptiveScreenPoint,
    val width: Float,
    val height: Float,
) {
    fun toRect(screenWidth: Int, screenHeight: Int): Rect = Rect(
        (screenWidth * (center.x - width / 2)).toInt(),
        (screenHeight * (center.y - height / 2)).toInt(),
        (screenWidth * (center.x + width / 2)).toInt(),
        (screenHeight * (center.y + height / 2)).toInt()
    )

    fun moveAllToScreen(): AdaptiveRect {
        var resultPoint = center

        if (width >= 1.0f) {
            resultPoint = AdaptiveScreenPoint(0.5f, resultPoint.y)
        } else {
            val deltaX = width / 2

            val left = resultPoint.x - deltaX
            val right = resultPoint.x + deltaX

            if (left < 0.0f) {
                resultPoint = AdaptiveScreenPoint(deltaX, resultPoint.y)
            }
            if (right > 1.0f) {
                resultPoint = AdaptiveScreenPoint(1.0f - deltaX, resultPoint.y)
            }
        }

        if (height >= 1.0f) {
            resultPoint = AdaptiveScreenPoint(resultPoint.x, 0.5f)
        } else {
            val deltaY = height / 2

            val top = resultPoint.y - deltaY
            val bottom = resultPoint.y + deltaY

            if (top < 0.0f) {
                resultPoint = AdaptiveScreenPoint(resultPoint.x, deltaY)
            }
            if (bottom > 1.0f) {
                resultPoint = AdaptiveScreenPoint(resultPoint.x,1.0f - deltaY)
            }
        }

        return AdaptiveRect(
            resultPoint,
            width, height
        )
    }
}

data class ClassifiedBox(
    val adaptiveRect: AdaptiveRect,
    val classId: Int,
    val confidence: Float
) {
    private fun round(number: Double, decimals: Int): Double {
        val multiplier = 10.0.pow(decimals)
        return kotlin.math.round(number * multiplier) / multiplier
    }

    fun getStrInfo(): String {
        val strPosition =
            "topLeft = (${round(adaptiveRect.center.x.toDouble() - adaptiveRect.width / 2, 2)}; ${round(adaptiveRect.center.y.toDouble() - adaptiveRect.height / 2, 2)});\n" +
                    "bottomRight = (${round(adaptiveRect.center.x.toDouble() + adaptiveRect.width / 2, 2)}; ${round(adaptiveRect.center.y.toDouble() + adaptiveRect.height / 2, 2)})"
        val strConfidence = "conf: ${round((confidence * 100).toDouble(), 2)}"
        return "$strPosition\n$classId\n$strConfidence"
    }
}
