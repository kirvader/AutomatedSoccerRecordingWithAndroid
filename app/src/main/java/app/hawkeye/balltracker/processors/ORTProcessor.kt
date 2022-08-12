// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package app.hawkeye.balltracker.processors

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import app.hawkeye.balltracker.R
import app.hawkeye.balltracker.processors.interfaces.ModelImageProcessor
import app.hawkeye.balltracker.utils.ClassifiedBox
import java.util.*


internal class ORTModelImageProcessor(
    context: Context
) : ModelImageProcessor {
    private var ortSession: OrtSession? = null

    private fun readYoloModel(context: Context): ByteArray {
        val modelID = R.raw.yolov5s
        return context.resources.openRawResource(modelID).readBytes()
    }

    private fun createOrtSessionForYOLO(context: Context): OrtSession? =
        OrtEnvironment.getEnvironment()?.createSession(readYoloModel(context))

    init {
        ortSession = createOrtSessionForYOLO(context)
    }
    // Get index of top 3 values
    // This is for demo purpose only, there are more efficient algorithms for topK problems
    private fun getTop3(foundObjects: List<ClassifiedBox>): List<ClassifiedBox> {
        return foundObjects.sortedByDescending { it.confidence }.take(3)
    }

    private val CONFIDENCE_THRESHOLD: Float = 0.3F
    private val SCORE_THRESHOLD: Float = 0.2F
    private val IMAGE_WIDTH: Int = 640
    private val IMAGE_HEIGHT: Int = 640


    override fun processImageProxy(imageProxy: ImageProxy): List<ClassifiedBox> {

        val imgBitmap = imageProxy.toBitmap()
        val rawBitmap = imgBitmap?.let { Bitmap.createScaledBitmap(it, IMAGE_WIDTH, IMAGE_HEIGHT, false) }
        val bitmap = rawBitmap?.rotate(imageProxy.imageInfo.rotationDegrees.toFloat())

        if (bitmap != null) {

            val imgData = preProcess(bitmap, IMAGE_WIDTH, IMAGE_HEIGHT)
            val inputName = ortSession?.inputNames?.iterator()?.next()
            val shape = longArrayOf(1, 3, IMAGE_HEIGHT.toLong(), IMAGE_WIDTH.toLong())
            val env = OrtEnvironment.getEnvironment()
            env.use {
                val tensor = OnnxTensor.createTensor(env, imgData, shape)
                tensor.use {
                    val output = ortSession?.run(Collections.singletonMap(inputName, tensor))
                    if (output?.get(0)?.value != null) {
                        output.use {
                            val arr = ((output.get(0)?.value) as Array<Array<FloatArray>>)[0]

                            val balls = getAllObjectsByClassFromYOLO(arr, -1, CONFIDENCE_THRESHOLD, SCORE_THRESHOLD, IMAGE_WIDTH, IMAGE_HEIGHT)

                            return getTop3(balls)
                        }
                    }
                }
            }
        }
        return listOf()
    }
}