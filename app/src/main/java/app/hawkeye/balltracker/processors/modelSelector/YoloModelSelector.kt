package app.hawkeye.balltracker.processors.modelSelector

import app.hawkeye.balltracker.processors.yoloDataProcessors.DataProcessor

abstract class YoloModelSelector: ModelSelector {
    protected var availableSortedDataProcessors: List<DataProcessor> = listOf()

    override fun selectAppropriateModelToHandleScreenSquare(sideSize: Int): DataProcessor? {
        return availableSortedDataProcessors.firstOrNull { it.getModelInputSideSize() == sideSize }
    }

    override fun getAvailableModelSideSizes(): List<Int> {
        return availableSortedDataProcessors.map { it.getModelInputSideSize() }
    }
}
