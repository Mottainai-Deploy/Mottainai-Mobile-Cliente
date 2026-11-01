package com.mottainai.cliente.design;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.mottainai.cliente.R;
import com.mottainai.cliente.fragments.DesignScreenFragment;
import org.json.JSONObject;

public final class ClientContent {
    private ClientContent(){}
    public static void bind(DesignScreenFragment fragment,View root,String screen,SharedPreferences prefs) {
        replace(root,"Ofertas / Lista compacta",R.string.design_offers_empty);
        replace(root,"Ofertas / Grade compacta",R.string.design_offers_empty);
        replace(root,"Lojas / Mercados próximos",R.string.design_stores_empty);
        replace(root,"Fidelidade / Saldo confirmado",R.string.design_points_empty);
        replace(root,"Economia / Exemplo de cálculo",R.string.design_economy_empty);
        hide(root,"Economia / Contexto dos dados");
        hide(root,"Sugestões de pesquisa");hide(root,"Sugestões de busca");
        hide(root,"Opção / Status de participação");hide(root,"Participação / Próximo nível");
        hide(root,"Progresso / Broto");hide(root,"Progresso até Planta / 55%");
        replace(root,"Histórico / Conteúdo",R.string.design_chat_history_empty);
        for(String name:new String[]{"Registro / Mercado Verde","Registro / Padaria Sol","Opção / Compra cancelada","Filtro / Loja selecionada","Opção / Mercado da Praça"})hide(root,name);
        if(screen.equals("2093:6456")||screen.equals("2093:6588")) {
            View scroll=root.findViewById(R.id.design_scroll);
            if(scroll instanceof ViewGroup&&((ViewGroup)scroll).getChildCount()>0) {
                ViewGroup content=(ViewGroup)((ViewGroup)scroll).getChildAt(0);
                content.removeAllViews();content.addView(label(root.getContext(),root.getContext().getString(R.string.design_points_empty),16));
            }
        }
        update(root,prefs,screen);
    }
    private static void update(View view,SharedPreferences prefs,String screen) {
        JSONObject tag=tag(view);String action=tag.optString("action").trim();
        if(action.equals("Compartilhar")||action.equals("Mercado da Praça"))view.setVisibility(View.GONE);
        if(action.equals("Claro")||action.equals("Escuro")) {
            boolean dark=(view.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
            boolean selected=action.equals(dark?"Escuro":"Claro");view.setSelected(selected);
            androidx.core.view.ViewCompat.setStateDescription(view,selected?"Selecionado":"Não selecionado");
            view.setMinimumHeight(dp(view.getContext(),56));
            if(view instanceof LinearLayout)((LinearLayout)view).setGravity(android.view.Gravity.CENTER);
        }
        if(view instanceof TextView&&!(view instanceof EditText)) {
            TextView text=(TextView)view;String value=text.getText().toString();
            String name=prefs.getString("field_Nome completo","");
            if(name.equals("Camila Oliveira"))name="";
            String email=prefs.getString("field_E-mail","");if(email.equals("camila@exemplo.com"))email="";
            if(value.equals("Camila")||value.equals("Camila Oliveira"))text.setText(name.isEmpty()?"Seu perfil":name);
            else if(value.contains("camila@exemplo.com"))text.setText(email.isEmpty()?"Conta autenticada":email);
            else if(value.equals("Bom dia, Camila")||value.equals("Oi, Camila."))text.setText(name.isEmpty()?"Olá!":"Olá, "+name.split("\\s+")[0]+"!");
            else if(value.contains("320 pontos")||value.equals("320"))text.setText("Saldo indisponível");
            else if(value.equals("Sua posição atual"))text.setText("Sua região");
            else if(value.equals("Vila Mariana, São Paulo")||value.equals("Vila Mariana, SP"))text.setText("Localização do aparelho");
            else if(value.equals("Café sem açúcar"))text.setText(prefs.getString("search_query",""));
            else if(value.equals("Outubro de 2026"))view.setVisibility(View.GONE);
            else if(value.startsWith("Sugestões para buscar")||value.startsWith("Sugestões de busca"))view.setVisibility(View.GONE);
            else if(value.startsWith("Faltam 180")||value.equals("55%"))view.setVisibility(View.GONE);
            else if(value.equals("Desativados"))text.setText(prefs.getBoolean("nearby_alerts",false)&&androidx.core.app.NotificationManagerCompat.from(view.getContext()).areNotificationsEnabled()?"Ativados":"Desativados");
            else if(value.equals("Compartilhar")||value.startsWith("Broto ·")||value.equals("55%"))view.setVisibility(View.GONE);
        }
        if(view instanceof EditText&&tag.optString("field").equals("Busca")&&screen.equals("2413:31330"))((EditText)view).setText(prefs.getString("search_query",""));
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)update(((ViewGroup)view).getChildAt(i),prefs,screen);
    }
    private static void replace(View root,String name,int message) {
        View view=find(root,name);
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;group.removeAllViews();group.addView(label(root.getContext(),root.getContext().getString(message),16));}
    }
    private static void hide(View root,String name){View v=find(root,name);if(v!=null)v.setVisibility(View.GONE);}
    public static View find(View root,String name){if(tag(root).optString("name").equals(name))return root;if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){View v=find(((ViewGroup)root).getChildAt(i),name);if(v!=null)return v;}return null;}
    public static JSONObject tag(View view){try{return new JSONObject(String.valueOf(view.getTag()));}catch(Exception ignored){return new JSONObject();}}
    public static TextView label(Context context,String text,int size){TextView label=new TextView(context);label.setText(text);label.setTextSize(size);label.setTextColor(ContextCompat.getColor(context,R.color.design_text));label.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(context,R.font.dm_sans));label.setFontVariationSettings("'wght' 400, 'opsz' 14");label.setPadding(0,dp(context,12),0,dp(context,12));label.setLayoutParams(new ViewGroup.LayoutParams(-1,-2));return label;}
    private static int dp(Context c,int value){return Math.round(c.getResources().getDisplayMetrics().density*value);}
}
