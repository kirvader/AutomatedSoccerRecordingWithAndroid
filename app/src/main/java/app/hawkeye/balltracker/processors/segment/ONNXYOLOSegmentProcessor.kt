package app.hawkeye.balltracker.processors.segment

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import android.content.Context
import androidx.camera.core.ImageProxy

import app.hawkeye.balltracker.processors.getAllObjectsByClassFromYOLO
import app.hawkeye.balltracker.processors.preProcess
import app.hawkeye.balltracker.processors.rotate
import app.hawkeye.balltracker.processors.toBitmap
import app.hawkeye.balltracker.utils.ClassifiedBox
import app.hawkeye.balltracker.utils.ScreenPoint
import app.hawkeye.balltracker.utils.createLogger
import java.util.*
import kotlin.math.max
import kotlin.math.min

private val LOG = createLogger<ONNXYOLOSegmentProcessor>()

class ONNXYOLOSegmentProcessor(context: Context, modelId: Int, inputImageSize: Int) :
    ONNXSegmentProcessor(context, modelId, inputImageSize) {

    private val CONFIDENCE_THRESHOLD: Float = 0.3F
    private val SCORE_THRESHOLD: Float = 0.2F

    // Get index of top 3 values
    // This is for demo purpose only, there are more efficient algorithms for topK problems
    private fun getTopDetectedObject(foundObjects: List<ClassifiedBox>): ClassifiedBox? {
        return foundObjects.maxByOrNull { it.confidence }
    }

    private fun getTopLeftRectPoint(
        screenPoint: ScreenPoint,
        imageWidth: Int,
        imageHeight: Int
    ): Pair<Int, Int> {
        val leftBorder = (screenPoint.x * imageWidth).toInt() - inputImageSize / 2
        val topBorder = (screenPoint.y * imageHeight).toInt() - inputImageSize / 2

        val calibratedLeftBorder = max(0, min(imageWidth - inputImageSize, leftBorder))
        val calibratedTopBorder = max(0, min(imageHeight - inputImageSize, topBorder))

        return Pair(calibratedLeftBorder, calibratedTopBorder)
    }

    override fun processImageSegment(
        imageProxy: ImageProxy,
        screenPoint: ScreenPoint
    ): ClassifiedBox? {

        val wholeImageWidth = if ((imageProxy.imageInfo.rotationDegrees / 90) % 2 == 0) imageProxy.width else imageProxy.height
        val wholeImageHeight = if ((imageProxy.imageInfo.rotationDegrees / 90) % 2 == 0) imageProxy.height else imageProxy.width
        val (left, top) = getTopLeftRectPoint(screenPoint, wholeImageWidth, wholeImageHeight)

        val imgBitmap = imageProxy.toBitmap()
        val bitmap = imgBitmap?.rotate(imageProxy.imageInfo.rotationDegrees.toFloat())

        if (bitmap != null) {
            val imgData = preProcess(bitmap, inputImageSize, inputImageSize, left, top)

            val inputName = ortSession.inputNames?.iterator()?.next()
            val shape = longArrayOf(1, 3, inputImageSize.toLong(), inputImageSize.toLong())
            val env = OrtEnvironment.getEnvironment()
            env.use {
                val tensor = OnnxTensor.createTensor(env, imgData, shape)
                tensor.use {
                    val output = ortSession.run(Collections.singletonMap(inputName, tensor))
                    if (output?.get(0)?.value != null) {
                        output.use {
                            val arr = ((output.get(0)?.value) as Array<Array<FloatArray>>)[0]

                            val balls = getAllObjectsByClassFromYOLO(arr, -1, CONFIDENCE_THRESHOLD, SCORE_THRESHOLD, inputImageSize, inputImageSize)

                            val relativeResult = getTopDetectedObject(balls) ?: return null

                            return getAbsoluteClassifiedBoxFromRelative(
                                relativeResult,
                                screenPoint,
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