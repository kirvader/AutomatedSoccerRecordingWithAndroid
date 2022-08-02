package app.hawkeye.balltracker.utils

import android.graphics.Rect
import kotlin.math.pow

data class ScreenPoint(
    val x: Float,
    val y: Float
)

data class ClassifiedBox(
    val center: ScreenPoint,
    val width: Float,
    val height: Float,
    val classId: Int,
    val confidence: Float
) {
    fun toRect(screenWidth: Int, screenHeight: Int): Rect = Rect(
        (screenWidth * (center.x - width / 2)).toInt(),
        (screenHeight * (center.y - height / 2)).toInt(),
        (screenWidth * (center.x + width / 2)).toInt(),
        (screenHeight * (center.y + height / 2)).toInt()
    )

    private fun round(number: Double, decimals: Int): Double {
        val multiplier = 10.0.pow(decimals)
        return kotlin.math.round(number * multiplier) / multiplier
    }

    fun getStrInfo(): String {
        val strPosition =
            "(${round(center.x.toDouble(), 2)};${round(center.y.toDouble(), 2)})"
        val strConfidence = "conf: ${round((confidence * 100).toDouble(), 2)}"
        return "$strPosition $strConfidence"
    }
}
