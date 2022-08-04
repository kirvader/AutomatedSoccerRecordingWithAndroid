package app.hawkeye.balltracker.processors

import androidx.camera.core.ImageProxy
import app.hawkeye.balltracker.utils.ClassifiedBox

interface ImageProcessor {
    fun processAndCloseImageProxy(imageProxy: ImageProxy) : List<ClassifiedBox>

    object Default : ImageProcessor {
        override fun processAndCloseImageProxy(imageProxy: ImageProxy): List<ClassifiedBox> {
            return listOf()
        }
    }
}