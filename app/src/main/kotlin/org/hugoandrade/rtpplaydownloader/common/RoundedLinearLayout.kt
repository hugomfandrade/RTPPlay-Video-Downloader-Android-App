package org.hugoandrade.rtpplaydownloader.common

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.os.Build
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.TypedValue
import android.widget.LinearLayout
import org.hugoandrade.rtpplaydownloader.R
import java.util.*

class RoundedLinearLayout : LinearLayout {

    private var doCrop: Boolean = false
    private val rect = RectF()
    private val path = Path()

    private var strokeWidth: Int = 0

    constructor(context: Context) : super(context) {

        init(context, null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {

        init(context, attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {

        init(context, attrs, defStyleAttr)
    }

    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {

        val standardA = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.background))
        val hasBackgroundAttribute = standardA.hasValue(0)
        standardA.recycle()

        if (!hasBackgroundAttribute) {

            val a = context.obtainStyledAttributes(attrs, R.styleable.RoundedLinearLayout, defStyleAttr, 0)
            val strokeWidth = a.getDimension(R.styleable.RoundedLinearLayout_stroke_width, convertDpToPixel(2f, context)).toInt()
            val strokeColor = a.getColor(R.styleable.RoundedLinearLayout_stroke_color, getThemePrimaryDarkColor(context))
            val backgroundColor = a.getColor(R.styleable.RoundedLinearLayout_background_color, Color.WHITE)
            a.recycle()

            this.strokeWidth = strokeWidth

            val gradientDrawable = GradientDrawable()
            gradientDrawable.setColor(backgroundColor)
            gradientDrawable.setStroke(strokeWidth, strokeColor)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val outerRadii = FloatArray(8)
                Arrays.fill(outerRadii, 0f)
                val r = RoundRectShape(outerRadii, null, null)
                val mask = ShapeDrawable(r)
                mask.paint.color = Color.WHITE
                val rippleDrawable = RippleDrawable(ColorStateList.valueOf(Color.WHITE), gradientDrawable, mask)

                background = rippleDrawable
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    background = gradientDrawable
                } else {
                    setBackgroundDrawable(gradientDrawable)
                }
            }

            doCrop = true
        }

        isClickable = true
    }

    override fun setBackgroundDrawable(background: Drawable) {
        super.setBackgroundDrawable(background)

        doCrop = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (w != oldw || h != oldh)
            requestLayout()

        val cornerRadius = Math.min(w, h) / 2f

        updateBackgroundDrawable()

        // compute the path
        path.reset()
        rect.set(
                strokeWidth.toFloat(),
                strokeWidth.toFloat(),
                (w - strokeWidth).toFloat(),
                (h - strokeWidth).toFloat())
        path.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
        path.close()
    }

    private fun updateBackgroundDrawable() {
        if (doCrop) {
            val drawable = background

            val gradientDrawable: GradientDrawable

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (drawable is RippleDrawable) {

                    val childDrawable = drawable.getDrawable(0)

                    if (childDrawable is GradientDrawable) {
                        gradientDrawable = childDrawable

                        val mask = drawable.findDrawableByLayerId(android.R.id.mask)

                        if (mask is ShapeDrawable) {
                            val outerRadii = FloatArray(8)
                            Arrays.fill(outerRadii, Math.min(measuredWidth, measuredHeight) / 2f)
                            val r = RoundRectShape(outerRadii, null, null)
                            mask.shape = r
                        }
                    } else {
                        return
                    }
                } else {
                    return
                }
            } else {
                if (drawable is GradientDrawable) {
                    gradientDrawable = drawable
                } else {
                    return
                }
            }

            gradientDrawable.cornerRadius = Math.min(measuredWidth, measuredHeight) / 2f
        }
    }


    override fun onDraw(canvas: Canvas) {
        if (doCrop) {
            canvas.clipPath(path)
            super.onDraw(canvas)
        } else {
            super.onDraw(canvas)
        }
    }

    private fun getThemePrimaryDarkColor(context: Context): Int {
        val colorAttr: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            colorAttr = android.R.attr.colorPrimaryDark
        } else {
            //Get colorAccent defined for AppCompat
            colorAttr = context.resources.getIdentifier("colorPrimaryDark", "attr", context.packageName)
        }
        val outValue = TypedValue()
        context.theme.resolveAttribute(colorAttr, outValue, true)
        return outValue.data
    }

    private fun convertDpToPixel(dp: Float, context: Context): Float {
        return dp * (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }
}