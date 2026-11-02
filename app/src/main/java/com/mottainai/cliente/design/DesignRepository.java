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

/** Definições das telas nativas e links do protótipo, incluídos para uso offline. */
public final class DesignRepository {
    private static DesignRepository instance;
    private final JSONObject screens;
    private final Map<String, JSONObject> links = new HashMap<>();
    /** Obtém o recurso solicitado usando a instância disponível para o contexto. */
    public static synchronized DesignRepository get(Context context) {
        if(instance==null) instance=new DesignRepository(context.getApplicationContext());
        return instance;
    }
    /** Inicializa DesignRepository com os dados e as dependências recebidos. */
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
    /** Lê os dados locais que descrevem as telas do aplicativo. */
    private static String read(Context context,String asset) throws IOException {
        try(InputStream stream=context.getAssets().open(asset)) {
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            byte[] buffer=new byte[8192];
            int count;
            while((count=stream.read(buffer))!=-1) bytes.write(buffer,0,count);
            return new String(bytes.toByteArray(),StandardCharsets.UTF_8);
        }
    }
    /** Obtém a descrição da tela pelo seu identificador. */
    public JSONObject screen(String id) { return screens.optJSONObject(id); }
    /** Resolve o identificador usado como referência principal da tela. */
    public String canonical(String id) {
        JSONObject screen=screen(id);
        return screen==null?id:screen.optString("canonical",id);
    }
    /** Resolve o destino de navegação associado ao componente. */
    public JSONObject link(String id) { return links.get(id); }
    /** Obtém a ação de navegação ou interação associada ao componente. */
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
    /** Verifica se o recurso solicitado está disponível no repositório de telas. */
    public boolean available(String id) { return screens.has(id); }
}
