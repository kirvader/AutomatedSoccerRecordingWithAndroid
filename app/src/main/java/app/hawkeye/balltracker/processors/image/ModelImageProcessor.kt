package app.hawkeye.balltracker.processors.image

import androidx.camera.core.ImageProxy
import app.hawkeye.balltracker.processors.utils.AdaptiveScreenRect
import app.hawkeye.balltracker.processors.utils.ClassifiedBox

interface ModelImageProcessor {
    suspend fun processImageProxy(imageProxy: ImageProxy) : ClassifiedBox?

    fun getAreaOfDetection() : List<AdaptiveScreenRect>

    object Default : ModelImageProcessor {
        override suspend fun processImageProxy(imageProxy: ImageProxy): ClassifiedBox? {
            return null
        }

        override fun getAreaOfDetection(): List<AdaptiveScreenRect> {
            return listOf()
        }
    }
}