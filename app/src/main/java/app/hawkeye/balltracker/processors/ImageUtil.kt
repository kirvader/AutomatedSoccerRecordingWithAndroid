/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.hawkeye.balltracker.processors

import android.content.res.AssetManager
import android.graphics.*
import androidx.camera.core.ImageProxy
import app.hawkeye.balltracker.utils.AdaptiveRect
import app.hawkeye.balltracker.utils.ClassifiedBox
import app.hawkeye.balltracker.utils.ScreenPoint
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.nio.FloatBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel


const val DIM_BATCH_SIZE = 1
const val DIM_PIXEL_SIZE = 3


fun loadModelFile(assets: AssetManager, modelFilename: String?): MappedByteBuffer? {
    val fileDescriptor = assets.openFd(modelFilename!!)
    val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
    val fileChannel: FileChannel = inputStream.channel
    val startOffset = fileDescriptor.startOffset
    val declaredLength = fileDescriptor.declaredLength
    return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
}

fun preProcess(bitmap: Bitmap, imageSizeX: Int, imageSizeY: Int, offsetX: Int = 0, offsetY: Int = 0): FloatBuffer {
    val imgData = FloatBuffer.allocate(
        DIM_BATCH_SIZE
                * DIM_PIXEL_SIZE
                * imageSizeX
                * imageSizeY
    )
    imgData.rewind()
    val wholeImageStride = bitmap.width * bitmap.height
    val bmpData = IntArray(wholeImageStride)
    bitmap.getPixels(bmpData, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

    val resultImageStride = imageSizeX * imageSizeY
    for (i in 0 until imageSizeY) {
        for (j in 0 until imageSizeX) {
            val wholeImageIndex = bitmap.width * (i + offsetY) + (j + offsetX)
            val resultImageIndex = i * imageSizeX + j
            val pixelValue = bmpData[wholeImageIndex]
            imgData.put(resultImageIndex,                            (pixelValue shr 16 and 0xFF) / 255.0F)
            imgData.put(resultImageIndex + resultImageStride,        (pixelValue shr 8 and 0xFF) / 255.0F)
            imgData.put(resultImageIndex + resultImageStride * 2,    (pixelValue and 0xFF) / 255.0F)
        }
    }

    imgData.rewind()
    return imgData
}

fun ImageProxy.toBitmap(): Bitmap? {
    val nv21 = yuv420888ToNv21(this)
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    return yuvImage.toBitmap()
}

private fun YuvImage.toBitmap(): Bitmap? {
    val out = ByteArrayOutputStream()
    if (!compressToJpeg(Rect(0, 0, width, height), 100, out))
        return null
    val imageBytes: ByteArray = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

private fun yuv420888ToNv21(image: ImageProxy): ByteArray {
    val pixelCount = image.cropRect.width() * image.cropRect.height()
//    XLog.tag("yuv").i(pixelCount)
    val pixelSizeBits = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888)
    val outputBuffer = ByteArray(pixelCount * pixelSizeBits / 8)
    imageToByteBuffer(image, outputBuffer, pixelCount)
    return outputBuffer
}

private fun imageToByteBuffer(image: ImageProxy, outputBuffer: ByteArray, pixelCount: Int) {
    assert(image.format == ImageFormat.YUV_420_888)

    val imageCrop = image.cropRect
    val imagePlanes = image.planes

    imagePlanes.forEachIndexed { planeIndex, plane ->
        // How many values are read in input for each output value written
        // Only the Y plane has a value for every pixel, U and V have half the resolution i.e.
        //
        // Y Plane            U Plane    V Plane
        // ===============    =======    =======
        // Y Y Y Y Y Y Y Y    U U U U    V V V V
        // Y Y Y Y Y Y Y Y    U U U U    V V V V
        // Y Y Y Y Y Y Y Y    U U U U    V V V V
        // Y Y Y Y Y Y Y Y    U U U U    V V V V
        // Y Y Y Y Y Y Y Y
        // Y Y Y Y Y Y Y Y
        // Y Y Y Y Y Y Y Y
        val outputStride: Int

        // The index in the output buffer the next value will be written at
        // For Y it's zero, for U and V we start at the end of Y and interleave them i.e.
        //
        // First chunk        Second chunk
        // ===============    ===============
        // Y Y Y Y Y Y Y Y    V U V U V U V U
        // Y Y Y Y Y Y Y Y    V U V U V U V U
        // Y Y Y Y Y Y Y Y    V U V U V U V U
        // Y Y Y Y Y Y Y Y    V U V U V U V U
        // Y Y Y Y Y Y Y Y
        // Y Y Y Y Y Y Y Y
        // Y Y Y Y Y Y Y Y
        var outputOffset: Int

        when (planeIndex) {
            0 -> {
                outputStride = 1
                outputOffset = 0
            }
            1 -> {
                outputStride = 2
                // For NV21 format, U is in odd-numbered indices
                outputOffset = pixelCount + 1
            }
            2 -> {
                outputStride = 2
                // For NV21 format, V is in even-numbered indices
                outputOffset = pixelCount
            }
            else -> {
                // Image contains more than 3 planes, something strange is going on
                return@forEachIndexed
            }
        }

        val planeBuffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride

        // We have to divide the width and height by two if it's not the Y plane
        val planeCrop = if (planeIndex == 0) {
            imageCrop
        } else {
            Rect(
                imageCrop.left / 2,
                imageCrop.top / 2,
                imageCrop.right / 2,
                imageCrop.bottom / 2
            )
        }

        val planeWidth = planeCrop.width()
        val planeHeight = planeCrop.height()

        // Intermediate buffer used to store the bytes of each row
        val rowBuffer = ByteArray(plane.rowStride)

        // Size of each row in bytes
        val rowLength = if (pixelStride == 1 && outputStride == 1) {
            planeWidth
        } else {
            // Take into account that the stride may include data from pixels other than this
            // particular plane and row, and that could be between pixels and not after every
            // pixel:
            //
            // |---- Pixel stride ----|                    Row ends here --> |
            // | Pixel 1 | Other Data | Pixel 2 | Other Data | ... | Pixel N |
            //
            // We need to get (N-1) * (pixel stride bytes) per row + 1 byte for the last pixel
            (planeWidth - 1) * pixelStride + 1
        }

        for (row in 0 until planeHeight) {
            // Move buffer position to the beginning of this row
            planeBuffer.position(
                (row + planeCrop.top) * rowStride + planeCrop.left * pixelStride
            )

            if (pixelStride == 1 && outputStride == 1) {
                // When there is a single stride value for pixel and output, we can just copy
                // the entire row in a single step
                planeBuffer.get(outputBuffer, outputOffset, rowLength)
                outputOffset += rowLength
            } else {
                // When either pixel or output have a stride > 1 we must copy pixel by pixel
                planeBuffer.get(rowBuffer, 0, rowLength)
                for (col in 0 until planeWidth) {
                    outputBuffer[outputOffset] = rowBuffer[col * pixelStride]
                    outputOffset += outputStride
                }
            }
        }
    }
}

fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
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
fun getAllObjectsByClassFromYOLO(
    modelOutput: Array<FloatArray>,
    importantClassId: Int,
    confidenceThreshold: Float,
    scoreThreshold: Float,
    imageWidth: Int,
    imageHeight: Int
): List<ClassifiedBox> {
    val result = mutableListOf<ClassifiedBox>()
    for (record in modelOutput) {
        val confidence = record[4]
        if (confidence < confidenceThreshold)
            continue
        val maxScoreInd = getIndOfMaxValue(record.takeLast(80)) + 5
        if (record[maxScoreInd] < scoreThreshold) continue
        val classId = maxScoreInd - 5
        if (importantClassId != -1 && classId != importantClassId) continue
        result.add(
            ClassifiedBox(
                AdaptiveRect(
                    ScreenPoint(
                        record[0] / imageWidth,
                        record[1] / imageHeight
                    ),
                    record[2] / imageWidth,
                    record[3] / imageHeight
                ),
                classId, confidence
            )
        )
    }
    return result
}
