package app.hawkeye.balltracker.processors.segment

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy

import app.hawkeye.balltracker.processors.getAllObjectsByClassFromYOLO
import app.hawkeye.balltracker.processors.preProcess
import app.hawkeye.balltracker.processors.rotate
import app.hawkeye.balltracker.processors.toBitmap
import app.hawkeye.balltracker.processors.utils.ScreenRect
import app.hawkeye.balltracker.utils.ClassifiedBox
import app.hawkeye.balltracker.utils.createLogger
import java.util.*
import kotlin.math.max
import kotlin.math.min

private val LOG = createLogger<ONNXYOLOSegmentProcessor_NoScaling>()

class ONNXYOLOSegmentProcessor_Scalable(
    context: Context,
    modelId: Int,
    modelInputImageSideSize: Int
) :
    ONNXSegmentProcessor(context, modelId, modelInputImageSideSize) {

    private val CONFIDENCE_THRESHOLD: Float = 0.3F
    private val SCORE_THRESHOLD: Float = 0.2F

    // Get index of top 3 values
    // This is for demo purpose only, there are more efficient algorithms for topK problems
    private fun getTopDetectedObject(foundObjects: List<ClassifiedBox>): ClassifiedBox? {
        return foundObjects.maxByOrNull { it.confidence }
    }

    private fun getTopLeftRectPoint(
        screenRect: ScreenRect,
        imageWidth: Int,
        imageHeight: Int
    ): Pair<Int, Int> {
        val leftBorder = screenRect.center.x - screenRect.width / 2
        val topBorder = screenRect.center.y - screenRect.height / 2

        val calibratedLeftBorder = max(0, min(imageWidth - modelInputImageSideSize, leftBorder))
        val calibratedTopBorder = max(0, min(imageHeight - modelInputImageSideSize, topBorder))

        return Pair(calibratedLeftBorder, calibratedTopBorder)
    }

    private fun getScaleFactor(rectToProcessSideSize: Int): Float {
        return modelInputImageSideSize.toFloat() / rectToProcessSideSize
    }

    override fun processImageSegment(
        imageProxy: ImageProxy,
        screenRect: ScreenRect
    ): ClassifiedBox? {
        val widthScaleFactor = getScaleFactor(screenRect.width)
        val heightScaleFactor = getScaleFactor(screenRect.height)

        val wholeImageWidth: Int =
            (widthScaleFactor * if ((imageProxy.imageInfo.rotationDegrees / 90) % 2 == 0) imageProxy.width else imageProxy.height).toInt()
        val wholeImageHeight: Int =
            (heightScaleFactor * if ((imageProxy.imageInfo.rotationDegrees / 90) % 2 == 0) imageProxy.height else imageProxy.width).toInt()

        val (left, top) = getTopLeftRectPoint(screenRect.scaleBy(Pair(widthScaleFactor, heightScaleFactor)), wholeImageWidth, wholeImageHeight)

        val imgBitmap = imageProxy.toBitmap()
        val rawBitmap =
            imgBitmap?.let { Bitmap.createScaledBitmap(it, wholeImageWidth, wholeImageHeight, false) }
        val bitmap = rawBitmap?.rotate(imageProxy.imageInfo.rotationDegrees.toFloat())

        if (bitmap != null) {
            val imgData =
                preProcess(bitmap, modelInputImageSideSize, modelInputImageSideSize, left, top)

            val inputName = ortSession.inputNames?.iterator()?.next()
            val shape = longArrayOf(
                1,
                3,
                modelInputImageSideSize.toLong(),
                modelInputImageSideSize.toLong()
            )
            val env = OrtEnvironment.getEnvironment()
            env.use {
                val tensor = OnnxTensor.createTensor(env, imgData, shape)
                tensor.use {
                    val output = ortSession.run(Collections.singletonMap(inputName, tensor))
                    if (output?.get(0)?.value != null) {
                        output.use {
                            val arr = ((output.get(0)?.value) as Array<Array<FloatArray>>)[0]

                            val balls = getAllObjectsByClassFromYOLO(
                                arr,
                                0,
                                CONFIDENCE_THRESHOLD,
                                SCORE_THRESHOLD,
                                modelInputImageSideSize,
                                modelInputImageSideSize
                            )

                            val relativeResult = getTopDetectedObject(balls) ?: return null

                            return getAbsoluteClassifiedBoxFromRelative(
                                relativeResult,
                                screenRect,
                                wholeImageWidth,
                                wholeImageHeight
                            )
                        }
                    }
                }
            }
        }
        return null

    }

}