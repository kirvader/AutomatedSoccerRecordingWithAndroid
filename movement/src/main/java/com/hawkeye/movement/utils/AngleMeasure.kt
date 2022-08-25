package com.hawkeye.movement.utils

import kotlin.math.PI


fun cos(angle: AngleMeasure): Float = kotlin.math.cos(angle.radian())
fun sin(angle: AngleMeasure): Float = kotlin.math.sin(angle.radian())

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
