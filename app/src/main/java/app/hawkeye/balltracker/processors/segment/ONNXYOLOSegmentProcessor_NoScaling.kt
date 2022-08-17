package app.hawkeye.balltracker.processors.segment

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import android.content.Context
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

class ONNXYOLOSegmentProcessor_NoScaling(context: Context, modelId: Int, inputImageSize: Int) :
    ONNXSegmentProcessor(context, modelId, inputImageSize) {

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

        val calibratedLeftBorder = max(0, min(imageWidth - screenRect.width, leftBorder))
        val calibratedTopBorder = max(0, min(imageHeight - screenRect.height, topBorder))

        return Pair(calibratedLeftBorder, calibratedTopBorder)
    }

    override fun processImageSegment(
        imageProxy: ImageProxy,
        screenRect: ScreenRect
    ): ClassifiedBox? {

        val wholeImageWidth = if ((imageProxy.imageInfo.rotationDegrees / 90) % 2 == 0) imageProxy.width else imageProxy.height
        val wholeImageHeight = if ((imageProxy.imageInfo.rotationDegrees / 90) % 2 == 0) imageProxy.height else imageProxy.width
        val (left, top) = getTopLeftRectPoint(screenRect, wholeImageWidth, wholeImageHeight)

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

                            val balls = getAllObjectsByClassFromYOLO(arr, 0, CONFIDENCE_THRESHOLD, SCORE_THRESHOLD, inputImageSize, inputImageSize)

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