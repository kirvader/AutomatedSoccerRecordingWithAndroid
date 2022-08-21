package app.hawkeye.balltracker.processors.tracking_strategies

import app.hawkeye.balltracker.processors.utils.AdaptiveScreenRect
import app.hawkeye.balltracker.processors.utils.AdaptiveScreenVector
import kotlin.math.exp
import kotlin.math.log
import kotlin.math.max
import kotlin.math.min

abstract class ExpGrowingArea: TrackingStrategy {
    private var lastDetectedObjectCenter: AdaptiveScreenVector = AdaptiveScreenVector(0.5f, 0.5f)
    private var startSize: Float = 0.1f

    private var lastTimeOfUpdate_ms: Long = 0L

    private var adaptiveRectGrowthFactor: Float = -log(0.1f, exp(1.0f))

    private fun getSizeAtTime(baseSize: Float, absTime_ms: Long): Float {
        val deltaTime = (absTime_ms - lastTimeOfUpdate_ms).toFloat() / msInS

        return min(1.0f, baseSize * exp(adaptiveRectGrowthFactor * min(maxTimeOfRelevance, deltaTime)))
    }

    private fun updateGrowthFactor(newStartSize: Float) {
        adaptiveRectGrowthFactor = -log(newStartSize, exp(1.0f)) / maxTimeOfRelevance
    }

    override fun getAreaOfDetectionAtTime(absTime_ms: Long): List<AdaptiveScreenRect> {
        val currentAreaOfDetectionSize = getSizeAtTime(startSize, absTime_ms)

        val topLeftPointOfDetectionArea = (guessDetectionAreaCenterIfChanging(absTime_ms) ?: lastDetectedObjectCenter) - AdaptiveScreenVector(currentAreaOfDetectionSize, currentAreaOfDetectionSize) * 0.5f
        val movedArea = AdaptiveScreenRect(topLeftPointOfDetectionArea, AdaptiveScreenVector(currentAreaOfDetectionSize, currentAreaOfDetectionSize)).getCropped()

        return listOf(movedArea)
    }

    override fun updateLastDetectedObjectPositionAtTime(adaptiveRect: AdaptiveScreenRect?, absTime_ms: Long) {
        if (adaptiveRect == null) {
            return
        }

        lastDetectedObjectCenter = adaptiveRect.getCenter()
        startSize = max(adaptiveRect.size.x, adaptiveRect.size.y)

        lastTimeOfUpdate_ms = absTime_ms

        updateGrowthFactor(startSize)
    }

    open fun guessDetectionAreaCenterIfChanging(absTime_ms: Long): AdaptiveScreenVector? {
        return null
    }

    companion object {
        private const val msInS = 1000f
        private const val maxTimeOfRelevance = 1.0f
    }
}