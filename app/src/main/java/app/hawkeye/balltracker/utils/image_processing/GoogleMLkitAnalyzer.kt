package app.hawkeye.balltracker.utils.image_processing

import android.app.Activity
import android.content.Context.CAMERA_SERVICE
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.SparseIntArray
import android.view.Surface
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import app.hawkeye.balltracker.utils.ClassifiedBox
import app.hawkeye.balltracker.utils.ScreenPoint
import app.hawkeye.balltracker.utils.createLogger
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetector

private val LOG = createLogger<GoogleMLkitAnalyzer>()


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
        classId = if (detectedObject.labels.isNotEmpty()) detectedObject.labels[0].index else 1000,
        confidence = detectedObject.labels[0].confidence
    )
}

internal class GoogleMLkitAnalyzer(
    private val objectDetector: ObjectDetector?,
    private val cameraSurfaceWidth: Int,
    private val cameraSurfaceHeight: Int,
    private val onUpdateUI: (List<ClassifiedBox>) -> Unit,
    private val onUpdateCameraFOV: (List<ClassifiedBox>) -> Unit

) : ImageAnalysis.Analyzer {

    override fun analyze(imageProxy: ImageProxy) {
        @androidx.camera.core.ExperimentalGetImage
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            if (objectDetector == null) return
            objectDetector.process(image)
                .addOnSuccessListener { detectedObjects ->
                    val top3AppropriateObjects = detectedObjects.mapNotNull {
                        toClassifiedBox(
                            it,
                            cameraSurfaceWidth,
                            cameraSurfaceHeight
                        )
                    }.take(3)
                    onUpdateUI(top3AppropriateObjects)
                    onUpdateCameraFOV(top3AppropriateObjects)
                    imageProxy.close()
                }
                .addOnFailureListener { e ->
                    LOG.e("Image processor failed to process image", e)

                    imageProxy.close()
                }
        }
    }

    // We can switch analyzer in the app, need to make sure the native resources are freed
    protected fun finalize() {
        objectDetector?.close()
    }
}