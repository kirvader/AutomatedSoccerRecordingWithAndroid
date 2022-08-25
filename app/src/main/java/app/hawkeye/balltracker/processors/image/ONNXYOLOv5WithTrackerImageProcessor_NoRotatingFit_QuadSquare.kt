package app.hawkeye.balltracker.processors.image

import android.content.Context
import app.hawkeye.balltracker.processors.tiling_strategies.DynamicScaleQuadSquareTiling
import app.hawkeye.balltracker.processors.tiling_strategies.SquareTilingStrategy
import app.hawkeye.balltracker.processors.tracking_strategies.SingleObjectTrackingStrategy
import app.hawkeye.balltracker.processors.tracking_strategies.SingleRectFollower_StaticCamera
import app.hawkeye.balltracker.processors.utils.*
import app.hawkeye.balltracker.utils.createLogger

private val LOG = createLogger<ONNXYOLOv5WithTrackerImageProcessor_NoRotatingFit_QuadSquare>()

class ONNXYOLOv5WithTrackerImageProcessor_NoRotatingFit_QuadSquare(
    context: Context,
    getCurrentImageProcessingStart: () -> Long,
    updateUIAreaOfDetectionWithNewArea: (List<AdaptiveScreenRect>) -> Unit
) : OnnxYoloV5WithTrackerImageProcessor(
    context,
    getCurrentImageProcessingStart,
    updateUIAreaOfDetectionWithNewArea
) {
    override var singleObjectTrackingStrategy: SingleObjectTrackingStrategy =
        SingleRectFollower_StaticCamera()

    override var squareTilingStrategy: SquareTilingStrategy =
        DynamicScaleQuadSquareTiling(objectExtractor.getCurrentAvailableModelSideSizes())
}