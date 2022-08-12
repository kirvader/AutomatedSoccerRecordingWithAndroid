package app.hawkeye.balltracker.processors

import androidx.camera.core.ImageProxy
import app.hawkeye.balltracker.processors.interfaces.ModelImageProcessor
import app.hawkeye.balltracker.utils.AdaptiveRect
import app.hawkeye.balltracker.utils.ClassifiedBox
import app.hawkeye.balltracker.utils.ScreenPoint
import app.hawkeye.balltracker.utils.createLogger
import com.google.android.gms.tasks.Tasks.await
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

private val LOG = createLogger<GoogleMLkitModelImageProcessor>()

private fun toClassifiedBox(
    detectedObject: DetectedObject?,
    cameraSurfaceWidth: Int,
    cameraSurfaceHeight: Int
): ClassifiedBox? {
    if (detectedObject == null) {
        return null
    }
    return ClassifiedBox(
        AdaptiveRect(
            ScreenPoint(
                x = detectedObject.boundingBox.exactCenterX() / cameraSurfaceWidth.toFloat(),
                y = detectedObject.boundingBox.exactCenterY() / cameraSurfaceHeight.toFloat()
            ),
            width = detectedObject.boundingBox.width() / cameraSurfaceWidth.toFloat(),
            height = detectedObject.boundingBox.height() / cameraSurfaceHeight.toFloat()
        ),
        classId = if (detectedObject.labels.isNotEmpty()) detectedObject.labels[0].index else -1,
        confidence = if (detectedObject.labels.isNotEmpty()) detectedObject.labels[0].confidence else 0.0f
    )
}

internal class GoogleMLkitModelImageProcessor() : ModelImageProcessor {
    private val objectDetectorOptions = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
        .enableClassification()
        .build()
    private val objectDetector = ObjectDetection.getClient(objectDetectorOptions)


    override fun processImageProxy(imageProxy: ImageProxy): List<ClassifiedBox> {
        @androidx.camera.core.ExperimentalGetImage
        val mediaImage = imageProxy.image
        var top3AppropriateObjects: List<ClassifiedBox> = listOf()
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            await(objectDetector.process(image)
                .addOnSuccessListener { detectedObjects ->
                    top3AppropriateObjects = detectedObjects.mapNotNull {
                        toClassifiedBox(
                            it,
                            image.width,
                            image.height
                        )
                    }.take(3)
                }
                .addOnFailureListener { e ->
                    LOG.e("Image processor failed to process image", e)
                })
        }
        return top3AppropriateObjects
    }
}