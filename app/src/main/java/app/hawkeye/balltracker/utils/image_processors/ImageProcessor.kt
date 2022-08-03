package app.hawkeye.balltracker.utils.image_processors

import androidx.camera.core.ImageProxy
import app.hawkeye.balltracker.utils.ClassifiedBox

interface ImageProcessor {
    fun processAndCloseImageProxy(imageProxy: ImageProxy) : List<ClassifiedBox>

    object Default : ImageProcessor {
        override fun processAndCloseImageProxy(imageProxy: ImageProxy): List<ClassifiedBox> {
            imageProxy.close()
            return listOf()
        }
    }
}