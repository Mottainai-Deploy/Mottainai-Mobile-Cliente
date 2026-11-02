package com.mottainai.cliente.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

/** Mantém o login estável enquanto permite alcançar campos com o teclado aberto. */
public final class LoginScrollView extends NestedScrollView {
    /** Inicializa LoginScrollView com os dados e as dependências recebidos. */
    public LoginScrollView(Context context, AttributeSet attrs) { super(context, attrs); }
    /** Verifica se o teclado está ocupando parte da área da tela. */
    private boolean keyboardVisible() {
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(this);
        return insets != null && insets.isVisible(WindowInsetsCompat.Type.ime());
    }
    /** Decide se o contêiner deve interceptar o toque para rolagem. */
    @Override public boolean onInterceptTouchEvent(MotionEvent event) {
        return keyboardVisible() && super.onInterceptTouchEvent(event);
    }
    /** Trata os gestos de toque conforme o estado da rolagem e do teclado. */
    @Override public boolean onTouchEvent(MotionEvent event) {
        return keyboardVisible() && super.onTouchEvent(event);
    }
}
