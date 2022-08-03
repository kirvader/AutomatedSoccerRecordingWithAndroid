package app.hawkeye.balltracker.utils


data class ImageAnalyzerChoice (
    val imageAnalysisName: String,
    val setupCurrentImageAnalysisMethod: () -> Unit
)
