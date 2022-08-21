package app.hawkeye.balltracker.processors.imageObjectExtractors

import android.content.Context
import android.graphics.Bitmap
import app.hawkeye.balltracker.processors.modelSelector.ModelSelector
import app.hawkeye.balltracker.processors.modelSelector.YOLOv5sModelSelector
import app.hawkeye.balltracker.processors.utils.ClassifiedBox
import app.hawkeye.balltracker.processors.utils.ScreenRect
import app.hawkeye.balltracker.processors.utils.ScreenVector
import java.nio.FloatBuffer


class YOLOObjectExtractor(context: Context) : ImageObjectsExtractor {

    private var modelSelector: ModelSelector

    init {
        modelSelector = YOLOv5sModelSelector(context)
    }
    private val DIM_BATCH_SIZE = 1
    private val DIM_PIXEL_SIZE = 3

    fun preProcess(bitmap: Bitmap, sideSize: Int, offset: ScreenVector): FloatBuffer {
        val imgData = FloatBuffer.allocate(
            DIM_BATCH_SIZE
                    * DIM_PIXEL_SIZE
                    * sideSize
                    * sideSize
        )
        imgData.rewind()
        val wholeImageStride = bitmap.width * bitmap.height
        val bmpData = IntArray(wholeImageStride)
        bitmap.getPixels(bmpData, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val resultImageStride = sideSize * sideSize
        for (i in 0 until sideSize) {
            for (j in 0 until sideSize) {
                val wholeImageIndex = bitmap.width * (i + offset.y) + (j + offset.x)
                val resultImageIndex = i * sideSize + j
                val pixelValue = bmpData[wholeImageIndex]
                imgData.put(resultImageIndex,                            (pixelValue shr 16 and 0xFF) / 255.0F)
                imgData.put(resultImageIndex + resultImageStride,        (pixelValue shr 8 and 0xFF) / 255.0F)
                imgData.put(resultImageIndex + resultImageStride * 2,    (pixelValue and 0xFF) / 255.0F)
            }
        }

        imgData.rewind()
        return imgData
    }

    override fun extractObjects(
        bitmap: Bitmap,
        areaOfDetection: List<ScreenRect>,
        soughtClassIds: Int
    ): ClassifiedBox? {
        var result: ClassifiedBox? = null
        areaOfDetection.map {
            if (it.isSquare()) {
                val chosenModel = modelSelector.selectAppropriateModelToHandleScreenSquare(it.size.x)
                if (chosenModel != null) {
                    val imgData = preProcess(bitmap, it.size.x, it.topLeftPoint)

                    // TODO add multithreading
                    val curRectResult = chosenModel.process(imgData, 0)
                    if (result == null) {
                        result = curRectResult
                    } else if (curRectResult != null && result!!.confidence < curRectResult.confidence) {
                        result = curRectResult
                    }
                }
            }
        }
        return result
    }

}