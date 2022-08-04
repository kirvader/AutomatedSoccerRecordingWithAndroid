package app.hawkeye.balltracker.utils

import android.graphics.Rect
import kotlin.math.pow

data class ScreenPoint(
    val x: Float,
    val y: Float
)

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
            "(${round(adaptiveRect.center.x.toDouble(), 2)};${round(adaptiveRect.center.y.toDouble(), 2)})"
        val strConfidence = "conf: ${round((confidence * 100).toDouble(), 2)}"
        return "$strPosition $strConfidence"
    }
}
