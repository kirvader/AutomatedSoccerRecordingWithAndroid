package app.hawkeye.balltracker.processors.yoloDataProcessors

import app.hawkeye.balltracker.processors.utils.ClassifiedBox
import java.nio.FloatBuffer

interface DataProcessor {
    fun process(data: FloatBuffer, soughtClassIds: Int): ClassifiedBox?

    /**
     * @return side size of an input needed by the model
     */
    fun getModelInputSideSize(): Int
}