package com.mottainai.cliente.utils;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.credentials.GetCredentialRequest;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.mottainai.cliente.MainActivity;
import com.mottainai.cliente.R;
import com.mottainai.cliente.design.ClientContent;
import com.mottainai.cliente.viewmodel.AuthViewModel;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Reúne os cenários de teste de GoogleButtonRegression. */
@RunWith(AndroidJUnit4.class)
public class GoogleButtonRegressionTest {
    /** Verifica se o botão Google solicita somente a credencial prevista para essa ação. */
    @Test public void explicitButtonUsesOnlyTheGoogleButtonCredentialOption() {
        GetCredentialRequest request=GoogleSignInHelper.buttonRequest("test.apps.googleusercontent.com");
        assertEquals(1,request.getCredentialOptions().size());
        assertTrue(request.getCredentialOptions().get(0) instanceof GetSignInWithGoogleOption);
    }
    /** Verifica o alinhamento central do texto do botão Google nos dois temas. */
    @Test public void googleLabelIsCenteredInBothThemes() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            Context base=InstrumentationRegistry.getInstrumentation().getTargetContext();
            for(int night:new int[]{Configuration.UI_MODE_NIGHT_NO,Configuration.UI_MODE_NIGHT_YES}) {
                Configuration config=new Configuration(base.getResources().getConfiguration());
                config.uiMode=(config.uiMode&~Configuration.UI_MODE_NIGHT_MASK)|night;
                Context context=new ContextThemeWrapper(base.createConfigurationContext(config),R.style.Theme_MottainaiCliente);
                View root=LayoutInflater.from(context).inflate(R.layout.fragment_login,null,false);
                ViewGroup button=(ViewGroup)ClientContent.find(root,"Acesso / Continuar com Google");
                int width=Math.round(320*base.getResources().getDisplayMetrics().density);
                button.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.AT_MOST));
                button.layout(0,0,width,button.getMeasuredHeight());
                TextView label=(TextView)button.getChildAt(1);
                assertEquals(android.view.Gravity.CENTER,label.getGravity());
                assertEquals(width/2f,label.getLeft()+label.getWidth()/2f,1f);
            }
        });
    }
    /** Verifica a posição do carregamento e do erro do Google e a possibilidade de tentar novamente. */
    @Test public void googleProgressAndFailureAppearBeforeTheButtonsAndAllowRetry() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(new Intent(context,MainActivity.class).putExtra("screen","2028:4492"))) {
            scenario.onActivity(activity->{
                androidx.fragment.app.Fragment fragment=activity.currentScreen();
                View root=fragment.requireView();
                ViewGroup actions=(ViewGroup)ClientContent.find(root,"Entrar / Ações");
                View google=ClientContent.find(root,"Acesso / Continuar com Google");
                AuthViewModel model=new ViewModelProvider(fragment).get(AuthViewModel.class);
                assertTrue(model.beginGoogleSignIn());
                assertFalse(google.isEnabled());
                ViewGroup parent=(ViewGroup)actions.getParent();
                View progress=parent.getChildAt(parent.indexOfChild(actions)-1);
                assertTrue(progress instanceof TextView);
                assertTrue(((TextView)progress).getText().toString().contains("Abrindo o Google"));
                model.googleSignInFailed("Falha de teste visível");
                assertEquals("Falha de teste visível",((TextView)progress).getText().toString());
                assertTrue(google.isEnabled());
            });
        }
    }
}
