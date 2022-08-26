package app.hawkeye.balltracker.views.configs

import app.hawkeye.balltracker.configs.choices.TrackingStrategies
import app.hawkeye.balltracker.views.configs.choices.ConfigChoicesInfo
import app.hawkeye.balltracker.views.configs.choices.TrackingStrategyChoiceInfo

class TrackingStrategyAdapter: ConfigAdapter() {
    override var choices: List<ConfigChoicesInfo> = listOf(
        TrackingStrategyChoiceInfo("StaticScale_SingleSquare", TrackingStrategies.SingleObjectWithStaticCamera)
    )
}