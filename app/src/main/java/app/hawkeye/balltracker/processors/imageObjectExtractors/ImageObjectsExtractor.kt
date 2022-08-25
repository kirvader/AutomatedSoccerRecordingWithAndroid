package app.hawkeye.balltracker.processors.imageObjectExtractors

import android.graphics.Bitmap
import app.hawkeye.balltracker.configs.choices.ModelSelectors
import app.hawkeye.balltracker.processors.utils.ClassifiedBox
import app.hawkeye.balltracker.processors.utils.ScreenRect

interface ImageObjectsExtractor {
    suspend fun extractObjects(bitmap: Bitmap, areaOfDetection: List<ScreenRect>, soughtClassIds: Int): ClassifiedBox?

    fun getCurrentAvailableModelSideSizes(): List<Int>

    fun setModelSelector(modelSelectorChoice: ModelSelectors)
}