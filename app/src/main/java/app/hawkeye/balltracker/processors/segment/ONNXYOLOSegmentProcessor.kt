package app.hawkeye.balltracker.processors.segment

import android.content.Context
import android.graphics.Rect
import androidx.camera.core.ImageProxy
import app.hawkeye.balltracker.processors.image.toBitmap
import app.hawkeye.balltracker.utils.AdaptiveRect
import app.hawkeye.balltracker.utils.ClassifiedBox
import app.hawkeye.balltracker.utils.ScreenPoint
import app.hawkeye.balltracker.utils.createLogger

private val LOG = createLogger<ONNXYOLOSegmentProcessor>()

class ONNXYOLOSegmentProcessor(context: Context, modelId: Int, inputImageSize: Int) :
    ONNXSegmentProcessor(context, modelId, inputImageSize) {
    override fun processImageSegment(
        imageProxy: ImageProxy,
        screenPoint: ScreenPoint
    ): ClassifiedBox? {
        val realImageWidth = imageProxy.width
        val realImageHeight = imageProxy.height

        val rectSize = inputImageSize

        val leftBorder = (screenPoint.x * realImageWidth).toInt() - inputImageSize / 2
        val rightBorder = leftBorder + inputImageSize

        val topBorder = (screenPoint.y * realImageHeight).toInt() - inputImageSize / 2
        val bottomBorder = topBorder + inputImageSize

        val rect = Rect(leftBorder, topBorder, rightBorder, bottomBorder)

        val imgBitmap = imageProxy.toBitmap()

        if (imgBitmap != null) {
            LOG.i("width = ${imgBitmap.width}, height = ${imgBitmap.height}")
        }
//        val rawBitmap =
//            imgBitmap?.let { Bitmap.createScaledBitmap(it, IMAGE_WIDTH, IMAGE_HEIGHT, false) }
//        val bitmap = rawBitmap?.rotate(imageProxy.imageInfo.rotationDegrees.toFloat())
//
//        if (bitmap != null) {
//
//            val imgData = preProcess(bitmap, IMAGE_WIDTH, IMAGE_HEIGHT)
//            val inputName = ortSession.inputNames?.iterator()?.next()
//            val shape = longArrayOf(1, 3, IMAGE_HEIGHT.toLong(), IMAGE_WIDTH.toLong())
//            val env = OrtEnvironment.getEnvironment()
//            env.use {
//                val tensor = OnnxTensor.createTensor(env, imgData, shape)
//                tensor.use {
//                    val output = ortSession.run(Collections.singletonMap(inputName, tensor))
//                    if (output?.get(0)?.value != null) {
//                        output.use {
//                            val arr = ((output.get(0)?.value) as Array<Array<FloatArray>>)[0]
//
//                            val balls = getAllObjectsByClassFromYOLO(
//                                arr,
//                                -1,
//                                CONFIDENCE_THRESHOLD,
//                                SCORE_THRESHOLD,
//                                IMAGE_WIDTH,
//                                IMAGE_HEIGHT
//                            )
//
//                            return getTopDetectedObject(balls)
//                        }
//                    }
//                }
//            }
//        }
//        return null

        return null
    }

}