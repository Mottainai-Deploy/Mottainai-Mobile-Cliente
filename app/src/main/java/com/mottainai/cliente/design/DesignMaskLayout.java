package com.mottainai.cliente.design;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import com.caverock.androidsvg.SVG;
import com.mottainai.cliente.R;
import java.io.InputStream;

public class DesignMaskLayout extends DesignArtLayout {
    private final Picture mask;
    private final float maskX,maskY,maskWidth,maskHeight;
    private final Paint maskPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    public DesignMaskLayout(Context context,AttributeSet attrs) {
        super(context,attrs);
        TypedArray a=context.obtainStyledAttributes(attrs,R.styleable.DesignMaskLayout);
        String asset=a.getString(R.styleable.DesignMaskLayout_designMask);
        maskX=a.getFloat(R.styleable.DesignMaskLayout_maskX,0);
        maskY=a.getFloat(R.styleable.DesignMaskLayout_maskY,0);
        maskWidth=a.getFloat(R.styleable.DesignMaskLayout_maskWidth,20);
        maskHeight=a.getFloat(R.styleable.DesignMaskLayout_maskHeight,20);
        a.recycle();
        try(InputStream in=context.getAssets().open(asset)) {
            mask=SVG.getFromInputStream(in).renderToPicture();
        } catch(Exception e) {throw new IllegalStateException("Cannot load artwork mask "+asset,e);}
        maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        setLayerType(LAYER_TYPE_SOFTWARE,null);
    }
    @Override protected void dispatchDraw(Canvas canvas) {
        int layer=canvas.saveLayer(0,0,getWidth(),getHeight(),null);
        super.dispatchDraw(canvas);
        int maskLayer=canvas.saveLayer(0,0,getWidth(),getHeight(),maskPaint);
        float scale=getWidth()/designWidth;
        canvas.translate(maskX*scale,maskY*scale);
        canvas.scale(maskWidth*scale/mask.getWidth(),maskHeight*scale/mask.getHeight());
        canvas.drawPicture(mask);
        canvas.restoreToCount(maskLayer);
        canvas.restoreToCount(layer);
    }
}
