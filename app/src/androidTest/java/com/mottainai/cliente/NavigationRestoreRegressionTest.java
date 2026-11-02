package com.mottainai.cliente;

import android.content.Context;
import android.content.Intent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/** Verifica a restauração da navegação sem exigir conta real ou serviços externos. */
@RunWith(AndroidJUnit4.class)
public class NavigationRestoreRegressionTest {
    /** Mantém a tela de lojas após recriar a atividade, como ocorre em mudanças de configuração. */
    @Test public void storesScreenSurvivesActivityRecreation() {
        try (ActivityScenario<MainActivity> scenario = launchStores()) {
            idle();
            scenario.recreate();
            idle();
            scenario.onActivity(activity -> assertEquals("2005:4305", activity.currentScreen().getTag()));
        }
    }

    /** Restaura o painel de raio e retorna à mesma tela de lojas ao fechá-lo. */
    @Test public void radiusPanelRestoresItsUnderlyingStoresScreen() {
        try (ActivityScenario<MainActivity> scenario = launchStores()) {
            idle();
            scenario.onActivity(activity -> activity.showScreen("2143:10256", true));
            idle();
            scenario.recreate();
            idle();
            scenario.onActivity(activity -> {
                assertEquals("2143:10256", activity.currentScreen().getTag());
                assertNotNull(activity.getSupportFragmentManager().findFragmentById(R.id.design_content));
                assertEquals("2005:4305", activity.getSupportFragmentManager()
                        .findFragmentById(R.id.design_content).getTag());
                activity.getSupportFragmentManager().popBackStackImmediate();
                assertEquals("2005:4305", activity.currentScreen().getTag());
            });
        }
    }

    /** Abre a prévia de depuração para verificar navegação sem liberar acesso real aos serviços. */
    private ActivityScenario<MainActivity> launchStores() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        return ActivityScenario.launch(new Intent(context, MainActivity.class)
                .putExtra("design_preview", true).putExtra("screen", "2005:4305"));
    }

    /** Aguarda a fila da interface concluir a criação e a troca dos fragmentos. */
    private void idle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
}
