package com.mottainai.cliente;

import android.content.res.Configuration;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.mottainai.cliente.design.DesignRepository;
import com.mottainai.cliente.fragments.DesignScreenFragment;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS="cliente_preferences";
    private boolean artworkFullBleed;
    @Override protected void onCreate(Bundle state) {
        AppCompatDelegate.setDefaultNightMode(getSharedPreferences(PREFS,MODE_PRIVATE).getInt("theme",AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));
        super.onCreate(state);
        if(!isDesignPreview()) {
            android.content.SharedPreferences prefs=getSharedPreferences(PREFS,MODE_PRIVATE);
            android.content.SharedPreferences.Editor cleanup=prefs.edit().remove("demo_session");
            for(String key:prefs.getAll().keySet())if(key.startsWith("field_"))cleanup.remove(key);
            cleanup.apply();
            if(!prefs.getBoolean("fixtures_removed_v1",false)) {
                android.content.SharedPreferences.Editor editor=prefs.edit().putBoolean("fixtures_removed_v1",true);
                for(String key:new String[]{"field_Nome completo","field_E-mail","field_Telefone"}) {
                    String value=prefs.getString(key,"");
                    if(value.equals("Camila Oliveira")||value.equals("camila@exemplo.com")||value.equals("(11) 90000-0000"))editor.remove(key);
                }
                editor.apply();
            }
        }
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        boolean dark=(getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
        WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).setAppearanceLightStatusBars(!dark);
        WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).setAppearanceLightNavigationBars(!dark);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.design_content),(v,insets)->{
            Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout());
            Insets keyboard=insets.getInsets(WindowInsetsCompat.Type.ime());
            if(artworkFullBleed)v.setPadding(bars.left,0,bars.right,keyboard.bottom);
            else v.setPadding(bars.left,bars.top,bars.right,Math.max(bars.bottom,keyboard.bottom));
            return insets;
        });
        if(state==null) {
            String requested=getIntent().getStringExtra("screen");
            com.mottainai.cliente.utils.SessionManager session=new com.mottainai.cliente.utils.SessionManager(this);
            String initial=session.canBrowse()?"2005:3812":"2244:11739";
            showScreen(requested!=null?requested:initial,false);
        }
    }
    public void showScreen(String id, boolean history) {
        DesignRepository repository=DesignRepository.get(this);
        if(!repository.available(id))return;
        if(!isDesignPreview()) {
            com.mottainai.cliente.utils.SessionManager session=new com.mottainai.cliente.utils.SessionManager(this);
            String name=repository.screen(repository.canonical(id)).optString("name");
            if(!com.mottainai.cliente.design.ClientRoutes.isPublic(name)&&!session.canBrowse())id="2028:4492";
            else if(session.canBrowse()&&!session.isDemoMode()&&!com.mottainai.cliente.design.ClientRoutes.isPublic(name)
                    &&!com.mottainai.cliente.design.ClientRoutes.isLocationAccess(name)
                    &&(!com.mottainai.cliente.utils.ClientAccessGuard.hasLocationPermission(this)
                    ||!com.mottainai.cliente.utils.ClientAccessGuard.isLocationEnabled(this))) {
                id=!com.mottainai.cliente.utils.ClientAccessGuard.hasLocationPermission(this)?"2557:30312":"2557:30376";
            }
        }

        if(!isDesignPreview()&&repository.canonical(id).equals("2557:30792")) {
            if(currentScreen() instanceof DesignScreenFragment){((DesignScreenFragment)currentScreen()).requestNotificationPermission();return;}
            id="2557:30760";
        }
        if(!repository.available(id)) return;
        android.view.View focused=getCurrentFocus();
        if(focused!=null) {
            android.view.inputmethod.InputMethodManager keyboard=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            keyboard.hideSoftInputFromWindow(focused.getWindowToken(),0);
        }
        String canonical=repository.canonical(id);
        androidx.fragment.app.FragmentManager manager=getSupportFragmentManager();
        if(!isDesignPreview()&&canonical.equals("2028:4492")&&manager.getBackStackEntryCount()>0) {
            manager.popBackStack(null,androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        if(manager.findFragmentById(R.id.design_overlay)!=null)manager.popBackStack("overlay",androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        boolean overlay=repository.screen(canonical).optBoolean("overlay");
        androidx.fragment.app.FragmentTransaction transaction=manager.beginTransaction().setReorderingAllowed(true);
        transaction.setCustomAnimations(android.R.anim.fade_in,android.R.anim.fade_out,android.R.anim.fade_in,android.R.anim.fade_out);
        if(overlay) {
            if(manager.findFragmentById(R.id.design_content)==null) {
                String base=overlayBase(canonical);
                transaction.replace(R.id.design_content,DesignScreenFragment.create(base),base);
            }
            transaction.add(R.id.design_overlay,DesignScreenFragment.create(canonical),canonical).addToBackStack("overlay");
        } else {
            transaction.replace(R.id.design_content,DesignScreenFragment.create(canonical),canonical);
            if(history)transaction.addToBackStack(canonical);
        }
        transaction.commit();
    }
    @Override protected void onResume() {
        super.onResume();
        if(isDesignPreview())return;
        androidx.fragment.app.Fragment current=currentScreen();
        if(current==null||current.getArguments()==null)return;
        String id=current.getArguments().getString("screen","");
        String name=DesignRepository.get(this).screen(id).optString("name");
        com.mottainai.cliente.utils.SessionManager session=new com.mottainai.cliente.utils.SessionManager(this);
        if(!com.mottainai.cliente.design.ClientRoutes.isPublic(name)&&!session.canBrowse())showScreen("2028:4492",false);
        else if(session.canBrowse()&&com.mottainai.cliente.design.ClientRoutes.isLocationAccess(name)
                &&com.mottainai.cliente.utils.ClientAccessGuard.hasLocationPermission(this)
                &&com.mottainai.cliente.utils.ClientAccessGuard.isLocationEnabled(this))showScreen("2005:3812",false);
    }
    public void openAuthenticatedHome() {
        startActivity(new android.content.Intent(this,MainActivity.class).putExtra("screen","2005:3812")
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK|android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
    public androidx.fragment.app.Fragment currentScreen() {
        androidx.fragment.app.Fragment overlay=getSupportFragmentManager().findFragmentById(R.id.design_overlay);
        return overlay!=null?overlay:getSupportFragmentManager().findFragmentById(R.id.design_content);
    }
    public boolean isDesignPreview() { return BuildConfig.DEBUG&&getIntent().getBooleanExtra("design_preview",false); }
    private String overlayBase(String id) {
        String name=DesignRepository.get(this).screen(id).optString("name");
        if(name.contains("Foto do perfil"))return "2022:4219";
        if(name.contains("Fidelidade"))return "2093:6331";
        if(name.contains("Sair da conta"))return "2005:4483";
        if(name.contains("Filtros"))return "2005:3914";
        if(name.contains("Conversa"))return "2055:5379";
        if(name.contains("Cadastro"))return "2143:9538";
        return "2005:3812";
    }
    public void setAppearance(boolean dark) {
        int mode=dark?AppCompatDelegate.MODE_NIGHT_YES:AppCompatDelegate.MODE_NIGHT_NO;
        getSharedPreferences(PREFS,MODE_PRIVATE).edit().putInt("theme",mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }
    public void setArtworkFullBleed(boolean fullBleed) {
        artworkFullBleed=fullBleed;
        if(fullBleed)findViewById(R.id.design_content).setPadding(0,0,0,0);
        ViewCompat.requestApplyInsets(findViewById(R.id.design_content));
    }
    public void setDemoSession(boolean active) {
        getSharedPreferences(PREFS,MODE_PRIVATE).edit().putBoolean("demo_session",active).apply();
    }
}
