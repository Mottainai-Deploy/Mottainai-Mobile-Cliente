package com.mottainai.cliente.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

public final class LoginScrollView extends NestedScrollView {
    public LoginScrollView(Context context, AttributeSet attrs) { super(context, attrs); }
    private boolean keyboardVisible() {
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(this);
        return insets != null && insets.isVisible(WindowInsetsCompat.Type.ime());
    }
    @Override public boolean onInterceptTouchEvent(MotionEvent event) {
        return keyboardVisible() && super.onInterceptTouchEvent(event);
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        return keyboardVisible() && super.onTouchEvent(event);
    }
}
