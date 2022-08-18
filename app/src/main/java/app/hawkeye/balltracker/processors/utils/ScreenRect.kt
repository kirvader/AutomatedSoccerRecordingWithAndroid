package app.hawkeye.balltracker.processors.utils

import app.hawkeye.balltracker.utils.AdaptiveRect
import java.lang.Exception
import kotlin.math.max
import kotlin.math.min


data class ScreenRect(
    val center: ScreenPoint,
    val width: Int,
    val height: Int
) {
    fun scaleBy(scalePair: Pair<Float, Float>): ScreenRect {
        return ScreenRect(
            center.scaleBy(scalePair),
            (width * scalePair.first).toInt(),
            (height * scalePair.second).toInt()
        )
    }

    fun canBeTiledBySquareWithSide(sideSize: Int): Boolean =
        (width <= sideSize) && (height <= sideSize)


    fun toAdaptive(imageWidth: Int, imageHeight: Int): AdaptiveRect {
        return AdaptiveRect(
            center.toAdaptive(imageWidth, imageHeight),
            width.toFloat() / imageWidth,
            height.toFloat() / imageHeight
        )
    }

    fun cropByImageSize(imageWidth: Int, imageHeight: Int): ScreenRect {
        val deltaX = width / 2

        val newLeft = max(center.x - deltaX, 0)
        val newRight = min(center.x + deltaX, imageWidth - 1)

        val deltaY = height / 2

        val newTop = max(center.y - deltaY, 0)
        val newBottom = min(center.y + deltaY, imageHeight - 1)

        return ScreenRect(
            ScreenPoint((newLeft + newRight) / 2, (newBottom + newTop) / 2),
            newRight - newLeft,
            newBottom - newTop
        )
    }

    fun moveAllToImagePlace(imageWidth: Int, imageHeight: Int): ScreenRect {
        var newLeft = 0
        var newRight = imageWidth - 1

        var newTop = 0
        var newBottom = imageHeight - 1

        if (width > imageWidth) {
            throw Exception("Current width exceeds image's width.")
        }
        if (height > imageHeight) {
            throw Exception("Current height exceeds image's height.")
        }

        val deltaX = width / 2

        newLeft = center.x - deltaX
        newRight = center.x + deltaX

        if (newLeft < 0) {
            newRight = width - 1
            newLeft = 0
        }
        if (newRight > imageWidth) {
            newRight = imageWidth - 1
            newLeft = imageWidth - width
        }

        val deltaY = height / 2

        newTop = center.y - deltaY
        newBottom = center.y + deltaY

        if (newTop < 0) {
            newBottom = height - 1
            newTop = 0
        }
        if (newRight > imageHeight) {
            newBottom = imageHeight - 1
            newTop = imageHeight - height
        }
        return ScreenRect(
            ScreenPoint((newLeft + newRight) / 2, (newBottom + newTop) / 2),
            width,
            height
        )
    }
}
