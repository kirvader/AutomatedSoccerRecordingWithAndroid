package app.hawkeye.balltracker.processors.yoloDataProcessors

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import app.hawkeye.balltracker.processors.utils.*
import java.nio.FloatBuffer
import java.util.*

class YoloDataProcessor(context: Context, modelId: Int, private val modelInputImageSideSize: Int) :
    DataProcessor {
    private var ortSession: OrtSession

    private fun readYoloModel(context: Context, modelId: Int): ByteArray {
        return context.resources.openRawResource(modelId).readBytes()
    }

    init {
        ortSession = OrtEnvironment.getEnvironment().createSession(readYoloModel(context, modelId))
    }

    private fun getTopDetectedObject(foundObjects: List<ClassifiedBox>): ClassifiedBox? {
        return foundObjects.maxByOrNull { it.confidence }
    }

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
    private fun getAllObjectsByClassFromYOLO(
        modelOutput: Array<FloatArray>,
        importantClassId: Int,
        imageWidth: Int,
        imageHeight: Int
    ): List<ClassifiedBox> {
        val result = mutableListOf<ClassifiedBox>()
        for (record in modelOutput) {
            val confidence = record[4]
            if (confidence < CONFIDENCE_THRESHOLD)
                continue
            val maxScoreInd = getIndOfMaxValue(record.takeLast(80)) + 5
            if (record[maxScoreInd] < SCORE_THRESHOLD) continue
            val classId = maxScoreInd - 5
            if (importantClassId != -1 && importantClassId != classId) continue

            val width = record[2] / modelInputImageSideSize
            val height = record[3] / modelInputImageSideSize
            val left = record[0] / modelInputImageSideSize - width / 2
            val top = record[1] / modelInputImageSideSize - height / 2

            result.add(
                ClassifiedBox(
                    AdaptiveScreenRect(
                        left,
                        top,
                        width,
                        height
                    ),
                    classId, confidence
                )
            )
        }
        return result
    }

    override fun process(data: FloatBuffer, soughtClassIds: Int): ClassifiedBox? {
        val inputName = ortSession.inputNames?.iterator()?.next()
        val shape =
            longArrayOf(1, 3, modelInputImageSideSize.toLong(), modelInputImageSideSize.toLong())
        val env = OrtEnvironment.getEnvironment()

        env.use {
            val tensor = OnnxTensor.createTensor(env, data, shape)
            tensor.use {
                val output = ortSession.run(Collections.singletonMap(inputName, tensor))

                if (output?.get(0)?.value != null) {
                    output.use {
                        val arr = ((output.get(0)?.value) as Array<Array<FloatArray>>)[0]

                        val balls = getAllObjectsByClassFromYOLO(
                            arr,
                            soughtClassIds,
                            modelInputImageSideSize,
                            modelInputImageSideSize
                        )

                        return getTopDetectedObject(balls)
                    }
                }
            }
        }
        return null
    }

    override fun getModelInputSideSize(): Int {
        return modelInputImageSideSize
    }

    companion object {
        private const val CONFIDENCE_THRESHOLD: Float = 0.5F
        private const val SCORE_THRESHOLD: Float = 0.4F
    }
}