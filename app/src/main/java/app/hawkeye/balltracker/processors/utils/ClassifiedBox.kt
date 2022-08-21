package app.hawkeye.balltracker.processors.utils

import android.graphics.Rect
import kotlin.math.pow


data class ClassifiedBox(
    val adaptiveRect: AdaptiveScreenRect,
    val classId: Int,
    val confidence: Float
) {
    private fun round(number: Double, decimals: Int): Double {
        val multiplier = 10.0.pow(decimals)
        return kotlin.math.round(number * multiplier) / multiplier
    }

    fun getStrInfo(): String {
        val strPosition =
            "topLeft = (${round(adaptiveRect.topLeftPoint.x.toDouble(), 2)}; ${round(adaptiveRect.topLeftPoint.y.toDouble(), 2)});\n" +
                    "bottomRight = (${round((adaptiveRect.topLeftPoint.x + adaptiveRect.size.x).toDouble(), 2)}; ${round((adaptiveRect.topLeftPoint.y + adaptiveRect.size.y).toDouble(), 2)})"
        val strConfidence = "conf: ${round((confidence * 100).toDouble(), 2)}"
        return "$strPosition\n$classId\n$strConfidence"
    }
}
