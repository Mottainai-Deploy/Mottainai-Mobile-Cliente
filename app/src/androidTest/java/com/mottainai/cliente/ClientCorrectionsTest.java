package com.mottainai.cliente;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.mottainai.cliente.design.ClientContent;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ClientCorrectionsTest {
    private ActivityScenario<MainActivity> launch(String screen,boolean preview) {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        c.getSharedPreferences("cliente_preferences",Context.MODE_PRIVATE).edit().clear().putInt("theme",2).commit();
        return ActivityScenario.launch(new Intent(c,MainActivity.class).putExtra("screen",screen).putExtra("design_preview",preview));
    }
    private void idle(){InstrumentationRegistry.getInstrumentation().waitForIdleSync();}
    private View root(MainActivity a){return a.currentScreen().requireView();}
    @Test public void passwordVisibilityAndInlineErrorsWorkWithKeyboard() {
        try(ActivityScenario<MainActivity> scenario=launch("2028:4492",false)) {
            scenario.onActivity(a->{
                EditText email=field(root(a),"E-mail"),password=field(root(a),"Senha");
                assertNotNull(email);assertNotNull(password);assertEquals("",email.getText().toString());assertEquals("",password.getText().toString());
                email.setText("pessoa@exemplo.com");
                View login=action(root(a),"Entrar");assertNotNull(login);login.performClick();
                assertNull("No native popup hiding the password",password.getError());
                TextView error=((ViewGroup)password.getParent().getParent()).findViewById(R.id.design_field_error);
                assertEquals(View.VISIBLE,error.getVisibility());assertEquals("Preencha este campo",error.getText().toString());
                password.setText("senha-segura");password.setSelection(4);
                View eye=description(root(a),"Mostrar senha");assertNotNull(eye);eye.performClick();
                assertNull(password.getTransformationMethod());assertEquals(4,password.getSelectionStart());
                description(root(a),"Ocultar senha").performClick();assertNotNull(password.getTransformationMethod());
                assertEquals(View.GONE,error.getVisibility());
                password.requestFocus();((android.view.inputmethod.InputMethodManager)a.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(password,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            });
            SystemClock.sleep(900);idle();
            scenario.onActivity(a->{
                EditText password=field(root(a),"Senha");
                android.graphics.Rect visible=new android.graphics.Rect();a.getWindow().getDecorView().getWindowVisibleDisplayFrame(visible);
                int[] xy=new int[2];password.getLocationOnScreen(xy);
                assertTrue("Focused password stays above the IME",xy[1]+password.getHeight()<=visible.bottom+2);
            });
        }
    }
    @Test public void everyPasswordFieldHasIndependentVisibilityControl() {
        try(ActivityScenario<MainActivity> scenario=launch("2028:4279",true)) {
            scenario.onActivity(a->{
                assertEquals(3,countDescription(root(a),"Mostrar senha"));
                EditText current=field(root(a),"Senha atual"),fresh=field(root(a),"Nova senha");
                current.setText("senha-atual");fresh.setText("senha-nova");
                description(root(a),"Mostrar senha").performClick();
                assertNull(current.getTransformationMethod());assertNotNull(fresh.getTransformationMethod());
            });
        }
    }
    @Test public void consentShowsNativeCheckAndPersists() {
        try(ActivityScenario<MainActivity> scenario=launch("2650:73471",true)) {
            scenario.onActivity(a->{View option=ClientContent.find(root(a),"Conta / Consentimento de marketing");assertNotNull(option);assertFalse(a.getSharedPreferences("cliente_preferences",Context.MODE_PRIVATE).getBoolean("marketing",false));assertTrue("Consent action must be clickable",option.performClick());assertTrue("Consent preference must change",a.getSharedPreferences("cliente_preferences",Context.MODE_PRIVATE).getBoolean("marketing",false));CheckBox check=findCheck(option);assertNotNull(check);assertTrue("Native check must reflect preference",check.isChecked());assertNotNull(check.getButtonDrawable());});
            scenario.recreate();idle();
            scenario.onActivity(a->{CheckBox check=findCheck(root(a));assertNotNull(check);assertTrue(check.isChecked());});
        }
    }
    @Test public void panelsRetainScreenAndBackRestoresIt() {
        try(ActivityScenario<MainActivity> scenario=launch("2005:4483",true)) {
            Fragment[] base={null};
            scenario.onActivity(a->{base[0]=a.currentScreen();a.showScreen("2571:30930",true);});idle();
            scenario.onActivity(a->{assertSame(base[0],a.getSupportFragmentManager().findFragmentById(R.id.design_content));assertEquals("2571:30930",a.currentScreen().getTag());assertTrue(base[0].requireView().isShown());a.getOnBackPressedDispatcher().onBackPressed();});idle();
            scenario.onActivity(a->assertSame(base[0],a.currentScreen()));
        }
    }
    @Test public void selectedRadiusNeverChangesTheFourOptions() {
        try(ActivityScenario<MainActivity> scenario=launch("2005:3812",true)) {
            scenario.onActivity(a->{a.getSharedPreferences("cliente_preferences",Context.MODE_PRIVATE).edit().putString("radius","15 km").commit();a.showScreen("2143:10256",true);});idle();
            scenario.onActivity(a->{Set<String> options=new HashSet<>();for(String option:new String[]{"5 km","10 km","15 km","20 km"}){View v=action(root(a),option);assertNotNull(option,v);assertTrue(contains(v,option));options.add(option);}assertEquals(4,options.size());});
        }
    }
    @Test public void nonSquarePhotoRemainsCircularInEveryPlacement() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();File file=new File(context.getFilesDir(),"profile-photo.jpg");
        Bitmap source=Bitmap.createBitmap(600,120,Bitmap.Config.ARGB_8888);source.eraseColor(Color.GREEN);
        try(FileOutputStream out=new FileOutputStream(file)){source.compress(Bitmap.CompressFormat.JPEG,95,out);}source.recycle();
        try(ActivityScenario<MainActivity> scenario=launch("2005:4483",true)) {
            for(String id:new String[]{"2005:4483","2022:4219","2571:30930"}) {
                scenario.onActivity(a->a.showScreen(id,true));idle();
                scenario.onActivity(a->{View photo=ClientContent.find(root(a),"Ellipse");assertNotNull(photo);assertEquals(photo.getWidth(),photo.getHeight());assertTrue(photo.getWidth()>0);Bitmap bitmap=Bitmap.createBitmap(photo.getWidth(),photo.getHeight(),Bitmap.Config.ARGB_8888);photo.draw(new Canvas(bitmap));assertEquals(0,Color.alpha(bitmap.getPixel(0,0)));int center=bitmap.getPixel(bitmap.getWidth()/2,bitmap.getHeight()/2);assertTrue(Color.green(center)>245&&Color.red(center)<10&&Color.blue(center)<10);bitmap.recycle();});
            }
        }finally{file.delete();}
    }
    @Test public void productionDoesNotPublishFigmaOffersOrBalances() {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        new com.mottainai.cliente.utils.SessionManager(c).clearClientSession();
        try(ActivityScenario<MainActivity> scenario=launch("2005:3812",false)) {
            scenario.onActivity(a->{assertEquals("2028:4492",a.currentScreen().getTag());assertFalse(visibleContains(root(a),"R$ 12,90"));assertFalse(visibleContains(root(a),"Cesta hortifruti"));assertFalse(visibleContains(root(a),"320 pontos"));});
        }
    }
    @Test public void newChatRemovesIntroductionAndUsesContentSizedBubbles() {
        try(ActivityScenario<MainActivity> scenario=launch("2055:5379",true)) {
            scenario.onActivity(a->{EditText composer=field(root(a),"Mensagem");assertNotNull(composer);composer.setText("oi");composer.onEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_SEND);});idle();
            scenario.onActivity(a->{View user=root(a).findViewById(R.id.design_chat_user);assertNotNull(user);assertTrue(user.getWidth()<((View)user.getParent()).getWidth()/2);assertFalse(visibleContains(root(a),"Vamos encontrar algo bom?"));assertNotNull(root(a).findViewById(R.id.design_chat_reply));});
        }
    }
    @Test public void chatHistoryStaysAboveMessagesAndIdentifiesBothSpeakers() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        File folder=new File(context.getExternalFilesDir(null),"chat-header-review");assertTrue(folder.isDirectory()||folder.mkdirs());
        for(boolean dark:new boolean[]{false,true})try(ActivityScenario<MainActivity> scenario=launch("2055:5379",true)) {
            scenario.onActivity(a->a.setAppearance(dark));idle();SystemClock.sleep(500);
            scenario.onActivity(a->{
                View history=action(root(a),"Minhas conversas");assertNotNull(history);
                View header=ClientContent.find(root(a),"Cabeçalho");assertNotNull(header);
                android.graphics.Rect historyBounds=new android.graphics.Rect(),headerBounds=new android.graphics.Rect();
                history.getGlobalVisibleRect(historyBounds);header.getGlobalVisibleRect(headerBounds);
                assertTrue("History belongs in the fixed header",headerBounds.contains(historyBounds));
                EditText composer=field(root(a),"Mensagem");composer.setText("oi");composer.onEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_SEND);
            });idle();
            scenario.onActivity(a->{
                View user=root(a).findViewById(R.id.design_chat_user),assistant=root(a).findViewById(R.id.design_chat_reply);
                assertNotNull(user);assertNotNull(assistant);
                assertTrue("User message identifies its author",hasExactText((View)user.getParent(),"Você"));
                assertTrue("Assistant message identifies its author",hasExactText((View)assistant.getParent(),"Lô"));
            });idle();SystemClock.sleep(1000);
            scenario.onActivity(a->assertTrue(a.getWindow().getDecorView().hasWindowFocus()));
            Bitmap capture=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(capture);
            try(FileOutputStream out=new FileOutputStream(new File(folder,dark?"chat-dark.png":"chat-light.png"))){capture.compress(Bitmap.CompressFormat.PNG,100,out);}finally{capture.recycle();}
            scenario.onActivity(a->{View history=action(root(a),"Minhas conversas");assertNotNull(history);history.performClick();});idle();
            scenario.onActivity(a->assertEquals("2557:30504",a.currentScreen().getTag()));
        }
        for(String command:new String[]{"mkdir -p /data/local/tmp/mottainai-chat-header","cp -r "+folder.getAbsolutePath()+"/. /data/local/tmp/mottainai-chat-header/"})try(java.io.InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command))){while(in.read()!=-1){}}
    }
    @Test public void pointValuesAlignAtTheTrailingEdge() {
        try(ActivityScenario<MainActivity> scenario=launch("2093:6456",true)) {
            scenario.onActivity(a->{View row=ClientContent.find(root(a),"Registro / Mercado Verde");assertNotNull(row);ViewGroup group=(ViewGroup)row;View points=group.getChildAt(group.getChildCount()-1);assertTrue(points instanceof TextView);assertEquals(row.getWidth()-row.getPaddingRight(),points.getRight());assertEquals(android.view.Gravity.END|android.view.Gravity.CENTER_VERTICAL,((TextView)points).getGravity());});
        }
    }
    @Test public void mapsOpensTheSelectedPartnerInGoogleMaps() {
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        android.content.IntentFilter filter=new android.content.IntentFilter(Intent.ACTION_VIEW);filter.addDataScheme("https");
        android.app.Instrumentation.ActivityMonitor monitor=instrumentation.addMonitor(filter,new android.app.Instrumentation.ActivityResult(android.app.Activity.RESULT_CANCELED,null),true);
        try(ActivityScenario<MainActivity> scenario=launch("2005:4305",true)) {
            scenario.onActivity(a->com.mottainai.cliente.utils.MapNavigation.openRoute(a,new com.mottainai.cliente.models.PartnerStore(com.mottainai.cliente.repository.DemoData.stores().get(0))));
            assertEquals(1,monitor.getHits());
        }finally{instrumentation.removeMonitor(monitor);}
    }
    @Test public void notificationSettingsRequireAnAuthenticatedProfile() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        com.mottainai.cliente.utils.SessionManager session=new com.mottainai.cliente.utils.SessionManager(context);
        session.clearSession();session.enterDemoMode();
        try(ActivityScenario<MainActivity> scenario=launch("2557:30760",false)) {
            scenario.onActivity(a->assertEquals("2028:4492",a.currentScreen().getTag()));
        }finally{session.clearClientSession();}
    }
    @Test public void captureCorrectedPreviewScreensInBothThemes() throws Exception {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        File folder=new File(c.getExternalFilesDir(null),"phone-corrections");assertTrue(folder.isDirectory()||folder.mkdirs());
        for(boolean dark:new boolean[]{false,true}) {
            c.getSharedPreferences("cliente_preferences",Context.MODE_PRIVATE).edit().clear().putInt("theme",dark?2:1).commit();
            try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(new Intent(c,MainActivity.class).putExtra("screen","2028:4492").putExtra("design_preview",true))) {
                for(String screen:new String[]{"2028:4492","2005:3812","2005:3914","2005:4305","2005:4483","2028:4409","2650:73471","2028:4279","2143:11079","2571:30930","2680:78485","2557:31016","2565:30879","2143:10256","2143:10917","2557:30504"}) {
                    scenario.onActivity(a->{if(screen.equals("2143:10256"))a.getSharedPreferences("cliente_preferences",Context.MODE_PRIVATE).edit().putString("radius","15 km").commit();a.showScreen(screen,false);});idle();SystemClock.sleep(1000);
                    if(screen.equals("2650:73471")){scenario.onActivity(a->ClientContent.find(root(a),"Conta / Consentimento de marketing").performClick());idle();}
                    if(screen.equals("2005:4305"))SystemClock.sleep(8000);
                    scenario.onActivity(a->assertTrue("Refuse captures covered by another system window",a.getWindow().getDecorView().hasWindowFocus()));
                    Bitmap bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
                    assertNotNull("System screenshot",bitmap);
                    try(FileOutputStream out=new FileOutputStream(new File(folder,screen.replace(':','_')+(dark?"_dark":"_light")+".png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}finally{bitmap.recycle();}
                }
            }
        }
        for(String command:new String[]{"mkdir -p /data/local/tmp/mottainai-phone-corrections","cp -r "+folder.getAbsolutePath()+"/. /data/local/tmp/mottainai-phone-corrections/"}) {
            try(java.io.InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command))){byte[] buffer=new byte[1024];while(in.read(buffer)!=-1){}}
        }
    }
    private EditText field(View view,String name){if(view instanceof EditText&&ClientContent.tag(view).optString("field").equals(name))return (EditText)view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){EditText f=field(((ViewGroup)view).getChildAt(i),name);if(f!=null)return f;}return null;}
    private View action(View view,String value){if(ClientContent.tag(view).optString("action").trim().equals(value)&&view.isClickable())return view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View v=action(((ViewGroup)view).getChildAt(i),value);if(v!=null)return v;}return null;}
    private View description(View view,String value){if(value.equals(String.valueOf(view.getContentDescription())))return view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View v=description(((ViewGroup)view).getChildAt(i),value);if(v!=null)return v;}return null;}
    private int countDescription(View v,String s){int n=s.equals(String.valueOf(v.getContentDescription()))?1:0;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)n+=countDescription(((ViewGroup)v).getChildAt(i),s);return n;}
    private CheckBox findCheck(View v){if(v instanceof CheckBox)return (CheckBox)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){CheckBox c=findCheck(((ViewGroup)v).getChildAt(i));if(c!=null)return c;}return null;}
    private boolean contains(View v,String text){if(v instanceof TextView&&((TextView)v).getText().toString().contains(text))return true;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)if(contains(((ViewGroup)v).getChildAt(i),text))return true;return false;}
    private boolean hasExactText(View v,String text){if(v instanceof TextView&&((TextView)v).getText().toString().equals(text))return true;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)if(hasExactText(((ViewGroup)v).getChildAt(i),text))return true;return false;}
    private boolean visibleContains(View v,String text){if(v.getVisibility()!=View.VISIBLE)return false;if(v instanceof TextView&&((TextView)v).getText().toString().contains(text))return true;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)if(visibleContains(((ViewGroup)v).getChildAt(i),text))return true;return false;}
}
