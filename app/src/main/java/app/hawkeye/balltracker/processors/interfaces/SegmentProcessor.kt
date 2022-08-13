package app.hawkeye.balltracker.processors.interfaces

import androidx.camera.core.ImageProxy
import app.hawkeye.balltracker.utils.ClassifiedBox
import app.hawkeye.balltracker.utils.ScreenPoint

interface SegmentProcessor {
    fun processImageSegment(imageProxy: ImageProxy, rect: ScreenPoint) : ClassifiedBox?
}