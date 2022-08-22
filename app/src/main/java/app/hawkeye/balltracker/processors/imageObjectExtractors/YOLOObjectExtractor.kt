package app.hawkeye.balltracker.processors.imageObjectExtractors

import android.content.Context
import android.graphics.Bitmap
import app.hawkeye.balltracker.processors.modelSelector.ModelSelector
import app.hawkeye.balltracker.processors.modelSelector.YOLOv5sModelSelector
import app.hawkeye.balltracker.processors.utils.AdaptiveScreenRect
import app.hawkeye.balltracker.processors.utils.ClassifiedBox
import app.hawkeye.balltracker.processors.utils.ScreenRect
import app.hawkeye.balltracker.processors.utils.ScreenVector
import app.hawkeye.balltracker.utils.createLogger
import java.nio.FloatBuffer
import kotlin.math.abs

private val LOG = createLogger<YOLOObjectExtractor>()

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
        LOG.i("sideSize = $sideSize, offset = (${offset.x}; ${offset.y})")

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

    private fun relativeToAbsolute(relative: ClassifiedBox?, absScreenRect: ScreenRect, imageWidth: Int, imageHeight: Int): ClassifiedBox? {
        if (relative == null)
            return null
        return ClassifiedBox(
            AdaptiveScreenRect(
                absScreenRect.toAdaptiveScreenRect(imageWidth, imageHeight).topLeftPoint + relative.adaptiveRect.topLeftPoint.getScaled(absScreenRect.size.x.toFloat() / imageWidth, absScreenRect.size.y.toFloat() / imageHeight),
                relative.adaptiveRect.size.getScaled(absScreenRect.size.x.toFloat() / imageWidth, absScreenRect.size.y.toFloat() / imageHeight)
            ),
            relative.classId, relative.confidence
        )
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
                    val curRectResult = relativeToAbsolute(chosenModel.process(imgData, soughtClassIds), it, bitmap.width, bitmap.height)

                    if ((result == null) || (curRectResult != null && result!!.confidence < curRectResult.confidence)) {
                        result = curRectResult
                    }
                }
            }
        }
        return result
    }

}