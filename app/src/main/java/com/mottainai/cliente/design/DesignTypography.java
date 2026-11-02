package com.mottainai.cliente.design;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.json.JSONObject;

/** Aplica os eixos de fonte variável definidos pelos estilos de texto originais do Figma. */
public final class DesignTypography {
    /** Impede a criação de instâncias desta classe utilitária. */
    private DesignTypography() {}
    /** Aplica a configuração visual ou de edição prevista pelo componente. */
    public static void apply(View root) {
        if(root instanceof TextView) {
            try {
                JSONObject tag=new JSONObject(String.valueOf(root.getTag()));
                if(tag.has("fontWeight")) {
                    String axes="'wght' "+tag.optInt("fontWeight",400);
                    if(tag.has("fontOpticalSize"))axes+=", 'opsz' "+tag.optInt("fontOpticalSize",14);
                    ((TextView)root).setFontVariationSettings(axes);
                }
            } catch(org.json.JSONException ignored) {}
        }
        if(root instanceof ViewGroup) {
            ViewGroup group=(ViewGroup)root;
            for(int i=0;i<group.getChildCount();i++)apply(group.getChildAt(i));
        }
    }
}
