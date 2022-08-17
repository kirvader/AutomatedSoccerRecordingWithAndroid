package app.hawkeye.balltracker.processors.utils

import app.hawkeye.balltracker.utils.AdaptiveScreenPoint

data class ScreenPoint(
    val x: Int,
    val y: Int
) {
    fun toAdaptive(imageWidth: Int, imageHeight: Int): AdaptiveScreenPoint {
        return AdaptiveScreenPoint(x.toFloat() / imageWidth, y.toFloat() / imageHeight)
    }
}
