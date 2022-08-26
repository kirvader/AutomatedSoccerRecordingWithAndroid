package app.hawkeye.balltracker.views.configs

import app.hawkeye.balltracker.configs.choices.ModelSelectors
import app.hawkeye.balltracker.configs.choices.ObjectExtractors
import app.hawkeye.balltracker.views.configs.choices.ConfigChoicesInfo
import app.hawkeye.balltracker.views.configs.choices.ModelSelectorsChoiceInfo
import app.hawkeye.balltracker.views.configs.choices.ObjectExtractorsChoiceInfo

class ObjectExtractorAdapter: ConfigAdapter() {
    override var choices: List<ConfigChoicesInfo> = listOf(
        ObjectExtractorsChoiceInfo("YoloV5n6", ObjectExtractors.YOLO)
    )
}