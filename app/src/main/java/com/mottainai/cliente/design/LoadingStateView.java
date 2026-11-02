package com.mottainai.cliente.design;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.mottainai.cliente.R;

/** Apresenta o carregamento com indicador nativo e espaços reservados sem simular dados reais. */
public final class LoadingStateView extends LinearLayout {
    /** Cria um estado compacto ou uma prévia dos cartões aguardando a primeira resposta. */
    public LoadingStateView(Context context, String message, boolean showPlaceholders) {
        super(context);
        setOrientation(VERTICAL);
        setPadding(dp(4), dp(16), dp(4), dp(16));
        setContentDescription(message);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);
        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        ProgressBar progress = new ProgressBar(context);
        progress.setIndeterminateTintList(ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.design_primary)));
        header.addView(progress, new LayoutParams(dp(24), dp(24)));
        TextView label = ClientContent.label(context, message, 14);
        LayoutParams textParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1);
        textParams.setMarginStart(dp(12));
        header.addView(label, textParams);
        addView(header, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        if (showPlaceholders) {
            for (int index = 0; index < 3; index++) addPlaceholder();
        }
    }

    /** Reserva o espaço visual de um cartão enquanto a lista ainda não possui dados. */
    private void addPlaceholder() {
        LinearLayout card = new LinearLayout(getContext());
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        GradientDrawable background = new GradientDrawable();
        background.setColor(ContextCompat.getColor(getContext(), R.color.surface_card));
        background.setCornerRadius(dp(16));
        card.setBackground(background);
        card.addView(block(), new LayoutParams(dp(72), dp(72)));
        LinearLayout lines = new LinearLayout(getContext());
        lines.setOrientation(VERTICAL);
        LayoutParams linesParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1);
        linesParams.setMarginStart(dp(16));
        card.addView(lines, linesParams);
        for (int index = 0; index < 3; index++) {
            LayoutParams lineParams = new LayoutParams(LayoutParams.MATCH_PARENT, dp(index == 2 ? 18 : 12));
            lineParams.topMargin = index == 0 ? 0 : dp(10);
            lineParams.setMarginEnd(dp(index == 0 ? 12 : index == 1 ? 40 : 72));
            lines.addView(block(), lineParams);
        }
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(12);
        addView(card, params);
    }

    /** Cria uma área neutra arredondada usando a cor de texto do tema atual. */
    private View block() {
        View view = new View(getContext());
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(
                ContextCompat.getColor(getContext(), R.color.design_text), 22));
        shape.setCornerRadius(dp(8));
        view.setBackground(shape);
        return view;
    }

    /** Converte a medida visual para a densidade do aparelho. */
    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
