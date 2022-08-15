package app.hawkeye.balltracker.utils

import android.graphics.Rect
import kotlin.math.pow

data class ScreenPoint(
    val x: Float,
    val y: Float
) {
    operator fun times(float: Float): ScreenPoint {
        return ScreenPoint(x * float, y * float)
    }

    operator fun times(multipliers: Pair<Float, Float>): ScreenPoint {
        return ScreenPoint(x * multipliers.first, y * multipliers.second)
    }

    operator fun plus(other: ScreenPoint): ScreenPoint {
        return ScreenPoint(x + other.x, y + other.y)
    }

    operator fun minus(other: ScreenPoint): ScreenPoint {
        return ScreenPoint(x - other.x, y - other.y)
    }
}

// coordinates here are parts of current screen \in [0, 1]
data class AdaptiveRect(
    val center: ScreenPoint,
    val width: Float,
    val height: Float,
) {
    fun toRect(screenWidth: Int, screenHeight: Int): Rect = Rect(
        (screenWidth * (center.x - width / 2)).toInt(),
        (screenHeight * (center.y - height / 2)).toInt(),
        (screenWidth * (center.x + width / 2)).toInt(),
        (screenHeight * (center.y + height / 2)).toInt()
    )
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
