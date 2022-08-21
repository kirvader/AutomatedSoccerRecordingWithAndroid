package app.hawkeye.balltracker.processors.imageObjectExtractors

import android.graphics.Bitmap
import app.hawkeye.balltracker.processors.utils.ClassifiedBox
import app.hawkeye.balltracker.processors.utils.ScreenRect

interface ImageObjectsExtractor {
    fun extractObjects(bitmap: Bitmap, areaOfDetection: List<ScreenRect>, soughtClassIds: Int): ClassifiedBox?
}