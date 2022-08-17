package app.hawkeye.balltracker.processors.interfaces

import androidx.camera.core.ImageProxy
import app.hawkeye.balltracker.processors.utils.ScreenRect
import app.hawkeye.balltracker.utils.ClassifiedBox

interface SegmentProcessor {
    fun processImageSegment(imageProxy: ImageProxy, screenRect: ScreenRect) : ClassifiedBox?
}