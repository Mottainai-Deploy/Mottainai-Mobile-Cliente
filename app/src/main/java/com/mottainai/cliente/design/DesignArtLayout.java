package com.mottainai.cliente.design;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import com.mottainai.cliente.R;

/** Coordenadas proporcionais são restritas às ilustrações e anotações do mapa. */
public class DesignArtLayout extends ViewGroup {
    protected final float designWidth;
    private final float designHeight;
    /** Inicializa DesignArtLayout com os dados e as dependências recebidos. */
    public DesignArtLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DesignArtLayout);
        designWidth = a.getFloat(R.styleable.DesignArtLayout_designWidth, 364);
        designHeight = a.getFloat(R.styleable.DesignArtLayout_designHeight, 364);
        a.recycle();
        TypedArray clipping=context.obtainStyledAttributes(attrs,new int[]{android.R.attr.clipChildren,android.R.attr.clipToPadding});
        setClipChildren(clipping.getBoolean(0,false));
        setClipToPadding(clipping.getBoolean(1,false));
        clipping.recycle();
    }
    /** Desenha o componente respeitando sua apresentação e seus limites visuais. */
    @Override protected void dispatchDraw(android.graphics.Canvas canvas) {
        int state=canvas.save();
        if(getClipToOutline()&&getBackground()!=null) {
            android.graphics.Outline outline=new android.graphics.Outline();getBackground().getOutline(outline);
            float radius=outline.getRadius();
            // GradientDrawable usa um contorno de caminho para cantos definidos individualmente.
            // O avatar original da assistente é circular mesmo quando o contorno não possui raio.
            if (radius < 0 && String.valueOf(getTag()).contains("Avatar da Lô")) {
                radius = Math.min(getWidth(), getHeight()) / 2f;
            }
            if(radius>=0) {
                android.graphics.Path clip=new android.graphics.Path();
                radius=Math.min(radius,Math.min(getWidth(),getHeight())/2f);
                clip.addRoundRect(0,0,getWidth(),getHeight(),radius,radius,android.graphics.Path.Direction.CW);
                canvas.clipPath(clip);
            }
        }
        super.dispatchDraw(canvas);canvas.restoreToCount(state);
    }
    /** Calcula as dimensões do componente de acordo com o espaço disponível. */
    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int height = resolveSize(Math.round(width * designHeight / designWidth), heightSpec);
        float scale = width / designWidth;
        for (int i=0; i<getChildCount(); i++) {
            View child = getChildAt(i);
            ArtParams p = (ArtParams) child.getLayoutParams();
            child.measure(MeasureSpec.makeMeasureSpec(Math.round(p.w*scale), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(child instanceof android.widget.TextView||p.h<=0?0:Math.round(p.h*scale),
                            child instanceof android.widget.TextView||p.h<=0?MeasureSpec.UNSPECIFIED:MeasureSpec.EXACTLY));
        }
        setMeasuredDimension(width, height);
    }
    /** Posiciona os componentes filhos dentro dos limites do layout. */
    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        float scale = (right-left)/designWidth;
        for (int i=0;i<getChildCount();i++) {
            View child = getChildAt(i);
            ArtParams p = (ArtParams)child.getLayoutParams();
            int x = Math.round(p.x*scale), y = Math.round(p.y*scale);
            child.layout(x,y,x+child.getMeasuredWidth(),y+child.getMeasuredHeight());
        }
    }
    /** Cria os parâmetros usados para posicionar os filhos no layout. */
    @Override public LayoutParams generateLayoutParams(AttributeSet attrs) { return new ArtParams(getContext(),attrs); }
    /** Cria os parâmetros usados para posicionar os filhos no layout. */
    @Override protected LayoutParams generateDefaultLayoutParams() { return new ArtParams(); }
    /** Confere se os parâmetros recebidos são compatíveis com o layout. */
    @Override protected boolean checkLayoutParams(LayoutParams p) { return p instanceof ArtParams; }
    /** Representa ArtParams, responsável por a composição e a interação dos componentes visuais. */
    public static class ArtParams extends LayoutParams {
        float x,y,w,h;
        /** Inicializa ArtParams com os dados e as dependências recebidos. */
        ArtParams() { super(MATCH_PARENT,WRAP_CONTENT); w=364;h=364; }
        /** Inicializa ArtParams com os dados e as dependências recebidos. */
        ArtParams(Context c, AttributeSet attrs) {
            super(c,attrs);
            TypedArray a=c.obtainStyledAttributes(attrs,R.styleable.DesignArtLayout_Layout);
            x=a.getFloat(R.styleable.DesignArtLayout_Layout_designX,0);
            y=a.getFloat(R.styleable.DesignArtLayout_Layout_designY,0);
            w=a.getFloat(R.styleable.DesignArtLayout_Layout_designChildWidth,364);
            h=a.getFloat(R.styleable.DesignArtLayout_Layout_designChildHeight,364);
            a.recycle();
        }
    }
}
