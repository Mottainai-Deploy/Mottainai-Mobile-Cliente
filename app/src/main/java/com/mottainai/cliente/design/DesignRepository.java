package com.mottainai.cliente.design;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class DesignRepository {
    private static DesignRepository instance;
    private final JSONObject screens;
    private final Map<String, JSONObject> links = new HashMap<>();
    public static synchronized DesignRepository get(Context context) {
        if(instance==null) instance=new DesignRepository(context.getApplicationContext());
        return instance;
    }
    private DesignRepository(Context context) {
        try {
            screens=new JSONObject(read(context,"design_screens.json"));
            JSONArray navigation=new JSONArray(read(context,"design_navigation.json"));
            for(int i=0;i<navigation.length();i++) {
                JSONObject link=navigation.getJSONObject(i);
                links.put(link.getString("id"),link);
            }
        } catch(IOException|JSONException e) { throw new IllegalStateException(e); }
    }
    private static String read(Context context,String asset) throws IOException {
        try(InputStream stream=context.getAssets().open(asset)) {
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            byte[] buffer=new byte[8192];
            int count;
            while((count=stream.read(buffer))!=-1) bytes.write(buffer,0,count);
            return new String(bytes.toByteArray(),StandardCharsets.UTF_8);
        }
    }
    public JSONObject screen(String id) { return screens.optJSONObject(id); }
    public String canonical(String id) {
        JSONObject screen=screen(id);
        return screen==null?id:screen.optString("canonical",id);
    }
    public JSONObject link(String id) { return links.get(id); }
    public JSONObject action(String screenId,String nodeId,String name,String label) {
        JSONObject exact=links.get(nodeId);
        if(exact!=null) return exact;
        JSONObject spec=screen(screenId);
        JSONArray actions=spec==null?null:spec.optJSONArray("actions");
        if(actions==null) return null;
        for(int i=0;i<actions.length();i++) {
            JSONObject candidate=actions.optJSONObject(i);
            String candidateName=candidate.optString("name");
            if((!name.isEmpty()&&candidateName.equals(name))||
                    (!label.isEmpty()&&candidateName.endsWith(" / "+label))) {
                JSONObject link=links.get(candidate.optString("id"));
                if(link!=null) return link;
            }
        }
        return null;
    }
    public boolean available(String id) { return screens.has(id); }
}
