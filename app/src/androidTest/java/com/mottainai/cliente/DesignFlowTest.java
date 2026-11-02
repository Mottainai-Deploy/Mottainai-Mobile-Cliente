package com.mottainai.cliente;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Verifica os toques e as transições reais das telas, incluindo validação e persistência local. */
@RunWith(AndroidJUnit4.class)
public class DesignFlowTest {
    /** Abre a atividade usada pelo cenário de teste. */
    private ActivityScenario<MainActivity> launch(String screen) {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("cliente_preferences",Context.MODE_PRIVATE).edit().clear().putInt("theme",1).commit();
        return ActivityScenario.launch(new Intent(context,MainActivity.class).putExtra("screen",screen).putExtra("design_preview",true));
    }
    /** Verifica a apresentação inicial, o login, a navegação e a saída da conta. */
    @Test public void introductionLoginNavigationAndLogout() throws Exception {
        try(ActivityScenario<MainActivity> scenario=launch("2542:29599")) {
            click(scenario,"Continuar",false);expect(scenario,"2542:29639");
            click(scenario,"Continuar",false);expect(scenario,"2542:29679");
            click(scenario,"Começar",false);expect(scenario,"2028:4492");
            scenario.onActivity(a->fill(a.currentScreen().requireView(),"senha-local"));
            click(scenario,"Entrar",false);expect(scenario,"2005:3812");
            String[] labels={"Ofertas","Lojas","Pontos","Perfil"};
            String[] ids={"2005:3914","2005:4305","2093:6331","2005:4483"};
            for(int i=0;i<labels.length;i++){click(scenario,labels[i],false);expect(scenario,ids[i]);}
            click(scenario,"Opção / Configurações da conta",true);expect(scenario,"2022:4219");
            click(scenario,"Opção / Nome e telefone",true);expect(scenario,"2393:19184");
            scenario.onActivity(a->setField(a.currentScreen().requireView(),"Nome completo","Pessoa de teste"));
            click(scenario,"Salvar dados",false);expect(scenario,"2393:19412");
            scenario.onActivity(a->assertEquals("Pessoa de teste",a.getSharedPreferences("cliente_preferences",Context.MODE_PRIVATE).getString("field_Nome completo",null)));
            click(scenario,"Voltar à minha conta",false);expect(scenario,"2022:4219");
            click(scenario,"Opção / Nome e telefone",true);expect(scenario,"2393:19184");
            scenario.onActivity(a->{EditText field=(EditText)find(a.currentScreen().requireView(),"field","Nome completo");assertNotNull(field);assertEquals("Pessoa de teste",field.getText().toString());});
            click(scenario,"Perfil",false);expect(scenario,"2005:4483");
            click(scenario,"Sair da conta",false);expect(scenario,"2028:4409");
            click(scenario,"Sair",false);expect(scenario,"2028:4492");
        }
    }
    /** Verifica as etapas de revisão e conclusão do cadastro. */
    @Test public void registrationReviewAndCompletion() throws Exception {
        try(ActivityScenario<MainActivity> scenario=launch("2028:4492")) {
            click(scenario,"Criar conta",false);expect(scenario,"2143:9435");
            scenario.onActivity(a->fill(a.currentScreen().requireView(),"senha-local"));
            click(scenario,"Continuar",false);expect(scenario,"2143:9538");
            scenario.onActivity(a->fill(a.currentScreen().requireView(),"endereco"));
            click(scenario,"Consultar CEP",false);expect(scenario,"2430:27754");
            scenario.onActivity(a->fill(a.currentScreen().requireView(),"endereco"));
            click(scenario,"Revisar cadastro",false);expect(scenario,"2143:9630");
            click(scenario,"Criar conta",false);expect(scenario,"2005:3812");
        }
    }
    /** Verifica as preferências, a busca e o fluxo da conversa. */
    @Test public void preferencesAndSearchAndConversation() throws Exception {
        try(ActivityScenario<MainActivity> scenario=launch("2022:4219")) {
            click(scenario,"Opção / Preferências de comunicação",true);expect(scenario,"2650:73471");
            click(scenario,"Conta / Consentimento de marketing",true);
            click(scenario,"Salvar preferências",false);expect(scenario,"2650:73585");
            scenario.onActivity(a->assertTrue(a.getSharedPreferences("cliente_preferences",Context.MODE_PRIVATE).getBoolean("marketing",false)));
            click(scenario,"Ofertas",false);expect(scenario,"2005:3914");
            click(scenario,"Buscar produtos",true);expect(scenario,"2005:4018");
            scenario.onActivity(a->{EditText field=(EditText)find(a.currentScreen().requireView(),"field","Busca");assertNotNull(field);field.setText("leite");field.onEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);});
            expect(scenario,"2019:4155");
            click(scenario,"Cliente / Botão flutuante da Lô",true);expect(scenario,"2055:5379");
            scenario.onActivity(a->{EditText field=(EditText)find(a.currentScreen().requireView(),"field","Mensagem");assertNotNull(field);field.setText("Quais são meus pontos?");field.onEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_SEND);});
            scenario.onActivity(a->assertTrue("Chat should display the response",contains(a.currentScreen().requireView(),"320 pontos")));
            click(scenario,"Minhas conversas",false);expect(scenario,"2557:30504");
        }
    }
    /** Verifica se a preferência de tema permanece após recriar a atividade. */
    @Test public void themeSurvivesRecreation() throws Exception {
        try(ActivityScenario<MainActivity> scenario=launch("2005:4483")) {
            click(scenario,"Escuro",false);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.recreate();expect(scenario,"2005:4483");
            scenario.onActivity(a->{assertEquals(2,a.getSharedPreferences("cliente_preferences",Context.MODE_PRIVATE).getInt("theme",0));assertEquals(android.content.res.Configuration.UI_MODE_NIGHT_YES,a.getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK);});
            click(scenario,"Claro",false);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        }
    }
    /** Verifica as tentativas de recuperação e o retorno dos painéis informativos. */
    @Test public void retriesRecoverAndInformationalPanelsReturn() throws Exception {
        try(ActivityScenario<MainActivity> scenario=launch("2416:25538")) {
            click(scenario,"Tentar buscar ofertas",false);expect(scenario,"2005:3812");
            scenario.onActivity(a->a.showScreen("2413:31472",false));expect(scenario,"2413:31472");
            click(scenario,"Tentar novamente",false);expect(scenario,"2005:3914");
            scenario.onActivity(a->a.showScreen("2093:7392",false));expect(scenario,"2093:7392");
            click(scenario,"Tentar novamente",false);expect(scenario,"2093:6331");
            click(scenario,"Compartilhar",false);expect(scenario,"2557:31016");
            click(scenario,"Voltar à jornada",false);expect(scenario,"2093:6331");
            scenario.onActivity(a->a.showScreen("2055:6213",false));expect(scenario,"2055:6213");
            click(scenario,"Conferir envio",false);expect(scenario,"2557:30632");
            scenario.onActivity(a->{View footer=find(a.currentScreen().requireView(),"any_name","Escrever mensagem");assertNotNull("Pending composer is present",footer);assertTrue("Pending draft is preserved",contains(footer,"Quais ofertas estão disponíveis?"));assertNull("Unconfirmed delivery must prevent duplicate sends",find(footer,"field","Mensagem"));});
            click(scenario,"Atualizar conversa",false);expect(scenario,"2055:5739");
            scenario.onActivity(a->assertNotNull("Composer resumes after checking delivery",find(a.currentScreen().requireView(),"field","Mensagem")));
        }
    }
    /** Verifica a continuidade após corrigir os campos e alterar o consentimento. */
    @Test public void correctedValidationAndAlternateConsentContinue() throws Exception {
        try(ActivityScenario<MainActivity> scenario=launch("2143:9896")) {
            scenario.onActivity(a->fill(a.currentScreen().requireView(),"senha-local"));
            click(scenario,"Continuar",false);expect(scenario,"2143:9538");
            scenario.onActivity(a->a.showScreen("2650:73471",false));expect(scenario,"2650:73471");
            click(scenario,"Consentimento / Opção documentada",true);
            scenario.onActivity(a->assertTrue(a.getSharedPreferences("cliente_preferences",Context.MODE_PRIVATE).getBoolean("marketing",false)));
            scenario.onActivity(a->a.showScreen("2055:5379",false));expect(scenario,"2055:5379");
            click(scenario,"Ofertas por perto",false);expect(scenario,"2055:5739");
        }
    }
    /** Verifica se fotos da câmera e da galeria passam pela confirmação. */
    @Test public void galleryAndCameraResultsRequirePhotoConfirmation() throws Exception {
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        Context context=instrumentation.getTargetContext();
        android.graphics.Bitmap sample=android.graphics.Bitmap.createBitmap(80,80,android.graphics.Bitmap.Config.ARGB_8888);
        sample.eraseColor(android.graphics.Color.GREEN);
        java.io.File input=new java.io.File(context.getFilesDir(),"flow-photo-input.png");
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(input)){sample.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}
        try(ActivityScenario<MainActivity> scenario=launch("2571:30930")) {
            android.content.IntentFilter filter=new android.content.IntentFilter(Intent.ACTION_GET_CONTENT);
            filter.addCategory(Intent.CATEGORY_OPENABLE);filter.addDataType("image/*");
            android.app.Instrumentation.ActivityMonitor gallery=instrumentation.addMonitor(filter,new android.app.Instrumentation.ActivityResult(android.app.Activity.RESULT_OK,new Intent().setData(android.net.Uri.fromFile(input))),true);
            try {
                click(scenario,"Escolher da galeria",false);expect(scenario,"2571:30962");
                assertTrue(new java.io.File(context.getFilesDir(),"profile-photo-pending.jpg").isFile());
                click(scenario,"Salvar foto",false);expect(scenario,"2022:4219");
                assertTrue(new java.io.File(context.getFilesDir(),"profile-photo.jpg").isFile());
                assertFalse(new java.io.File(context.getFilesDir(),"profile-photo-pending.jpg").exists());
            }finally{instrumentation.removeMonitor(gallery);}
            click(scenario,"Ellipse",true);expect(scenario,"2571:30930");
            click(scenario,"Tirar foto",false);expect(scenario,"2680:78485");
            click(scenario,"Permitir câmera",false);expect(scenario,"2680:78487");
            android.app.Instrumentation.ActivityMonitor camera=instrumentation.addMonitor(new android.content.IntentFilter(android.provider.MediaStore.ACTION_IMAGE_CAPTURE),new android.app.Instrumentation.ActivityResult(android.app.Activity.RESULT_OK,new Intent().putExtra("data",sample)),true);
            try{click(scenario,"Capturar foto",true);expect(scenario,"2571:30962");click(scenario,"Cancelar",false);assertTrue(new java.io.File(context.getFilesDir(),"profile-photo.jpg").isFile());assertFalse(new java.io.File(context.getFilesDir(),"profile-photo-pending.jpg").exists());}
            finally{instrumentation.removeMonitor(camera);}
        }finally {
            sample.recycle();input.delete();
            new java.io.File(context.getFilesDir(),"profile-photo.jpg").delete();
            new java.io.File(context.getFilesDir(),"profile-photo-pending.jpg").delete();
        }
    }
    /** Localiza e aciona o componente usado pelo cenário de teste. */
    private void click(ActivityScenario<MainActivity> scenario,String value,boolean name) {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        scenario.onActivity(a->{View view=find(a.currentScreen().requireView(),name?"name":"action",value);assertNotNull("Missing action: "+value,view);assertTrue(value,view.performClick());});
    }
    /** Confere se a tela ou o resultado esperado foi apresentado. */
    private void expect(ActivityScenario<MainActivity> scenario,String id) {
        String[] actual={null};long until=SystemClock.uptimeMillis()+6000;
        do {InstrumentationRegistry.getInstrumentation().waitForIdleSync();scenario.onActivity(a->{Fragment f=a.currentScreen();actual[0]=f==null?null:f.getTag();});if(id.equals(actual[0]))return;SystemClock.sleep(80);}while(SystemClock.uptimeMillis()<until);
        assertEquals(id,actual[0]);
    }
    /** Lê os metadados usados para identificar e conectar o componente da tela. */
    private JSONObject tag(View view) {try{return new JSONObject(String.valueOf(view.getTag()));}catch(Exception ignored){return new JSONObject();}}
    /** Localiza o elemento ou resultado correspondente aos critérios informados. */
    private View find(View root,String key,String value) {
        String candidate=tag(root).optString(key.equals("any_name")?"name":key).trim().replaceAll("\\s+"," ");
        if(candidate.equals(value)&&(key.equals("field")||key.equals("any_name")||root.isClickable()))return root;
        if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){View match=find(((ViewGroup)root).getChildAt(i),key,value);if(match!=null)return match;}
        return null;
    }
    /** Preenche os campos necessários ao cenário de teste. */
    private void fill(View view,String fallback) {
        if(view instanceof EditText){String label=tag(view).optString("field").toLowerCase(java.util.Locale.ROOT);((EditText)view).setText(label.contains("mail")?"pessoa@exemplo.com":label.contains("telefone")?"11900000000":label.contains("cep")?"01001000":label.contains("cpf")?"12345678909":label.contains("número")?"210":label.contains("nome")?"Pessoa de teste":fallback);}
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)fill(((ViewGroup)view).getChildAt(i),fallback);
    }
    /** Preenche o campo indicado com o valor informado. */
    private void setField(View root,String name,String value){EditText field=(EditText)find(root,"field",name);assertNotNull(name,field);field.setText(value);}
    /** Verifica a presença do conteúdo esperado na hierarquia da tela. */
    private boolean contains(View root,String value){if(root instanceof TextView&&((TextView)root).getText().toString().contains(value))return true;if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++)if(contains(((ViewGroup)root).getChildAt(i),value))return true;return false;}
}
