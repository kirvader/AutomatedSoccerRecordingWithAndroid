package com.hawkeye.movement.utils

import kotlin.math.PI


fun cos(angle: AngleMeasure): Float = kotlin.math.cos(angle.radian())
fun sin(angle: AngleMeasure): Float = kotlin.math.sin(angle.radian())

fun abs(angle: AngleMeasure): AngleMeasure = Degree(kotlin.math.abs(angle.degree()))
fun sign(angle: AngleMeasure): Float {
    return if (angle > Degree(0f)) {
        1f
    } else if (angle < Degree(0f)) {
        -1f
    } else {
        0f
    }
}

fun min(a: AngleMeasure, b: AngleMeasure): AngleMeasure =
    Degree(kotlin.math.min(a.degree(), b.degree()))

fun max(a: AngleMeasure, b: AngleMeasure): AngleMeasure =
    Degree(kotlin.math.max(a.degree(), b.degree()))

interface AngleMeasure : Comparable<AngleMeasure> {
    fun degree(): Float {
        return this.radian() * 180.0f / PI.toFloat()
    }
    fun radian(): Float {
        return this.degree() * PI.toFloat() / 180.0f
    }

    override operator fun compareTo(angle: AngleMeasure): Int {
        val deltaAngle = this.degree() - angle.degree()
        if (deltaAngle < -eps) {
            return -1
        }
        if (deltaAngle > eps) {
            return 1
        }
        return 0
    }

    operator fun plus(angle: AngleMeasure): AngleMeasure {
        return Degree(this.degree() + angle.degree())
    }

    operator fun minus(angle: AngleMeasure): AngleMeasure {
        return Degree(this.degree() - angle.degree())
    }

    operator fun times(factor: Float): AngleMeasure {
        return Degree(this.degree() * factor)
    }

    operator fun div(factor: Float): AngleMeasure {
        return Degree(this.degree() / factor)
    }
    companion object {
        private const val eps = 0.00001f
    }
}

class Radian(private val angle: Float) : AngleMeasure {
    override fun degree(): Float = angle * 180.0f / PI.toFloat()

    override fun radian(): Float = angle
}

class Degree(private val angle: Float) : AngleMeasure {
    override fun degree(): Float = angle

    override fun radian(): Float = angle * PI.toFloat() / 180.0f
}
