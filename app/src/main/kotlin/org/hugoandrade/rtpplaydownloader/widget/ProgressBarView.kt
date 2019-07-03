package org.hugoandrade.rtpplaydownloader.widget

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import org.hugoandrade.rtpplaydownloader.R
import java.util.*

class ProgressBarView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    private val TAG = javaClass.simpleName

    private var mPaint: Paint = Paint()
    private var mIndicatorPath: Path? = null
    private var mIndicatorColor: Int = 0
    private var mIndicatorWidth: Int = 0
    private var mIndicatorType: Int = 0
    private var isAnimEnabled: Boolean = false

    private var mHandler: Handler = Handler()
    private var animTimer: Timer? = null
    private var animInitX = 0f

    private val cursorPath: Path?
        get() {
            if (mIndicatorPath == null) {
                mIndicatorPath = Path()

                val height = height
                val p1 = Point(animInitX.toInt(), 0)
                val p2 = Point(animInitX.toInt(), height)
                val p3 = Point((animInitX + mIndicatorWidth).toInt(), height)
                val p4 = Point((animInitX + mIndicatorWidth).toInt(), 0)

                mIndicatorPath?.moveTo(p1.x.toFloat(), p1.y.toFloat())
                mIndicatorPath?.lineTo(p2.x.toFloat(), p2.y.toFloat())
                mIndicatorPath?.lineTo(p3.x.toFloat(), p3.y.toFloat())
                mIndicatorPath?.lineTo(p4.x.toFloat(), p4.y.toFloat())
                invalidate()
            }
            return mIndicatorPath
        }

    init {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {

        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.ProgressBarView)
            mIndicatorColor = a.getColor(R.styleable.ProgressBarView_indicator_color, WidgetUtils.getThemePrimaryDarkColor(context))
            mIndicatorWidth = a.getDimension(R.styleable.ProgressBarView_indicator_width, WidgetUtils.convertDpToPixel(70f, context)).toInt()
            mIndicatorType = a.getInt(R.styleable.ProgressBarView_bar_type, 0)
            isAnimEnabled = a.getBoolean(R.styleable.ProgressBarView_anim_enabled, false)
            a.recycle()
        } else {
            mIndicatorColor = WidgetUtils.getThemePrimaryDarkColor(context)
            mIndicatorWidth = WidgetUtils.convertDpToPixel(70f, context).toInt()
            mIndicatorType = 0
            isAnimEnabled = false
        }

        mPaint = Paint()
        mPaint.style = Paint.Style.FILL
        mPaint.color = mIndicatorColor
        mPaint.isAntiAlias = true

        setProgressAnimation(isAnimEnabled)
    }

    private fun setProgressAnimation(enabled: Boolean) {
        isAnimEnabled = enabled
        if (!isAnimEnabled) {
            animInitX = -mIndicatorWidth.toFloat()
            animTimer?.cancel()
        }
        else {
            animTimer?.cancel()
            animTimer = Timer()
            animTimer?.scheduleAtFixedRate(object : TimerTask() {

                var i : Int = 0

                override fun run() {
                    android.util.Log.e(TAG, "run anim")

                    val width = width
                    val totalWidth = width + mIndicatorWidth

                    val percentage: Float

                    when (mIndicatorType) {
                        0 -> {
                            percentage = if (i < 100) {
                                i.toFloat() / 100f
                            } else {
                                (200f - i.toFloat()) / 100f
                            }
                            i += 1
                            i %= 201
                        }
                        1 -> {
                            percentage = i.toFloat() / 100f
                            i += 1
                            i %= 101
                        }
                        else -> {
                            percentage = 0f
                            i += 1
                            i %= 101
                        }
                    }

                    animInitX = percentage * totalWidth - mIndicatorWidth

                    mHandler.post {
                        mIndicatorPath = null
                        invalidate()
                    }
                }

            }, 0, 30)
        }
    }

    fun setColor(color: Int) {
        if (mIndicatorColor != color) {
            mIndicatorColor = color
            mPaint.color = color
            mIndicatorPath = null
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        cursorPath?.let { cursorPath-> canvas.drawPath(cursorPath, mPaint) }
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)

        if (visibility == VISIBLE) {
            setProgressAnimation(isAnimEnabled)
        }
        else {
            setProgressAnimation(false)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        setProgressAnimation(false)
    }
}
