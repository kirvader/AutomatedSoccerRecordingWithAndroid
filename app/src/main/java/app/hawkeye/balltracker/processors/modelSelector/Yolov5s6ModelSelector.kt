package app.hawkeye.balltracker.processors.modelSelector

import app.hawkeye.balltracker.configs.objects.AvailableYoloModels
import app.hawkeye.balltracker.processors.yoloDataProcessors.YoloDataProcessor

class Yolov5s6ModelSelector: YoloModelSelector() {
    init {
        availableSortedDataProcessors = listOf(
            YoloDataProcessor(AvailableYoloModels.yoloV5s6_64!!, 64),
            YoloDataProcessor(AvailableYoloModels.yoloV5s6_128!!, 128),
            YoloDataProcessor(AvailableYoloModels.yoloV5s6_256!!, 256),
            YoloDataProcessor(AvailableYoloModels.yoloV5s6_512!!, 512),
            YoloDataProcessor(AvailableYoloModels.yoloV5s6_640!!, 640),
        )
    }
}