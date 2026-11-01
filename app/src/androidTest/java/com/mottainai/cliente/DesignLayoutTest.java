package com.mottainai.cliente;

import android.content.Context;
import android.content.res.Configuration;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.SystemClock;
import androidx.test.core.app.ActivityScenario;
import java.io.File;
import java.io.FileOutputStream;
import android.os.ParcelFileDescriptor;
import android.os.Build;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DesignLayoutTest {
    private final java.util.List<String> boundsErrors=new java.util.ArrayList<>();
    @Test public void capturePrincipalScreensForReview() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String[] ids={"2028:4492","2005:3812","2005:3914","2005:4305","2093:6331","2005:4483","2055:5379","2542:29599","2430:27754","2456:28768","2557:30504","2680:78487","2680:78485"};
        for(boolean dark:new boolean[]{false,true}) {
            context.getSharedPreferences("cliente_preferences",Context.MODE_PRIVATE).edit()
                    .putInt("theme",dark?2:1).commit();
            for(String id:ids) {
                Intent intent=new Intent(context,MainActivity.class).putExtra("screen",id).putExtra("design_preview",true);
                try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(intent)) {
                    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                    SystemClock.sleep(300);
                    final boolean[] focused={false};
                    for(int attempt=0;attempt<20&&!focused[0];attempt++) {
                        scenario.onActivity(activity->focused[0]=activity.getWindow().getDecorView().hasWindowFocus());
                        if(!focused[0])SystemClock.sleep(250);
                    }
                    assertTrue("Another window covers the app: "+id,focused[0]);
                    Bitmap bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
                    assertNotNull(id,bitmap);
                    if(Build.VERSION.SDK_INT>=31) {
                        android.app.UiAutomation automation=InstrumentationRegistry.getInstrumentation().getUiAutomation();
                        try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("mkdir -p /data/local/tmp/mottainai-review"))) {while(in.read()!=-1){}}
                        String file="/data/local/tmp/mottainai-review/"+id.replace(':','_')+(dark?"_dark":"_light")+".png";
                        ParcelFileDescriptor[] pipes=automation.executeShellCommandRw("dd of="+file);
                        try(java.io.OutputStream out=new ParcelFileDescriptor.AutoCloseOutputStream(pipes[1])) {
                            bitmap.compress(Bitmap.CompressFormat.PNG,100,out);
                        }
                        pipes[0].close();
                    }
                    bitmap.recycle();
                }
            }
        }
        context.getSharedPreferences("cliente_preferences",Context.MODE_PRIVATE).edit().putInt("theme",1).commit();
    }
    @Test public void screensInflateAtNarrowAndWideWidthsInBothThemes() throws Exception {
        Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String json;
        try(InputStream in=target.getAssets().open("design_screens.json")) {
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            byte[] buffer=new byte[8192];int count;
            while((count=in.read(buffer))!=-1)out.write(buffer,0,count);
            json=new String(out.toByteArray(),StandardCharsets.UTF_8);
        }
        JSONObject screens=new JSONObject(json);
        final Throwable[] failure={null};
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            try {
                for(int theme:new int[]{Configuration.UI_MODE_NIGHT_NO,Configuration.UI_MODE_NIGHT_YES}) {
                    Configuration configuration=new Configuration(target.getResources().getConfiguration());
                    configuration.uiMode=(configuration.uiMode&~Configuration.UI_MODE_NIGHT_MASK)|theme;
                    configuration.fontScale=1.3f;
                    Context context=new ContextThemeWrapper(target.createConfigurationContext(configuration),R.style.Theme_MottainaiCliente);
                    for(Iterator<String> keys=screens.keys();keys.hasNext();) {
                        String key=keys.next();JSONObject spec=screens.getJSONObject(key);
                        if(spec.has("canonical"))continue;
                        int layout=context.getResources().getIdentifier(spec.getString("layout"),"layout",context.getPackageName());
                        assertTrue(key,layout!=0);
                        for(int dp:new int[]{320,600}) {
                            FrameLayout parent=new FrameLayout(context);
                            View view=LayoutInflater.from(context).inflate(layout,parent,false);
                            com.mottainai.cliente.design.DesignTypography.apply(view);
                            parent.addView(view);
                            int width=(int)(dp*context.getResources().getDisplayMetrics().density);
                            int height=(int)(900*context.getResources().getDisplayMetrics().density);
                            parent.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));
                            parent.layout(0,0,width,height);
                            assertEquals(key,width,view.getMeasuredWidth());
                            assertTrue(key,view.getMeasuredHeight()>0);
                            assertHorizontalBounds(view,key);
                        }
                    }
                }
            } catch(Throwable error) {failure[0]=error;}
        });
        if(failure[0]!=null)throw new AssertionError("Native screen inflation failed",failure[0]);
        assertTrue(String.join("\n",boundsErrors),boundsErrors.isEmpty());
    }
    private void assertHorizontalBounds(View view,String screen) {
        if(!(view instanceof android.view.ViewGroup)||view instanceof com.mottainai.cliente.design.DesignArtLayout)return;
        android.view.ViewGroup group=(android.view.ViewGroup)view;
        try {
            JSONObject tag=new JSONObject(String.valueOf(view.getTag()));
            if(tag.optString("name").startsWith("Navbar persistente")) {
                if(group.getChildCount()!=5)boundsErrors.add(screen+" navbar must have five destinations");
                int expected=(group.getWidth()-group.getPaddingLeft()-group.getPaddingRight())/5;
                for(int i=0;i<group.getChildCount();i++)if(group.getChildAt(i).getWidth()<expected-2)boundsErrors.add(screen+" compressed navigation destination");
            }
        } catch(org.json.JSONException ignored) {}
        for(int i=0;i<group.getChildCount();i++) {
            View child=group.getChildAt(i);
            if(child.getLeft() < -1)boundsErrors.add(screen+" child outside left edge: "+child.getTag());
            if(child.getRight() > group.getWidth()+1)boundsErrors.add(screen+" child outside right edge: "+child.getTag());
            assertHorizontalBounds(child,screen);
        }
    }
}
