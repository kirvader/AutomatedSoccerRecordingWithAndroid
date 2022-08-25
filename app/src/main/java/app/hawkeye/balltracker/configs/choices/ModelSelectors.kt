package app.hawkeye.balltracker.configs.choices

import app.hawkeye.balltracker.processors.modelSelector.ModelSelector
import app.hawkeye.balltracker.processors.modelSelector.Yolov5sModelSelector
import app.hawkeye.balltracker.processors.modelSelector.Yolov5n6ModelSelector
import app.hawkeye.balltracker.processors.modelSelector.Yolov5s6ModelSelector

enum class ModelSelectors(val modelSelector: ModelSelector) {
    YoloV5s(Yolov5sModelSelector()),
    YoloV5s6(Yolov5s6ModelSelector()),
    YoloV5n6(Yolov5n6ModelSelector())
}