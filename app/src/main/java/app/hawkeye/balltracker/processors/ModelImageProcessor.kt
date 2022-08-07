package app.hawkeye.balltracker.processors

import androidx.camera.core.ImageProxy
import app.hawkeye.balltracker.utils.ClassifiedBox

interface ModelImageProcessor {
    fun processImageProxy(imageProxy: ImageProxy) : List<ClassifiedBox>

    object Default : ModelImageProcessor {
        override fun processImageProxy(imageProxy: ImageProxy): List<ClassifiedBox> {
            return listOf()
        }
    }
}