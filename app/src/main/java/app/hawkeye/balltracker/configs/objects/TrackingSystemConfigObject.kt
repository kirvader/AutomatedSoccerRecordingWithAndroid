package app.hawkeye.balltracker.configs.objects

import app.hawkeye.balltracker.App
import app.hawkeye.balltracker.configs.choices.ModelSelectors
import app.hawkeye.balltracker.configs.choices.ObjectExtractors
import app.hawkeye.balltracker.configs.choices.TilingStrategies
import app.hawkeye.balltracker.configs.choices.TrackingStrategies
import app.hawkeye.balltracker.controllers.FootballTrackingSystemController
import app.hawkeye.balltracker.controllers.time.TimeKeeper


object TrackingSystemConfigObject {
    var availableModelSideSizes: List<Int> = listOf(64, 128, 256, 512)

    var timeKeeper = TimeKeeper()

    var objectExtractorChoice: ObjectExtractors = ObjectExtractors.YOLO
    var yoloModelSelectorChoice: ModelSelectors = ModelSelectors.YoloV5s
    var tilingStrategyChoice: TilingStrategies = TilingStrategies.DynamicScaleSingleSquare
    var trackingStrategyChoice: TrackingStrategies = TrackingStrategies.SingleObjectWithStaticCamera

    var movementControllerSystem: FootballTrackingSystemController = FootballTrackingSystemController(App.getRotatableDevice())
}