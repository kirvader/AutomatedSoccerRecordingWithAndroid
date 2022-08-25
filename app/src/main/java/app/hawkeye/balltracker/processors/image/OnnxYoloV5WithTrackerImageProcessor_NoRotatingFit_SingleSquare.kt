package app.hawkeye.balltracker.processors.image

import android.content.Context
import app.hawkeye.balltracker.processors.tiling_strategies.DynamicScaleSingleSquareTiling
import app.hawkeye.balltracker.processors.tiling_strategies.SquareTilingStrategy
import app.hawkeye.balltracker.processors.tracking_strategies.SingleObjectTrackingStrategy
import app.hawkeye.balltracker.processors.tracking_strategies.SingleRectFollower_StaticCamera
import app.hawkeye.balltracker.processors.utils.AdaptiveScreenRect

class OnnxYoloV5WithTrackerImageProcessor_NoRotatingFit_SingleSquare(
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
        DynamicScaleSingleSquareTiling(objectExtractor.getCurrentAvailableModelSideSizes())
}