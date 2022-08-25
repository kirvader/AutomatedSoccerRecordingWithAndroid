package app.hawkeye.balltracker.configs.choices

import app.hawkeye.balltracker.processors.imageObjectExtractors.ImageObjectsExtractor
import app.hawkeye.balltracker.processors.imageObjectExtractors.YOLOObjectExtractor

enum class ObjectExtractors(val objectsExtractor: ImageObjectsExtractor) {
    YOLO(YOLOObjectExtractor())
}