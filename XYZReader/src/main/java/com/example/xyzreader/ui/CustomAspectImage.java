package com.example.xyzreader.ui;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Created by BeTheChange on 6/14/2017.
 */

public class CustomAspectImage extends android.support.v7.widget.AppCompatImageView{
    int heightRatio=3;
    int widthRatio=2;
    public CustomAspectImage(Context context) {
        super(context);
    }

    public CustomAspectImage(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomAspectImage(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    public void setAspectRatio(int h,int w){
        heightRatio=h;
        widthRatio=w;

    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if(heightRatio!=widthRatio) {
            int height = MeasureSpec.getSize(widthMeasureSpec) * heightRatio / widthRatio;
            int heightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
            super.onMeasure(widthMeasureSpec, heightSpec);
        }
        else
            super.onMeasure(widthMeasureSpec,heightMeasureSpec);
    }
}