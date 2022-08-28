package app.hawkeye.balltracker.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import app.hawkeye.balltracker.processors.utils.AdaptiveScreenRect
import app.hawkeye.balltracker.utils.createLogger

private val LOG = createLogger<AreaOfDetectionView>()

class AreaOfDetectionView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var areaOfDetection: List<AdaptiveScreenRect> = listOf()

    private val detectionSegmentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        textAlign = Paint.Align.CENTER
        textSize = 55.0f
        typeface = Typeface.create("", Typeface.BOLD)
        color = Color.RED
        strokeWidth = 5.0f
    }

    fun updateAreaOfDetection(rect: AdaptiveScreenRect) {
        areaOfDetection = listOf(rect)
        invalidate()
    }

    fun updateAreaOfDetection(newArea: List<AdaptiveScreenRect>) {
        areaOfDetection = newArea
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        for (adaptiveRect in areaOfDetection) {

            canvas?.drawRect(adaptiveRect.toScreenRect(measuredWidth, measuredHeight).toRect(), detectionSegmentPaint)
        }
    }

}