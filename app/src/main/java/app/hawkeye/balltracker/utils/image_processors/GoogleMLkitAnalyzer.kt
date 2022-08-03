package app.hawkeye.balltracker.utils.image_processors

import androidx.camera.core.ImageProxy
import app.hawkeye.balltracker.utils.ClassifiedBox
import app.hawkeye.balltracker.utils.ScreenPoint
import app.hawkeye.balltracker.utils.createLogger
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetector

private val LOG = createLogger<GoogleMLkitImageProcessor>()

private fun toClassifiedBox(
    detectedObject: DetectedObject?,
    cameraSurfaceWidth: Int,
    cameraSurfaceHeight: Int
): ClassifiedBox? {
    if (detectedObject == null) {
        return null
    }
    return ClassifiedBox(
        ScreenPoint(
            x = detectedObject.boundingBox.exactCenterX() / cameraSurfaceWidth.toFloat(),
            y = detectedObject.boundingBox.exactCenterY() / cameraSurfaceHeight.toFloat()
        ),
        width = detectedObject.boundingBox.width() / cameraSurfaceWidth.toFloat(),
        height = detectedObject.boundingBox.height() / cameraSurfaceHeight.toFloat(),
        classId = if (detectedObject.labels.isNotEmpty()) detectedObject.labels[0].index else -1,
        confidence = if (detectedObject.labels.isNotEmpty()) detectedObject.labels[0].confidence else 0.0f
    )
}

internal class GoogleMLkitImageProcessor(
    private val cameraSurfaceWidth: Int,
    private val cameraSurfaceHeight: Int,
    private val objectDetector: ObjectDetector?

) : ImageProcessor {

    override fun processAndCloseImageProxy(imageProxy: ImageProxy): List<ClassifiedBox> {
        @androidx.camera.core.ExperimentalGetImage
        val mediaImage = imageProxy.image
        var top3AppropriateObjects: List<ClassifiedBox> = listOf()
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            if (objectDetector == null)
                return listOf()
            objectDetector.process(image)
                .addOnSuccessListener { detectedObjects ->
                    top3AppropriateObjects = detectedObjects.mapNotNull {
                        toClassifiedBox(
                            it,
                            cameraSurfaceWidth,
                            cameraSurfaceHeight
                        )
                    }.take(3)
                    imageProxy.close()
                }
                .addOnFailureListener { e ->
                    LOG.e("Image processor failed to process image", e)
                    imageProxy.close()
                }
        }
        return top3AppropriateObjects
    }
}