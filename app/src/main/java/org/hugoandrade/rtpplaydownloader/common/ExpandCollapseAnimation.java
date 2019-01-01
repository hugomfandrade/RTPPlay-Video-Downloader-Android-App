package org.hugoandrade.rtpplaydownloader.common;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;

public class ExpandCollapseAnimation extends Animation {

    private static final String TAG = ExpandCollapseAnimation.class.getSimpleName();

    // The views to be animated
    private View view;

    // The dimensions and animation values (this is stored so other changes to layout don't interfere)
    private final int fromDimension; // Dimension to animate from
    private int toDimension; // Dimension to animate to
    private int finalToDimension; // Dimension to animate to


    // Constructor
    public ExpandCollapseAnimation(View view, int toDimension) {
        this(view, view.getMeasuredHeight(), toDimension);

    }

    // Constructor
    public ExpandCollapseAnimation(View view, int fromDimension, int toDimension) {
        // Setup references
        // the view to animate
        this.view = view;
        // Get the current starting point of the animation (the current parentWidth or height of the provided view)
        this.fromDimension = fromDimension;

        // Dimension to animate to
        if (toDimension == ViewGroup.LayoutParams.WRAP_CONTENT) {
            //int initialHeight = view.getMeasuredHeight();
            view.measure(ViewGroup.LayoutParams.MATCH_PARENT, toDimension);
            this.toDimension = view.getMeasuredHeight();
            this.finalToDimension = toDimension;
        }
        else {
            this.toDimension = toDimension;
            this.finalToDimension = toDimension;
        }
    }

    @Override
    public void applyTransformation(float interpolatedTime, Transformation t) {
        // Used to apply the animation to the view
        // Animate given the height or parentWidth

        // android.util.Log.e(TAG, "applyTransformation = " + (fromDimension + (int) ((toDimension - fromDimension) * interpolatedTime)));
        view.getLayoutParams().height = interpolatedTime == 1?
                finalToDimension :
                fromDimension + (int) ((toDimension - fromDimension) * interpolatedTime);

        //view.setLayoutParams(view.getLayoutParams());

        // Ensure the views are measured appropriately
        view.requestLayout();
    }
}
