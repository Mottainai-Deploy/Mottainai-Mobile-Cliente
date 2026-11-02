package com.mottainai.cliente.design;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import com.mottainai.cliente.R;

/** Mantém um espaço compacto do design na largura planejada, adaptando-se a telas estreitas. */
public class DesignBoundedLayout extends LinearLayout {
    private final int maximumWidth;
    /** Inicializa DesignBoundedLayout com os dados e as dependências recebidos. */
    public DesignBoundedLayout(Context context,AttributeSet attrs) {
        super(context,attrs);
        TypedArray values=context.obtainStyledAttributes(attrs,R.styleable.DesignBoundedLayout);
        maximumWidth=values.getDimensionPixelSize(R.styleable.DesignBoundedLayout_designMaxWidth,Integer.MAX_VALUE);
        values.recycle();
    }
    /** Calcula as dimensões do componente de acordo com o espaço disponível. */
    @Override protected void onMeasure(int widthSpec,int heightSpec) {
        int available=MeasureSpec.getMode(widthSpec)==MeasureSpec.UNSPECIFIED?maximumWidth:MeasureSpec.getSize(widthSpec);
        super.onMeasure(MeasureSpec.makeMeasureSpec(Math.min(available,maximumWidth),MeasureSpec.EXACTLY),heightSpec);
    }
}
