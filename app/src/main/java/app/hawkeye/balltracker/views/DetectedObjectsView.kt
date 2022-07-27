package app.hawkeye.balltracker.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class DetectedObjectsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val lastDetectedObjectPaint = Paint(0).apply {
        color = 0x101010
        maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
    }
    private val currentDetectedObjectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
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

    private var lastDetectedObjectBox: Rect? = null
    private var currentDetectedObjectBox: Rect? = null
    fun updateCurrentDetectedObject(currentBox: Rect?) {
        lastDetectedObjectBox = currentDetectedObjectBox
        currentDetectedObjectBox = currentBox
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (lastDetectedObjectBox != null)
            canvas?.drawRect(lastDetectedObjectBox!!, lastDetectedObjectPaint)

        if (currentDetectedObjectBox != null)
            canvas?.drawRect(currentDetectedObjectBox!!, currentDetectedObjectPaint)

        if (lastDetectedObjectBox != null && currentDetectedObjectBox != null)
            canvas?.drawLine(
                (lastDetectedObjectBox!!.left + lastDetectedObjectBox!!.right) / 2.0f,
                (lastDetectedObjectBox!!.bottom + lastDetectedObjectBox!!.top) / 2.0f,
                (currentDetectedObjectBox!!.left + currentDetectedObjectBox!!.right) / 2.0f,
                (currentDetectedObjectBox!!.bottom + currentDetectedObjectBox!!.top) / 2.0f,
                lineBetweenCentersPaint
            )

    }
}