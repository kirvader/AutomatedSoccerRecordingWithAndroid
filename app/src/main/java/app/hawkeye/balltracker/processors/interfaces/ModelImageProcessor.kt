package app.hawkeye.balltracker.processors.interfaces

import androidx.camera.core.ImageProxy
import app.hawkeye.balltracker.utils.AdaptiveRect
import app.hawkeye.balltracker.utils.ClassifiedBox

interface ModelImageProcessor {
    fun processImageProxy(imageProxy: ImageProxy) : ClassifiedBox?

    fun getAreaOfDetection() : List<AdaptiveRect>

    object Default : ModelImageProcessor {
        override fun processImageProxy(imageProxy: ImageProxy): ClassifiedBox? {
            return null
        }

        override fun getAreaOfDetection(): List<AdaptiveRect> {
            return listOf()
        }
    }
}