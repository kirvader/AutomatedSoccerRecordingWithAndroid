package app.hawkeye.balltracker.views.configs.choices

import app.hawkeye.balltracker.configs.choices.ModelSelectors
import app.hawkeye.balltracker.configs.choices.ObjectExtractors
import app.hawkeye.balltracker.configs.choices.TilingStrategies
import app.hawkeye.balltracker.configs.choices.TrackingStrategies
import app.hawkeye.balltracker.configs.objects.TrackingSystemConfigObject

open class ConfigChoicesInfo(
    val stringInfo: String,
) {
    open fun onClick() {}

    open fun isChosen() = false
}

class ModelSelectorsChoiceInfo(stringInfo: String, private val choice: ModelSelectors) : ConfigChoicesInfo(stringInfo) {
    override fun onClick() {
        TrackingSystemConfigObject.yoloModelSelectorChoice = choice
    }

    override fun isChosen(): Boolean {
        return (choice == TrackingSystemConfigObject.yoloModelSelectorChoice)
    }
}

class ObjectExtractorsChoiceInfo(stringInfo: String, private val choice: ObjectExtractors) : ConfigChoicesInfo(stringInfo) {
    override fun onClick() {
        TrackingSystemConfigObject.objectExtractorChoice = choice
    }

    override fun isChosen(): Boolean {
        return (choice == TrackingSystemConfigObject.objectExtractorChoice)
    }
}

class TilingStrategyChoiceInfo(stringInfo: String, private val choice: TilingStrategies) : ConfigChoicesInfo(stringInfo) {
    override fun onClick() {
        TrackingSystemConfigObject.tilingStrategyChoice = choice
    }

    override fun isChosen(): Boolean {
        return (choice == TrackingSystemConfigObject.tilingStrategyChoice)
    }
}

class TrackingStrategyChoiceInfo(stringInfo: String, private val choice: TrackingStrategies) : ConfigChoicesInfo(stringInfo) {
    override fun onClick() {
        TrackingSystemConfigObject.trackingStrategyChoice = choice
    }

    override fun isChosen(): Boolean {
        return (choice == TrackingSystemConfigObject.trackingStrategyChoice)
    }
}