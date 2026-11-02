package com.mottainai.cliente;

import android.content.Context;
import android.content.Intent;
import android.view.MotionEvent;
import android.view.View;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.mottainai.cliente.utils.SessionManager;
import com.mottainai.cliente.widgets.LoginScrollView;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Reúne os cenários de teste de LoginLaunchRegression. */
@RunWith(AndroidJUnit4.class)
public class LoginLaunchRegressionTest {
    /** Verifica se a abertura sem sessão apresenta a splash antes da introdução. */
    @Test public void launchWithoutSessionAlwaysShowsSplashBeforeOnboarding() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        new SessionManager(context).clearSession();
        context.getSharedPreferences("cliente_preferences",Context.MODE_PRIVATE).edit().putBoolean("introduction_seen",true).commit();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(new Intent(context,MainActivity.class))) {
            scenario.onActivity(activity->assertEquals("2244:11739",activity.currentScreen().getArguments().getString("screen")));
            Thread.sleep(1700);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity->assertEquals("2542:29599",activity.currentScreen().getArguments().getString("screen")));
        }
    }
    /** Verifica se o login permanece fixo com o teclado fechado. */
    @Test public void loginDoesNotScrollOnTouchWhenKeyboardIsClosed() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(new Intent(context,MainActivity.class).putExtra("screen","2028:4492"))) {
            scenario.onActivity(activity->{
                View scroll=activity.currentScreen().requireView().findViewById(R.id.design_scroll);
                assertTrue(scroll instanceof LoginScrollView);
                MotionEvent down=MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN,20,20,0);
                try { assertFalse(scroll.onTouchEvent(down)); } finally { down.recycle(); }
            });
        }
    }
}
