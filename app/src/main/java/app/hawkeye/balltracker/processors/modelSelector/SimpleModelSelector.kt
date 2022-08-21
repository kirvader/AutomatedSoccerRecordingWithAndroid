package app.hawkeye.balltracker.processors.modelSelector

import android.content.Context
import app.hawkeye.balltracker.processors.yoloDataProcessors.DataProcessor
import app.hawkeye.balltracker.processors.yoloDataProcessors.YoloDataProcessor

abstract class SimpleModelSelector(context: Context): ModelSelector {
    private val availableSortedDataProcessors: List<DataProcessor>

    abstract val model64: Int
    abstract val model128: Int
    abstract val model256: Int
    abstract val model512: Int
    abstract val model640: Int

    init {
        availableSortedDataProcessors = listOf(
            YoloDataProcessor(context, model64, 64),
            YoloDataProcessor(context, model128, 128),
            YoloDataProcessor(context, model256, 256),
            YoloDataProcessor(context, model512, 512),
            YoloDataProcessor(context, model640, 640),
        )
    }
    override fun selectAppropriateModelToHandleScreenSquare(sideSize: Int): DataProcessor? {
        return availableSortedDataProcessors.firstOrNull { it.getModelInputSideSize() == sideSize }
    }
}
