package com.mottainai.cliente.design;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.BitmapFactory;
import android.graphics.drawable.PictureDrawable;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatImageView;
import com.caverock.androidsvg.SVG;
import com.mottainai.cliente.R;
import java.io.InputStream;

/** Ilustração local original do Figma, preservando a geometria SVG e a proporção. */
public class DesignImageView extends AppCompatImageView {
    /** Desenha o componente respeitando sua apresentação e seus limites visuais. */
    @Override protected void onDraw(android.graphics.Canvas canvas) {
        int saved=canvas.save();
        if(String.valueOf(getTag()).contains("\"name\": \"Ellipse\"")) {
            android.graphics.Path circle=new android.graphics.Path();
            circle.addCircle(getWidth()/2f,getHeight()/2f,Math.min(getWidth(),getHeight())/2f,android.graphics.Path.Direction.CW);
            canvas.clipPath(circle);
        }
        super.onDraw(canvas);canvas.restoreToCount(saved);
    }
    /** Inicializa DesignImageView com os dados e as dependências recebidos. */
    public DesignImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray values = context.obtainStyledAttributes(attrs, R.styleable.DesignImageView);
        String asset = values.getString(R.styleable.DesignImageView_designAsset);
        values.recycle();
        if (asset == null) return;
        try (InputStream input = context.getAssets().open(asset)) {
            if (asset.endsWith(".svg")) {
                SVG svg = SVG.getFromInputStream(input);
                setImageDrawable(new PictureDrawable(svg.renderToPicture()));
                setLayerType(LAYER_TYPE_SOFTWARE, null);
            } else {
                setImageBitmap(BitmapFactory.decodeStream(input));
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot load design asset " + asset, exception);
        }
    }
}
