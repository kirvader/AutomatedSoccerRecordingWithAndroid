package app.pivo.android.basicsdkdemo.utils

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
)