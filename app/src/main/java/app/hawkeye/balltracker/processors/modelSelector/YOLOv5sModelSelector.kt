package app.hawkeye.balltracker.processors.modelSelector

import android.content.Context
import app.hawkeye.balltracker.R

class YOLOv5sModelSelector(context: Context): YoloModelSelector(context) {
    override val model64: Int
        get() = R.raw.yolov5s_64
    override val model128: Int
        get() = R.raw.yolov5s_128
    override val model256: Int
        get() = R.raw.yolov5s_256
    override val model512: Int
        get() = R.raw.yolov5s_512
    override val model640: Int
        get() = R.raw.yolov5s_640
}