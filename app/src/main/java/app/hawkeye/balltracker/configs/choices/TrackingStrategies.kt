package app.hawkeye.balltracker.configs.choices

import app.hawkeye.balltracker.processors.tracking_strategies.SingleObjectTrackingStrategy
import app.hawkeye.balltracker.processors.tracking_strategies.SingleRectFollower_StaticCamera

enum class TrackingStrategies(val trackingStrategy: SingleObjectTrackingStrategy) {
    SingleObjectWithStaticCamera(SingleRectFollower_StaticCamera())
}