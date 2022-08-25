package app.hawkeye.balltracker.processors.modelSelector

import app.hawkeye.balltracker.processors.yoloDataProcessors.DataProcessor

interface ModelSelector {
    fun selectAppropriateModelToHandleScreenSquare(sideSize: Int): DataProcessor?

    fun getAvailableModelSideSizes(): List<Int>
}