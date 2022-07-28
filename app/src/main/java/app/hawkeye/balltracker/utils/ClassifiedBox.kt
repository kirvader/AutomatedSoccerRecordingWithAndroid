package app.hawkeye.balltracker.utils

import android.graphics.Rect

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
}
