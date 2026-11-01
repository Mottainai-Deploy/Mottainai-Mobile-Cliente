package com.mottainai.cliente;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.core.widget.NestedScrollView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import static org.junit.Assert.*;

/** Exhaustive native renders and text geometry, rather than representative screens only. */
@RunWith(AndroidJUnit4.class)
public class DesignVisualReviewTest {
    @Test public void renderEveryScreenAndScrolledContent() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("cliente_preferences",Context.MODE_PRIVATE).edit().clear().commit();
        JSONObject specs;
        try(InputStream in=context.getAssets().open("design_screens.json")) {
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            byte[] buffer=new byte[8192];int count;
            while((count=in.read(buffer))!=-1)out.write(buffer,0,count);
            specs=new JSONObject(new String(out.toByteArray(),StandardCharsets.UTF_8));
        }
        List<String> ids=new ArrayList<>();
        for(Iterator<String> keys=specs.keys();keys.hasNext();) {
            String id=keys.next();if(!specs.getJSONObject(id).has("canonical"))ids.add(id);
        }
        Collections.sort(ids);
        String requested=InstrumentationRegistry.getArguments().getString("screens","");
        if(!requested.isEmpty()) {
            java.util.Set<String> selected=new java.util.HashSet<>(java.util.Arrays.asList(requested.split(",")));
            ids.removeIf(id->!selected.contains(id));
            assertEquals("Every requested screen must exist",selected.size(),ids.size());
        }
        File folder=new File(context.getExternalFilesDir(null),"visual-review");
        assertTrue(folder.isDirectory()||folder.mkdirs());
        JSONArray report=new JSONArray();
        for(boolean dark:new boolean[]{false,true}) {
            context.getSharedPreferences("cliente_preferences",Context.MODE_PRIVATE).edit().putInt("theme",dark?2:1).commit();
            try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(new Intent(context,MainActivity.class).putExtra("screen",ids.get(0)).putExtra("design_preview",true))) {
            for(String id:ids) {
                String key=id.replace(':','_')+(dark?"_dark":"_light");
                    scenario.onActivity(activity->activity.showScreen(id,false));
                    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                    final Throwable[] error={null};
                    scenario.onActivity(activity->{
                        try {
                            View root=activity.findViewById(R.id.design_container);
                            assertTrue(key,root.getWidth()>0&&root.getHeight()>0);
                            save(root,new File(folder,key+".png"));
                            JSONObject item=new JSONObject();item.put("screen",id);item.put("dark",dark);
                            item.put("name",specs.getJSONObject(id).getString("name"));
                            JSONArray views=new JSONArray();inspect(root,views);item.put("views",views);
                            report.put(item);
                            List<NestedScrollView> scrolls=new ArrayList<>();findScrolls(root,scrolls);
                            int index=0;
                            for(NestedScrollView scroll:scrolls) {
                                if(scroll.getChildCount()==0||!scroll.canScrollVertically(1))continue;
                                scroll.scrollTo(0,scroll.getChildAt(0).getHeight());
                                save(root,new File(folder,key+"_end"+(index++)+".png"));
                            }
                        } catch(Throwable exception) {error[0]=exception;}
                    });
                    if(error[0]!=null)throw new AssertionError(key,error[0]);
                }
            }
        }
        try(FileOutputStream out=new FileOutputStream(new File(folder,"geometry.json"))) {
            out.write(report.toString().getBytes(StandardCharsets.UTF_8));
        }
        android.app.UiAutomation automation=InstrumentationRegistry.getInstrumentation().getUiAutomation();
        String command="mkdir -p /data/local/tmp/mottainai-visual-review";
        try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command))) {while(in.read()!=-1){}}
        command="cp -r "+folder.getAbsolutePath()+"/. /data/local/tmp/mottainai-visual-review/";
        try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command))) {while(in.read()!=-1){}}
        context.getSharedPreferences("cliente_preferences",Context.MODE_PRIVATE).edit().putInt("theme",1).commit();
        assertEquals(ids.size()*2,report.length());
    }
    private void save(View view,File file) throws Exception {
        Bitmap image=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(image));
        try(FileOutputStream out=new FileOutputStream(file)) {assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));}
        image.recycle();
    }
    private void findScrolls(View view,List<NestedScrollView> result) {
        if(view instanceof NestedScrollView)result.add((NestedScrollView)view);
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)findScrolls(((ViewGroup)view).getChildAt(i),result);
    }
    private void inspect(View view,JSONArray rows) throws Exception {
        JSONObject row=new JSONObject();row.put("tag",String.valueOf(view.getTag()));row.put("type",view.getClass().getSimpleName());
        int[] position=new int[2];view.getLocationOnScreen(position);
        row.put("x",position[0]);row.put("y",position[1]);row.put("w",view.getWidth());row.put("h",view.getHeight());
        if(view instanceof TextView) {
            TextView text=(TextView)view;row.put("text",text.getText());row.put("size",text.getTextSize());
            if(text.getLayout()!=null&&text.getLineCount()>0) {
                row.put("lines",text.getLineCount());row.put("neededHeight",text.getLayout().getHeight());
                row.put("availableHeight",text.getHeight()-text.getCompoundPaddingTop()-text.getCompoundPaddingBottom());
                row.put("editable",text instanceof EditText);
                float widest=0;for(int i=0;i<text.getLineCount();i++)widest=Math.max(widest,text.getLayout().getLineMax(i));
                row.put("neededWidth",widest);row.put("availableWidth",text.getWidth()-text.getCompoundPaddingLeft()-text.getCompoundPaddingRight());
            }
        }
        rows.put(row);
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)inspect(((ViewGroup)view).getChildAt(i),rows);
    }
}
