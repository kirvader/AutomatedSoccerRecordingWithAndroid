package app.hawkeye.balltracker.processors.image

import android.content.Context
import app.hawkeye.balltracker.processors.tiling_strategies.DynamicScaleSingleSquareTiling
import app.hawkeye.balltracker.processors.tiling_strategies.SquareTilingStrategy
import app.hawkeye.balltracker.processors.tracking_strategies.SingleObjectTrackingStrategy
import app.hawkeye.balltracker.processors.tracking_strategies.SingleRectFollower_RotatingCamera
import app.hawkeye.balltracker.processors.utils.*
import app.hawkeye.balltracker.utils.createLogger

private val LOG = createLogger<ONNXYOLOv5WithTrackerImageProcessor_NoRotatingFit_QuadSquare>()

class ONNXYOLOv5WithTrackerImageProcessor_WithRotationFit_QuadSquare(
    context: Context,
    getCurrentImageProcessingStart: () -> Long,
    updateUIAreaOfDetectionWithNewArea: (List<AdaptiveScreenRect>) -> Unit,
    getBallPositionAtTime: (Long) -> AdaptiveScreenVector?,
) : OnnxYoloV5WithTrackerImageProcessor(
    context,
    getCurrentImageProcessingStart,
    updateUIAreaOfDetectionWithNewArea
) {
    override var singleObjectTrackingStrategy: SingleObjectTrackingStrategy = SingleRectFollower_RotatingCamera(getBallPositionAtTime)

    override var squareTilingStrategy: SquareTilingStrategy =
        DynamicScaleSingleSquareTiling(objectExtractor.getCurrentAvailableModelSideSizes())
}