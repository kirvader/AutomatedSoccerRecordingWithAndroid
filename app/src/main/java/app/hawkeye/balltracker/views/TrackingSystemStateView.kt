package app.hawkeye.balltracker.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import app.hawkeye.balltracker.configs.objects.TrackingSystemConfigObject
import app.hawkeye.balltracker.utils.createLogger
import com.hawkeye.movement.interfaces.TrackingSystemControllerBase
import com.hawkeye.movement.utils.AngleMeasure
import com.hawkeye.movement.utils.Point
import kotlinx.android.synthetic.main.activity_camera.view.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin


private val LOG = createLogger<TrackingSystemStateView>()

// represents tracking system state
class TrackingSystemStateView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val guidelinesQuantity = 8

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL_AND_STROKE
        color = Color.WHITE
        strokeWidth = 3.0f
    }

    private val guidelinesPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.GRAY
        strokeWidth = 0.5f
    }
    private val trackedObjectPointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL_AND_STROKE
        color = Color.RED
        strokeWidth = 20f
    }
    private val cameraFOVSegmentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL_AND_STROKE
        color = Color.argb(0.6f, 0.4f, 0.4f, 0.4f)
        strokeWidth = 3f
    }

    fun updateLocatorState() {
        invalidate()
    }

    private fun drawAngle(canvas: Canvas?, rect: RectF, direction: AngleMeasure, FOVAngle: AngleMeasure) {
        canvas?.drawArc(rect, -(direction + FOVAngle / 2.0f).degree(), FOVAngle.degree(), true, cameraFOVSegmentPaint)
    }

    private fun drawTrackedObjectPointAtTime(canvas: Canvas?, canvasCenterX: Float, canvasCenterY: Float, distanceFromCenter: Float, point: Point?) {
        if (point == null) {
            return
        }
        val x = canvasCenterX + distanceFromCenter * cos(point.getAngle().radian())
        val y = canvasCenterY + distanceFromCenter * sin(point.getAngle().radian())

        canvas?.drawPoint(x, y, trackedObjectPointPaint)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val locatorSize = min(width, height)

        val centerX = locatorSize.toFloat() / 2
        val centerY = locatorSize.toFloat() / 2
        val radius = locatorSize / 2 - circlePaint.strokeWidth



        canvas?.drawCircle(centerX, centerY, radius, circlePaint)

        if (TrackingSystemConfigObject.movementControllerSystem != null) {

            val (direction, FOVAngle) = TrackingSystemConfigObject.movementControllerSystem.getCameraDirectionAndFOVAngleAtTime(
                System.currentTimeMillis()
            )

            val rect = RectF(
                circlePaint.strokeWidth,
                circlePaint.strokeWidth,
                locatorSize - circlePaint.strokeWidth,
                locatorSize - circlePaint.strokeWidth
            )
            drawAngle(canvas, rect, direction, FOVAngle)
        }

        for (i in 0 until guidelinesQuantity / 2) {
            val angle = i * 2 * PI.toFloat() / guidelinesQuantity

            val x = radius * cos(angle)
            val y = radius * sin(angle)

            canvas?.drawLine(centerX + x, centerY + y, centerX - x, centerY - y, guidelinesPaint)
        }

        if (TrackingSystemConfigObject.movementControllerSystem != null) {
            drawTrackedObjectPointAtTime(canvas, centerX, centerY, distanceFromCenter = radius * 0.6f, TrackingSystemConfigObject.movementControllerSystem.getObjectPositionAtTime(System.currentTimeMillis()))
        }

    }
}