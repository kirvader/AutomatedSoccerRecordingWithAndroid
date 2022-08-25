package app.hawkeye.balltracker.controllers

import androidx.camera.core.Preview
import app.hawkeye.balltracker.processors.utils.AdaptiveScreenRect

object UIController {
    var updateUIOnStartRecording: (() -> Unit) = {}

    var updateUIOnStopRecording: (() -> Unit) = {}

    var updateUIAreaOfDetectionWithNewArea: (List<AdaptiveScreenRect>) -> Unit = { _ -> }

    var updateUIWhenImageAnalyzerFinished: (AdaptiveScreenRect?, String) -> Unit = { _, _ -> }

    var getPreviewSurfaceProvider: () -> Preview.SurfaceProvider? = { null }
}