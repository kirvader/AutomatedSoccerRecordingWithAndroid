// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package app.hawkeye.balltracker.utils.image_processors

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import app.hawkeye.balltracker.utils.ClassifiedBox
import app.hawkeye.balltracker.utils.ScreenPoint
import java.util.*


internal class ORTImageProcessor(
        private val ortSession: OrtSession?
) : ImageProcessor {

    // Get index of top 3 values
    // This is for demo purpose only, there are more efficient algorithms for topK problems
    private fun getTop3(foundObjects: List<ClassifiedBox>): List<ClassifiedBox> {
        return foundObjects.sortedByDescending { it.confidence }.take(3)
    }

    private val CONFIDENCE_THRESHOLD: Float = 0.3F
    private val SCORE_THRESHOLD: Float = 0.2F
    private val IMAGE_WIDTH: Float = 640.0F
    private val IMAGE_HEIGHT: Float = 640.0F



    private fun getIndOfMaxValue(classesScore: List<Float>): Int {
        var ind = 0
        for (i in 1 until classesScore.size) {
            if (classesScore[i] > classesScore[ind]) {
                ind = i
            }
        }
        return ind
    }

    // That function parses the yolov5 model output.
    // I assumed the format from this project https://github.com/doleron/yolov5-opencv-cpp-python/blob/main/cpp/yolo.cpp#L59
    // It says that each row is a bunch of encoded elements:
    /*
    [0] -> centerX
    [1] -> centerY
    [2] -> width of box in received image
    [3] -> height of box in received image
    [5] -> confidence of the object
    [6-85] -> scores for each class
     */
    private fun getAllObjectsByClass(modelOutput: Array<FloatArray>, importantClassId: Int): List<ClassifiedBox> {
        val result = mutableListOf<ClassifiedBox>()
        for (record in modelOutput) {
            val confidence = record[4]
            if (confidence < CONFIDENCE_THRESHOLD)
                continue
            val maxScoreInd = getIndOfMaxValue(record.takeLast(80)) + 5
            if (record[maxScoreInd] < SCORE_THRESHOLD) continue
            val classId = maxScoreInd - 5
            if (classId != importantClassId) continue
            result.add(
                ClassifiedBox(
                ScreenPoint(record[0] / IMAGE_WIDTH,
                record[1] / IMAGE_HEIGHT),
                record[2] / IMAGE_WIDTH,
                record[3] / IMAGE_HEIGHT,
                classId, confidence)
            )
        }
        return result
    }

    // Rotate the image of the input bitmap
    private fun Bitmap.rotate(degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    override fun processAndCloseImageProxy(imageProxy: ImageProxy): List<ClassifiedBox> {

        val imgBitmap = imageProxy.toBitmap()
        val rawBitmap = imgBitmap?.let { Bitmap.createScaledBitmap(it, 640, 640, false) }
        val bitmap = rawBitmap?.rotate(imageProxy.imageInfo.rotationDegrees.toFloat())
        imageProxy.close()

        if (bitmap != null) {

            val imgData = preProcess(bitmap)
            val inputName = ortSession?.inputNames?.iterator()?.next()
            val shape = longArrayOf(1, 3, 640, 640)
            val env = OrtEnvironment.getEnvironment()
            env.use {
                val tensor = OnnxTensor.createTensor(env, imgData, shape)
                tensor.use {
                    val output = ortSession?.run(Collections.singletonMap(inputName, tensor))
                    if (output?.get(0)?.value != null) {
                        output.use {
                            val arr = ((output.get(0)?.value) as Array<Array<FloatArray>>)[0]

                            val balls = getAllObjectsByClass(arr, 32)

                            return getTop3(balls)
                        }
                    }
                }
            }
        }
        return listOf()
    }
}