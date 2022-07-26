package com.example.movementcontrollingmodule.movement.utils

import kotlin.math.*
import kotlin.system.exitProcess

fun convertGradToRadian(phi: Float): Float = phi / 180.0f * PI.toFloat()
fun convertRadianToGrad(phi: Float) = phi / PI.toFloat() * 180.0f

class PolarPoint(
    val d: Float = 0.0f,
    val phi: Float = 0.0f,
    val h: Float = 0.0f
) {
    fun getXProjection() = d * cos(phi)
    fun getYProjection() = d * sin(phi)

    fun toCartesian(): CartesianPoint = CartesianPoint(getXProjection(), getYProjection(), h)

}

class CartesianPoint(
    val x: Float = 0.0f,
    val y: Float = 0.0f,
    val h: Float = 0.0f
) {
    fun getLength() = sqrt(x * x + y * y)

    fun toPolar(): PolarPoint = PolarPoint(getLength(), getAngle(), h)

    private fun getAngle(): Float = atan2(y, x)

    operator fun div(d: Float): CartesianPoint {
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

    operator fun times(d: Float): CartesianPoint =
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

    fun getAngle() = polarPoint.phi
    fun getDistance() = polarPoint.d
    fun getHeight() = polarPoint.h

    operator fun div(d: Float): Point = Point(cartesianPoint / d)

    operator fun minus(other: Point): Point =
        Point(cartesianPoint - other.cartesianPoint)

    operator fun plus(other: Point): Point =
        Point(cartesianPoint + other.cartesianPoint)

    operator fun times(d: Float) = Point(cartesianPoint * d)

}