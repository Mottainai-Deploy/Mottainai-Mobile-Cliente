package com.mottainai.cliente.design;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import com.mottainai.cliente.R;

public class DesignBoundedLayout extends LinearLayout {
    private final int maximumWidth;
    public DesignBoundedLayout(Context context,AttributeSet attrs) {
        super(context,attrs);
        TypedArray values=context.obtainStyledAttributes(attrs,R.styleable.DesignBoundedLayout);
        maximumWidth=values.getDimensionPixelSize(R.styleable.DesignBoundedLayout_designMaxWidth,Integer.MAX_VALUE);
        values.recycle();
    }
    @Override protected void onMeasure(int widthSpec,int heightSpec) {
        int available=MeasureSpec.getMode(widthSpec)==MeasureSpec.UNSPECIFIED?maximumWidth:MeasureSpec.getSize(widthSpec);
        super.onMeasure(MeasureSpec.makeMeasureSpec(Math.min(available,maximumWidth),MeasureSpec.EXACTLY),heightSpec);
    }
}
