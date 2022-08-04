package app.hawkeye.balltracker.processors

import androidx.camera.core.ImageProxy
import app.hawkeye.balltracker.utils.ClassifiedBox

interface ImageProcessor {
    fun processImageProxy(imageProxy: ImageProxy) : List<ClassifiedBox>

    object Default : ImageProcessor {
        override fun processImageProxy(imageProxy: ImageProxy): List<ClassifiedBox> {
            return listOf()
        }
    }
}