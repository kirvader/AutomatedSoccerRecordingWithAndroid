package app.hawkeye.balltracker.views.configs


import app.hawkeye.balltracker.configs.choices.ModelSelectors
import app.hawkeye.balltracker.views.configs.choices.ConfigChoicesInfo
import app.hawkeye.balltracker.views.configs.choices.ModelSelectorsChoiceInfo


class ModelSelectorAdapter: ConfigAdapter() {
    override var choices: List<ConfigChoicesInfo> = listOf(
        ModelSelectorsChoiceInfo("YoloV5n6", ModelSelectors.YoloV5n6),
        ModelSelectorsChoiceInfo("YoloV5s", ModelSelectors.YoloV5s),
        ModelSelectorsChoiceInfo("YoloV5s6", ModelSelectors.YoloV5s6),
    )
}