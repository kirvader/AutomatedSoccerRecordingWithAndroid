package app.pivo.android.basicsdkdemo.movementController

import kotlin.math.*
import kotlin.system.exitProcess

fun convertGradToRadian(phi: Double) = phi / 180 * PI
fun convertRadianToGrad(phi: Double) = phi / PI * 180

class PolarPoint(
    val d: Double = 0.0,
    val phi: Double = 0.0,
    val h: Double = 0.0
) {
    fun getXProjection() = d * cos(phi)
    fun getYProjection() = d * sin(phi)

    fun toCartesian(): CartesianPoint = CartesianPoint(getXProjection(), getYProjection(), h)

}

class CartesianPoint(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val h: Double = 0.0
) {
    fun getLength() = sqrt(x * x + y * y)

    fun toPolar(): PolarPoint = PolarPoint(getLength(), getAngle(), h)

    private fun getAngle(): Double = atan2(y, x)

    operator fun div(d: Double): CartesianPoint {
        try {
            return CartesianPoint(x / d, y / d, h / d)
        } catch (ex: Exception) {
            println(ex)
            exitProcess(1)
        }
    }

    operator fun minus(other: CartesianPoint): CartesianPoint =
        CartesianPoint(x - other.x, y - other.y, h - other.h)

    operator fun plus(other: CartesianPoint): CartesianPoint =
        CartesianPoint(x + other.x, y + other.y, h + other.h)

    operator fun times(d: Double): CartesianPoint =
        CartesianPoint(x * d, y * d, h * d)
}

class Point {
    private var polarPoint: PolarPoint = PolarPoint()
    private var cartesianPoint: CartesianPoint = CartesianPoint()

    constructor(polarPoint: PolarPoint = PolarPoint()) {
        this.polarPoint = polarPoint
        this.cartesianPoint = polarPoint.toCartesian()
    }

    constructor(cartesianPoint: CartesianPoint) {
        this.polarPoint = cartesianPoint.toPolar()
        this.cartesianPoint = cartesianPoint
    }

    operator fun div(d: Double): Point = Point(cartesianPoint / d)

    operator fun minus(other: Point): Point =
        Point(cartesianPoint - other.cartesianPoint)

    operator fun plus(other: Point): Point =
        Point(cartesianPoint + other.cartesianPoint)

    operator fun times(d: Double) = Point(cartesianPoint * d)

}