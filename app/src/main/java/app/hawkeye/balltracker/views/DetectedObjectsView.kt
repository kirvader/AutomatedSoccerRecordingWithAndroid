package app.hawkeye.balltracker.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

private data class ObjectLocations(
    private val previous: Rect? = null,
    private val current: Rect? = null
) {
    fun drawDeltaLocationVector(canvas: Canvas?, vectorPaint: Paint) {
        if (previous == null || current == null) return
        canvas?.drawLine(
            (previous.left + previous.right) / 2.0f,
            (previous.bottom + previous.top) / 2.0f,
            (current.left + current.right) / 2.0f,
            (current.bottom + current.top) / 2.0f,
            vectorPaint
        )
    }

    fun drawBoxAroundCurrentLocation(canvas: Canvas?, paint: Paint) {
        if (current == null) return
        canvas?.drawRect(current, paint)
    }

    fun drawBoxAroundPreviousLocation(canvas: Canvas?, paint: Paint) {
        if (previous == null) return
        canvas?.drawRect(previous, paint)
    }

    fun updateCurrentLocation(newCurrentLocation: Rect?) =
        ObjectLocations(current, newCurrentLocation)
}

class DetectedObjectsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val previousDetectedObjectPaint = Paint(0).apply {
        style = Paint.Style.STROKE
        textAlign = Paint.Align.CENTER
        textSize = 55.0f
        typeface = Typeface.create("", Typeface.BOLD)
        color = Color.GREEN
    }
    private val currentDetectedObjectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        textAlign = Paint.Align.CENTER
        textSize = 55.0f
        typeface = Typeface.create("", Typeface.BOLD)
        color = Color.BLUE
    }
    private val lineBetweenCentersPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        textSize = 55.0f
        typeface = Typeface.create("", Typeface.BOLD)
        color = Color.RED
    }

    private var trackedObjectLocations = ObjectLocations()
    fun updateCurrentDetectedObject(newCurrentLocation: Rect?) {
        trackedObjectLocations = trackedObjectLocations.updateCurrentLocation(newCurrentLocation)
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        trackedObjectLocations.drawBoxAroundPreviousLocation(canvas, previousDetectedObjectPaint)
        trackedObjectLocations.drawBoxAroundCurrentLocation(canvas, currentDetectedObjectPaint)
        trackedObjectLocations.drawDeltaLocationVector(canvas, lineBetweenCentersPaint)
    }
}