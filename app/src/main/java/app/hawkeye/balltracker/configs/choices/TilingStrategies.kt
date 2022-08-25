package app.hawkeye.balltracker.configs.choices

import app.hawkeye.balltracker.configs.objects.TrackingSystemConfigObject
import app.hawkeye.balltracker.processors.tiling_strategies.DynamicScaleQuadSquareTiling
import app.hawkeye.balltracker.processors.tiling_strategies.DynamicScaleSingleSquareTiling
import app.hawkeye.balltracker.processors.tiling_strategies.SquareTilingStrategy
import app.hawkeye.balltracker.processors.tiling_strategies.StaticScaleSingleSquareTiling

enum class TilingStrategies(val tilingStrategy: SquareTilingStrategy) {
    StaticScaleSingleSquare(StaticScaleSingleSquareTiling()),
    DynamicScaleSingleSquare(DynamicScaleSingleSquareTiling()),
    DynamicScaleQuadSquare(DynamicScaleQuadSquareTiling()),
}