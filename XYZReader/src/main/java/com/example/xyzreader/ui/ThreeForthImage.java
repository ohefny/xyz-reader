package com.example.xyzreader.ui;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Created by BeTheChange on 6/14/2017.
 */

public class ThreeForthImage extends android.support.v7.widget.AppCompatImageView{
    public ThreeForthImage(Context context) {
        super(context);
    }

    public ThreeForthImage(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ThreeForthImage(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int threeTwoHeight= MeasureSpec.getSize(widthMeasureSpec)*3/4;
        int threeTwoHeightSpec=MeasureSpec.makeMeasureSpec(threeTwoHeight,MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, threeTwoHeightSpec);
    }
}