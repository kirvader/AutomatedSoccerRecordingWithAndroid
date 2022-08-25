package app.hawkeye.balltracker.processors.image

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import app.hawkeye.balltracker.configs.objects.TrackingSystemConfigObject
import app.hawkeye.balltracker.controllers.UIController
import app.hawkeye.balltracker.processors.rotate
import app.hawkeye.balltracker.processors.toBitmap
import app.hawkeye.balltracker.processors.utils.AdaptiveScreenRect
import app.hawkeye.balltracker.processors.utils.ClassifiedBox

class OnnxYoloV5WithTrackerImageProcessor() : ModelImageProcessor {

    override suspend fun processImageProxy(imageProxy: ImageProxy): ClassifiedBox? {
        val imgBitmap = imageProxy.toBitmap()
        val normalImageBitmap = imgBitmap?.rotate(imageProxy.imageInfo.rotationDegrees.toFloat()) ?: return null // image bitmap when it is vertical

        val areaOfDetection = TrackingSystemConfigObject.trackingStrategyChoice.trackingStrategy.getAreaOfDetectionAtTime(TrackingSystemConfigObject.timeKeeper.getCurrentCircleStartTime())

        val (newResolution, tiling) = TrackingSystemConfigObject.tilingStrategyChoice.tilingStrategy.tileRect(areaOfDetection, normalImageBitmap.width, normalImageBitmap.height)

        UIController.updateUIAreaOfDetectionWithNewArea(tiling.map { it.toAdaptiveScreenRect(newResolution.x, newResolution.y) })

        val resultBitmap = normalImageBitmap.let { Bitmap.createScaledBitmap(it, newResolution.x, newResolution.y, false) } ?: return null

        val result = TrackingSystemConfigObject.objectExtractorChoice.objectsExtractor.extractObjects(resultBitmap, tiling, 0)

        TrackingSystemConfigObject.trackingStrategyChoice.trackingStrategy.updateLastDetectedObjectPositionAtTime(result?.adaptiveRect, TrackingSystemConfigObject.timeKeeper.getCurrentCircleStartTime())

        return result
    }

    override fun getAreaOfDetection(): List<AdaptiveScreenRect> {
        return listOf(
            AdaptiveScreenRect(
                0f, 0f,
                640.0f / 720.0f,
                640.0f / 1280.0f
            )
        )
    }
}