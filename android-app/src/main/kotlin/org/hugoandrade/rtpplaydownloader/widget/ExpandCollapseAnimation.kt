package org.hugoandrade.rtpplaydownloader.widget

import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation


class ExpandCollapseAnimation// Constructor
(// The views to be animated
        private val view: View, // The dimensions and animation values (this is stored so other changes to layout don't interfere)
        private val fromDimension: Int // Dimension to animate from
        , toDimension: Int) : Animation() {
    private var toDimension: Int = 0 // Dimension to animate to
    private var finalToDimension: Int = 0 // Dimension to animate to

    companion object {

        private val TAG = ExpandCollapseAnimation::class.java.simpleName
    }

    // Constructor
    constructor(view: View, toDimension: Int) : this(view, view.measuredHeight, toDimension) {

    }

    init {

        // Dimension to animate to
        if (toDimension == ViewGroup.LayoutParams.WRAP_CONTENT) {
            //int initialHeight = view.getMeasuredHeight();
            view.measure(ViewGroup.LayoutParams.MATCH_PARENT, toDimension)
            this.toDimension = view.measuredHeight
            this.finalToDimension = toDimension
        } else {
            this.toDimension = toDimension
            this.finalToDimension = toDimension
        }
    }// Setup references
    // the view to animate
    // Get the current starting point of the animation (the current parentWidth or height of the provided view)

    public override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
        // Used to apply the animation to the view
        // Animate given the height or parentWidth

        // android.util.Log.e(TAG, "applyTransformation = " + (fromDimension + (int) ((toDimension - fromDimension) * interpolatedTime)));
        view.layoutParams.height = if (interpolatedTime == 1f)
            finalToDimension
        else
            fromDimension + ((toDimension - fromDimension) * interpolatedTime).toInt()

        //view.setLayoutParams(view.getLayoutParams());

        // Ensure the views are measured appropriately
        view.requestLayout()
    }
}