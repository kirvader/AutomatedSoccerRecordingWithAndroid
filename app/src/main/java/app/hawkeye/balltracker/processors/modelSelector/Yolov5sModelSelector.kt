package app.hawkeye.balltracker.processors.modelSelector

import app.hawkeye.balltracker.configs.objects.AvailableYoloModels
import app.hawkeye.balltracker.processors.yoloDataProcessors.YoloDataProcessor

class Yolov5sModelSelector: YoloModelSelector() {
    init {
        availableSortedDataProcessors = listOf(
            YoloDataProcessor(AvailableYoloModels.yoloV5s_64!!, 64),
            YoloDataProcessor(AvailableYoloModels.yoloV5s_128!!, 128),
            YoloDataProcessor(AvailableYoloModels.yoloV5s_256!!, 256),
            YoloDataProcessor(AvailableYoloModels.yoloV5s_512!!, 512),
            YoloDataProcessor(AvailableYoloModels.yoloV5s_640!!, 640),
        )
    }
}