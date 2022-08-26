package app.hawkeye.balltracker.views.configs

import app.hawkeye.balltracker.configs.choices.ObjectExtractors
import app.hawkeye.balltracker.configs.choices.TilingStrategies
import app.hawkeye.balltracker.views.configs.choices.ConfigChoicesInfo
import app.hawkeye.balltracker.views.configs.choices.ObjectExtractorsChoiceInfo
import app.hawkeye.balltracker.views.configs.choices.TilingStrategyChoiceInfo

class TilingStrategyAdapter: ConfigAdapter() {
    override var choices: List<ConfigChoicesInfo> = listOf(
        TilingStrategyChoiceInfo("StaticScale_SingleSquare", TilingStrategies.StaticScaleSingleSquare),
        TilingStrategyChoiceInfo("DynamicScale_SingleSquare", TilingStrategies.DynamicScaleSingleSquare),
        TilingStrategyChoiceInfo("DynamicScale_QuadSquare", TilingStrategies.DynamicScaleQuadSquare)
    )
}