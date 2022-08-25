package app.hawkeye.balltracker.processors.imageObjectExtractors

import android.graphics.Bitmap
import app.hawkeye.balltracker.configs.choices.ModelSelectors
import app.hawkeye.balltracker.configs.objects.TrackingSystemConfigObject
import app.hawkeye.balltracker.processors.modelSelector.ModelSelector
import app.hawkeye.balltracker.processors.modelSelector.Yolov5sModelSelector
import app.hawkeye.balltracker.processors.utils.AdaptiveScreenRect
import app.hawkeye.balltracker.processors.utils.ClassifiedBox
import app.hawkeye.balltracker.processors.utils.ScreenRect
import app.hawkeye.balltracker.processors.utils.ScreenVector
import app.hawkeye.balltracker.utils.createLogger
import kotlinx.coroutines.*
import java.nio.FloatBuffer

private val LOG = createLogger<YOLOObjectExtractor>()

class YOLOObjectExtractor : ImageObjectsExtractor {

    private var modelSelector: ModelSelector = Yolov5sModelSelector()

    private val scope = CoroutineScope(Dispatchers.Default)

    private val DIM_BATCH_SIZE = 1
    private val DIM_PIXEL_SIZE = 3

    private fun preProcess(bitmap: Bitmap, sideSize: Int, offset: ScreenVector): FloatBuffer {
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
                imgData.put(resultImageIndex, (pixelValue shr 16 and 0xFF) / 255.0F)
                imgData.put(
                    resultImageIndex + resultImageStride,
                    (pixelValue shr 8 and 0xFF) / 255.0F
                )
                imgData.put(
                    resultImageIndex + resultImageStride * 2,
                    (pixelValue and 0xFF) / 255.0F
                )
            }
        }

        imgData.rewind()
        return imgData
    }

    private fun relativeToAbsolute(
        relative: ClassifiedBox?,
        absScreenRect: ScreenRect,
        imageWidth: Int,
        imageHeight: Int
    ): ClassifiedBox? {
        if (relative == null)
            return null
        return ClassifiedBox(
            AdaptiveScreenRect(
                absScreenRect.toAdaptiveScreenRect(
                    imageWidth,
                    imageHeight
                ).topLeftPoint + relative.adaptiveRect.topLeftPoint.getScaled(
                    absScreenRect.size.x.toFloat() / imageWidth,
                    absScreenRect.size.y.toFloat() / imageHeight
                ),
                relative.adaptiveRect.size.getScaled(
                    absScreenRect.size.x.toFloat() / imageWidth,
                    absScreenRect.size.y.toFloat() / imageHeight
                )
            ),
            relative.classId, relative.confidence
        )
    }

    override suspend fun extractObjects(
        bitmap: Bitmap,
        areaOfDetection: List<ScreenRect>,
        soughtClassIds: Int
    ): ClassifiedBox? {
        val results: MutableList<Deferred<ClassifiedBox?>> = mutableListOf()
        areaOfDetection.filter { it.isSquare() }.map {
            val chosenModel =
                TrackingSystemConfigObject.yoloModelSelectorChoice.modelSelector.selectAppropriateModelToHandleScreenSquare(it.size.x)
            if (chosenModel != null) {
                results.add(
                    scope.async {
                        val imgData = preProcess(bitmap, it.size.x, it.topLeftPoint)

                        val relativeResult = chosenModel.process(imgData, soughtClassIds)

                        relativeToAbsolute(relativeResult, it, bitmap.width, bitmap.height)
                    }
                )
            }
        }
        return results.mapNotNull { it.await() }.maxByOrNull { it.confidence }
    }

    override fun getCurrentAvailableModelSideSizes(): List<Int> {
        return modelSelector.getAvailableModelSideSizes()
    }

    override fun setModelSelector(modelSelectorChoice: ModelSelectors) {
        modelSelector = modelSelectorChoice.modelSelector
    }

}