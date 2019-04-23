package org.hugoandrade.rtpplaydownloader.widget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import org.hugoandrade.rtpplaydownloader.R

class ProgressView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    private val TAG = javaClass.simpleName

    private var mPaint: Paint? = null
    private var mCursorPath: Path? = null
    private var mCursorColor: Int = 0
    private var mProgress = 0.0

    private val cursorPath: Path
        get() {
            if (mCursorPath == null) {
                mCursorPath = Path()
                val width = width
                val height = height

                val p1 = Point(0, 0)
                val p2 = Point(0, height)
                val p3 = Point((width * mProgress).toInt(), height)
                val p4 = Point((width * mProgress).toInt(), 0)

                mCursorPath?.moveTo(p1.x.toFloat(), p1.y.toFloat())
                mCursorPath?.lineTo(p2.x.toFloat(), p2.y.toFloat())
                mCursorPath?.lineTo(p3.x.toFloat(), p3.y.toFloat())
                mCursorPath?.lineTo(p4.x.toFloat(), p4.y.toFloat())
            }
            return mCursorPath as Path
        }

    init {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.ProgressView)
            mCursorColor = a.getColor(R.styleable.ProgressView_cursorColor, Color.WHITE)
            mProgress = a.getFloat(R.styleable.ProgressView_cursorProgress, 0f).toDouble()
            a.recycle()
        } else {
            mCursorColor = Color.WHITE
            mProgress = 0.0
        }

        mPaint = Paint()
        mPaint!!.style = Paint.Style.FILL
        mPaint!!.color = mCursorColor
        mPaint!!.isAntiAlias = true
    }

    fun setColor(color: Int) {
        if (mCursorColor != color) {
            mCursorColor = color
            mPaint?.color = color
            mCursorPath = null
            invalidate()
        }
    }

    fun setProgress(progress: Double) {
        if (progress < 0f || progress > 1f)
            return

        if (progress != mProgress) {
            mProgress = progress
            mCursorPath = null
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(cursorPath, mPaint!!)
    }
}
