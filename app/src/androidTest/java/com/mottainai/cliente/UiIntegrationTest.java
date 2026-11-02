package com.mottainai.cliente;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.mottainai.cliente.design.ClientContent;
import com.mottainai.cliente.utils.SessionManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Reúne os cenários de teste de UiIntegration. */
@RunWith(AndroidJUnit4.class)
public class UiIntegrationTest {
    /** Obtém o contexto Android utilizado pelo teste. */
    private Context context(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
    /** Verifica a criação dos cartões visuais nos temas claro e escuro. */
    @Test public void figmaCardsInflateInLightAndDarkModes() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            for(int mode:new int[]{Configuration.UI_MODE_NIGHT_NO,Configuration.UI_MODE_NIGHT_YES}) {
                Configuration config=new Configuration(context().getResources().getConfiguration());
                config.uiMode=(config.uiMode&~Configuration.UI_MODE_NIGHT_MASK)|mode;
                Context themed=new ContextThemeWrapper(context().createConfigurationContext(config),R.style.Theme_MottainaiCliente);
                View home=LayoutInflater.from(themed).inflate(R.layout.fragment_home,null,false);
                assertNotNull(ClientContent.find(home,"Ofertas / Lista compacta"));
                View card=LayoutInflater.from(themed).inflate(R.layout.design_item_offer_compact,null,false);
                assertNotNull(binding(card,"title"));assertNotNull(binding(card,"price"));
                View grid=LayoutInflater.from(themed).inflate(R.layout.design_item_offer_grid,null,false);
                assertNotNull(binding(grid,"title"));assertNotNull(binding(grid,"discount"));
                View store=LayoutInflater.from(themed).inflate(R.layout.design_item_store,null,false);
                assertNotNull(binding(store,"store"));assertNotNull(binding(store,"address"));
            }
        });
    }
    /** Verifica se as telas protegidas exigem autenticação e perfil sincronizado. */
    @Test public void protectedFigmaRoutesCannotBypassFirebaseAndSqlSession() {
        new SessionManager(context()).clearClientSession();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(new Intent(context(),MainActivity.class).putExtra("screen","2005:3914"))) {
            scenario.onActivity(a->assertEquals("2028:4492",a.currentScreen().getArguments().getString("screen")));
            for(String screen:new String[]{"2005:4305","2093:6331","2005:4483","2005:4104"}) {
                scenario.onActivity(a->a.showScreen(screen,true));
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                scenario.onActivity(a->assertEquals("2028:4492",a.currentScreen().getArguments().getString("screen")));
            }
        }
    }
    /** Verifica a validação entre etapas e a ausência de persistência indevida dos campos pessoais. */
    @Test public void registrationValidatesBeforeChangingStepsAndDoesNotPersistPersonalFields() {
        new SessionManager(context()).clearClientSession();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(new Intent(context(),MainActivity.class).putExtra("screen","2143:9435"))) {
            scenario.onActivity(a->{
                View root=a.currentScreen().requireView();
                EditText name=field(root,"Nome completo"),cpf=field(root,"CPF");
                assertNotNull(name);assertNotNull(cpf);
                name.setText("Pessoa Teste");cpf.setText("11111111111");
                assertEquals("111.111.111-11",cpf.getText().toString());
                action(root,"Continuar").performClick();
                assertEquals("2143:9435",a.currentScreen().getArguments().getString("screen"));
                for(String key:a.getSharedPreferences("cliente_preferences",0).getAll().keySet())assertFalse(key,key.startsWith("field_"));
            });
        }
    }
    /** Verifica se a marcação de demonstração não libera as telas protegidas. */
    @Test public void demoFlagDoesNotUnlockProtectedScreens() {
        SessionManager session=new SessionManager(context());session.clearSession();session.enterDemoMode();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(new Intent(context(),MainActivity.class).putExtra("screen","2005:3812"))) {
            scenario.onActivity(a->assertEquals("2028:4492",a.currentScreen().getArguments().getString("screen")));
        } finally {session.clearClientSession();}
    }
    /** Localiza o componente associado ao vínculo de dados verificado. */
    private View binding(View v,String key){if(ClientContent.tag(v).optString("binding").equals(key))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View child=binding(((ViewGroup)v).getChildAt(i),key);if(child!=null)return child;}return null;}
    /** Localiza o campo identificado no cenário de teste. */
    private EditText field(View v,String key){if(v instanceof EditText&&ClientContent.tag(v).optString("field").equals(key))return (EditText)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){EditText child=field(((ViewGroup)v).getChildAt(i),key);if(child!=null)return child;}return null;}
    /** Obtém a ação de navegação ou interação associada ao componente. */
    private View action(View v,String key){if(ClientContent.tag(v).optString("action").trim().equals(key))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View child=action(((ViewGroup)v).getChildAt(i),key);if(child!=null)return child;}return null;}
}
