package com.mottainai.cliente.design;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.lifecycle.ViewModelProvider;
import com.mottainai.cliente.BuildConfig;
import com.mottainai.cliente.MainActivity;
import com.mottainai.cliente.R;
import com.mottainai.cliente.fragments.DesignScreenFragment;
import com.mottainai.cliente.models.*;
import com.mottainai.cliente.network.dto.*;
import com.mottainai.cliente.repository.*;
import com.mottainai.cliente.utils.*;
import com.mottainai.cliente.viewmodel.*;
import com.mottainai.cliente.widgets.PartnerMapView;
import org.json.JSONObject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.*;

public final class ClientRuntime {
    private final DesignScreenFragment fragment;
    private final MainActivity host;
    private final View root;
    private final String screen;
    private final String screenName;
    private final ClientFlowState flow;
    private final AuthRepository authRepository;
    private final SessionManager session;
    private final Map<String,EditText> fields=new LinkedHashMap<>();
    private AuthViewModel auth;
    private CatalogViewModel catalog;
    private ProfileViewModel profile;
    private AssistantViewModel assistant;
    private PartnerMapView map;
    private TextView feedback;
    private LinearLayout authLoading;
    private TextView authLoadingMessage;
    private boolean active=true;
    private boolean passwordBusy;
    private android.os.CancellationSignal googleRequest;

    public ClientRuntime(DesignScreenFragment fragment,View root,String screen) {
        this.fragment=fragment;this.root=root;this.screen=screen;host=(MainActivity)fragment.requireActivity();
        screenName=DesignRepository.get(host).screen(screen).optString("name");
        flow=new ViewModelProvider(host).get(ClientFlowState.class);
        session=new SessionManager(host);authRepository=new AuthRepository(host);
    }
    public void bind() {
        collectFields(root);
        hideUnsupportedRadiusOptions(root);
        hideUnsupportedAccountActions(root);
        boolean registration=isRegistration();
        AuthRepository.LoginResult pending=authRepository.pendingProfile();
        for(Map.Entry<String,EditText> entry:fields.entrySet()) {
            String key=entry.getKey();EditText field=entry.getValue();
            field.setSaveEnabled(false);
            if(key.equals("CPF"))InputMasks.apply(field,"###.###.###-##");
            if(key.equals("CEP"))InputMasks.apply(field,"#####-###");
            if(key.startsWith("Data de nascimento"))InputMasks.apply(field,"##/##/####");
            if(key.equals("Telefone"))InputMasks.applyPhone(field);
            if(registration) {
                field.setText(flow.registration.getOrDefault(key,""));
                if(pending!=null&&key.equals("E-mail")){field.setText(pending.email);field.setEnabled(false);}
                if(pending!=null&&key.toLowerCase(Locale.ROOT).contains("senha"))((View)field.getParent().getParent().getParent()).setVisibility(View.GONE);
                field.addTextChangedListener(new TextWatcher(){
                    public void beforeTextChanged(CharSequence s,int start,int count,int after){}
                    public void onTextChanged(CharSequence s,int start,int before,int count){flow.registration.put(key,s.toString());}
                    public void afterTextChanged(Editable value){}
                });
            }
            if(key.equals("Busca")){field.setText(catalog().promotionQuery());field.setOnEditorActionListener((v,action,event)->{
                if(action!=EditorInfo.IME_ACTION_SEARCH)return false;
                catalog().loadPromotions(0,null,field.getText().toString());host.showScreen("2005:3914",true);return true;
            });}
        }
        if(registration)fragment.renderMarketingConsent(flow.marketing);
        if(registration||screenName.startsWith("Cliente / Entrar")||screenName.contains("Recuperar senha")) {
            bindAuth();
            if(isReview())bindReview();
            return;
        }
        if(!session.canBrowse())return;
        bindIdentity();
        bindLocation();
        if(screen.equals("2005:3914"))bindInlineSearch();
        if(session.isDemoMode())status("Demonstração: os dados desta sessão são fictícios.");
        if(screen.equals("2005:3812")||screen.equals("2005:3914"))bindPromotions();
        if(screen.equals("2005:4305")||screen.equals("2418:25646"))bindStores();
        if(screen.equals("2005:4104"))bindPromotionDetail();
        if(screen.equals("2005:4391"))bindStoreDetail();
        if(screen.equals("2093:6331")||screen.equals("2005:3812")||screen.equals("2005:4483")||screen.equals("2143:11079"))bindPoints();
        if(screen.equals("2093:6456")||screen.equals("2093:6588"))bindHistory();
        if(screen.equals("2393:19184")||screen.equals("2650:73471"))bindProfile();
        if(screenName.contains("Cliente / Avisos /"))status("Ative os avisos para receber novidades e ofertas próximas.");
        if(screenName.contains("Conversa com a Lô"))bindAssistant();
        if(screen.equals("2143:10917"))bindFilters();
    }
    private boolean isRegistration(){return screenName.contains("/ Cadastro /")||screenName.contains("/ Concluir cadastro /");}
    private boolean isReview(){return screen.equals("2143:9630")||screen.equals("2392:19502");}
    private void hideUnsupportedRadiusOptions(View view){
        String action=ClientContent.tag(view).optString("action").trim();
        if(action.equals("15 km")||action.equals("20 km")){view.setVisibility(View.GONE);return;}
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)hideUnsupportedRadiusOptions(((ViewGroup)view).getChildAt(i));
    }
    private void hideUnsupportedAccountActions(View view){
        String name=ClientContent.tag(view).optString("name").trim();
        String action=ClientContent.tag(view).optString("action").trim();
        if(action.equals("Alterar e-mail")){view.setVisibility(View.GONE);return;}
        if(name.equals("Opção / E-mail")){
            view.setClickable(false);view.setFocusable(false);
        }
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)hideUnsupportedAccountActions(((ViewGroup)view).getChildAt(i));
    }
    private void collectFields(View view){if(view instanceof EditText)fields.put(ClientContent.tag(view).optString("field"),(EditText)view);if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)collectFields(((ViewGroup)view).getChildAt(i));}
    private void bindAuth() {
        auth=new ViewModelProvider(fragment).get(AuthViewModel.class);
        auth.state().observe(fragment.getViewLifecycleOwner(),state->{
            renderAuthLoading(state);
            if(auth.isBusy())hideStatus();
            else if(state.message!=null)status(state.message);
            setAuthActionsEnabled(root,!auth.isBusy());
            for(Map.Entry<String,EditText> entry:fields.entrySet())entry.getValue().setEnabled(!auth.isBusy()
                    &&!(isRegistration()&&entry.getKey().equals("E-mail")&&authRepository.pendingProfile()!=null));
            if(state.kind==AuthViewModel.State.Kind.AUTHENTICATED||state.kind==AuthViewModel.State.Kind.REGISTERED) {
                flow.clearRegistration();auth.reset();host.openAuthenticatedHome();
            } else if(state.kind==AuthViewModel.State.Kind.PROFILE_REQUIRED) {
                flow.registration.remove("Senha");flow.registration.remove("Confirmar senha");
                flow.registration.put("E-mail",state.email==null?"":state.email);
                auth.reset();host.showScreen("2143:9435",true);
            } else if(state.kind==AuthViewModel.State.Kind.RESET_SENT) {
                auth.reset();host.showScreen("2143:10166",true);
            }
        });
        auth.address().observe(fragment.getViewLifecycleOwner(),address->{
            if(address==null||!RegistrationFields.digits(value("CEP")).equals(RegistrationFields.digits(address.cep)))return;
            setField("Rua",address.logradouro);setField("Bairro",address.bairro);setField("Cidade",address.localidade);setField("UF",address.uf);status("CEP consultado. Confira o endereço e informe o número.");
        });
        auth.cepError().observe(fragment.getViewLifecycleOwner(),message->{if(message!=null)status(message);});
    }
    public boolean act(JSONObject tag,String label) {
        String name=tag.optString("name");
        if(auth!=null&&auth.isBusy()){status("Aguarde a conclusão da solicitação.");return true;}
        if(label.equals("Entrar")) {
            if(validEmail("E-mail")&&required("Senha"))auth.login(authRepository,value("E-mail").trim(),value("Senha"));return true;
        }
        if(label.equals("Continuar com Google")||label.equals("Confirmar com Google")) {
            if(auth==null){status("A alteração de e-mail ainda não está disponível.");return true;}
            if(!auth.beginGoogleSignIn())return true;
            View focused=host.getCurrentFocus();
            if(focused!=null)((android.view.inputmethod.InputMethodManager)host.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(focused.getWindowToken(),0);
            googleRequest=GoogleSignInHelper.signIn(host,new GoogleSignInHelper.Callback(){
                public void onToken(String token){if(active)auth.loginWithGoogle(authRepository,token);}
                public void onError(String message){if(active)auth.googleSignInFailed(message);}
            });return true;
        }
        if(label.equals("Enviar link")) {if(validEmail("E-mail"))auth.resetPassword(authRepository,value("E-mail").trim());return true;}
        if(label.equals("Já tenho conta")||label.equals("Voltar para entrar")){flow.clearRegistration();host.showScreen("2028:4492",false);return true;}
        if(label.equals("Criar conta")&&isReview()) {CreateCustomerRequest request=registrationRequest();if(request!=null)auth.register(authRepository,request);return true;}
        if(label.equals("Continuar")&&isRegistration()){if(validatePersonal())host.showScreen("2143:9538",true);return true;}
        if(label.equals("Revisar cadastro")){if(validateAddress())host.showScreen("2143:9630",true);return true;}
        if(label.equals("Consultar CEP")){String cep=RegistrationFields.digits(value("CEP"));if(cep.length()!=8)error("CEP","Informe os 8 dígitos do CEP.");else{status("Consultando CEP…");auth.lookupCep(authRepository,cep);}return true;}
        if(name.equals("Conta / Consentimento de marketing")||name.equals("Consentimento / Opção documentada")){flow.marketing=!flow.marketing;fragment.renderMarketingConsent(flow.marketing);return true;}
        if(label.equals("Sair")){CloudinaryProfilePhoto.pendingFile(host).delete();authRepository.signOut();flow.clearRegistration();flow.store=null;flow.promotionId=null;
            host.startActivity(new Intent(host,MainActivity.class).putExtra("screen","2244:11739")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));host.finish();return true;}
        if(label.matches("(5|10) km")){int km=Integer.parseInt(label.split(" ")[0]);catalog().useStoreRadius(OfferRadius.fromKilometers(km));
            host.getSharedPreferences("cliente_preferences",0).edit().putString("radius",label).apply();host.getSupportFragmentManager().popBackStack();return true;}
        if(label.equals("Permitir localização")||label.equals("Usar minha localização")||label.equals("Durante o uso do app")||label.equals("Apenas desta vez")||(label.equals("Tentar novamente")||label.equals("Verificar novamente")||label.equals("Rever acesso"))&&ClientRoutes.isLocationAccess(screenName)) {
            if(!ClientAccessGuard.hasLocationPermission(host))fragment.requestClientLocation();
            else if(!ClientAccessGuard.isLocationEnabled(host))host.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            else host.openAuthenticatedHome();return true;
        }
        if(label.contains("Abrir configurações")||label.contains("Configurações do celular")) {
            if(ClientRoutes.isLocationAccess(screenName))host.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            else host.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+host.getPackageName())));return true;
        }
        if(label.equals("Ativar GPS")||label.equals("Abrir ajustes de localização")||label.equals("Ativar localização")){host.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));return true;}
        if(label.equals("Como chegar")){if(flow.store!=null)MapNavigation.openRoute(host,new PartnerStore(flow.store));else status("Selecione uma loja para abrir sua localização no Google Maps.");return true;}
        if(label.equals("Ver informações da loja")){if(flow.store!=null)host.showScreen("2005:4391",true);else status("A loja não foi informada nesta oferta.");return true;}
        if(label.equals("Explorar ofertas")){catalog().loadPromotions(0,flow.store==null?null:flow.store.id,"");host.showScreen("2005:3914",true);return true;}
        if(label.equals("Limpar")||label.equals("Todas as lojas")){catalog().loadPromotions(0,null,"");host.showScreen("2005:3914",false);return true;}
        if(label.equals("Salvar dados")||label.equals("Salvar preferências")){saveProfile();return true;}
        if(screen.equals("2028:4279")&&label.equals("Alterar senha")){
            changePassword();return true;
        }
        if(label.equals("Salvar foto")){saveProfilePhoto();return true;}
        if(name.equals("Buscar produtos")&&screen.equals("2005:3812")){flow.openSearch=true;host.showScreen("2005:3914",true);return true;}
        if(label.equals("Lojas próximas")){host.showScreen("2005:4305",true);return true;}
        if(label.equals("Ofertas por perto")){host.showScreen("2005:3914",true);return true;}
        if(label.equals("Como retirar?")){sendSuggestion("Como funciona a retirada na loja?");return true;}
        if(label.equals("Minhas conversas")){host.showScreen("2557:30504",true);return true;}
        if(label.equals("Nova conversa")){assistant().newConversation();host.showScreen("2055:5379",true);return true;}
        if(name.startsWith("Mensagem /")||name.equals("Escrever mensagem")||name.equals("Enviar mensagem")||name.equals("Campo / mensagem")){sendChat();return true;}
        return false;
    }
    public void saveProfilePhoto(){
        java.io.File pending=CloudinaryProfilePhoto.pendingFile(host);
        if(!pending.isFile()){notice("Escolha uma foto antes de salvar.");return;}
        if(!CloudinaryProfilePhoto.isConfigured()){notice("Não foi possível enviar a foto agora. Tente novamente mais tarde.");return;}
        notice("Enviando foto…");
        CloudinaryProfilePhoto.upload(host,pending,new CloudinaryProfilePhoto.Result(){
            public void onSuccess(String secureUrl){
                if(!active)return;
                ProfileViewModel photoProfile=new ViewModelProvider(fragment).get(ProfileViewModel.class);
                photoProfile.saveAvatar(secureUrl,new ProfileViewModel.SaveResult(){
                    public void success(CustomerProfileResponse saved){
                        if(!active)return;
                        CloudinaryProfilePhoto.saveUrl(host,secureUrl);
                        try{java.nio.file.Files.move(pending.toPath(),CloudinaryProfilePhoto.photoFile(host).toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);}
                        catch(java.io.IOException ignored){}
                        notice("Foto atualizada.");
                        host.showScreen("2022:4219",false);
                    }
                    public void error(String message){if(active)notice("A foto foi enviada, mas não foi possível vinculá-la ao perfil. Tente novamente.");}
                });
            }
            public void onFailure(){if(active)notice("Não foi possível enviar a foto agora. Confira sua conexão e tente novamente.");}
        });
    }
    public void locationResult(){host.showScreen(!ClientAccessGuard.hasLocationPermission(host)?"2557:30344":!ClientAccessGuard.isLocationEnabled(host)?"2557:30376":"2005:3812",false);}
    private String value(String label){EditText field=fields.get(label);return field!=null?field.getText().toString():flow.registration.getOrDefault(label,"");}
    private void setField(String label,String value){EditText field=fields.get(label);if(field!=null)field.setText(value==null?"":value);}
    private boolean error(String label,String message){EditText field=fields.get(label);if(field!=null)DesignFormControls.error(field,message);else status(message);return false;}
    private boolean required(String label){return !value(label).trim().isEmpty()||error(label,"Preencha este campo");}
    private boolean validEmail(String label){return Patterns.EMAIL_ADDRESS.matcher(value(label).trim()).matches()&&value(label).length()<=150||error(label,"Informe um e-mail válido.");}
    private boolean validatePersonal(){
        if(value("Nome completo").trim().isEmpty()||value("Nome completo").length()>150)return error("Nome completo","Informe seu nome (até 150 caracteres).");
        if(!CpfValidator.isValid(RegistrationFields.digits(value("CPF"))))return error("CPF","Confira os dígitos do CPF.");
        if(!validEmail("E-mail"))return false;
        if(!RegistrationFields.validPhone(value("Telefone")))return error("Telefone","Informe DDD e um telefone válido.");
        try{RegistrationFields.birthDateIso(value("Data de nascimento (opcional)"));}catch(Exception e){return error("Data de nascimento (opcional)","Informe uma data válida em DD/MM/AAAA, sem data futura.");}
        if(authRepository.pendingProfile()==null){
            String password=value("Senha");if(password.length()<8||password.length()>100)return error("Senha","Use uma senha de 8 a 100 caracteres.");
            if(!password.equals(value("Confirmar senha")))return error("Confirmar senha","As senhas precisam ser iguais.");
        }return true;
    }
    private boolean validateAddress(){
        if(RegistrationFields.digits(value("CEP")).length()!=8)return error("CEP","Informe os 8 dígitos do CEP.");
        for(String label:new String[]{"Rua","Número","Bairro","Cidade"})if(!required(label))return false;
        if(!Arrays.asList("AC","AL","AP","AM","BA","CE","DF","ES","GO","MA","MT","MS","MG","PA","PB","PR","PE","PI","RJ","RN","RS","RO","RR","SC","SP","SE","TO").contains(value("UF").trim().toUpperCase(Locale.ROOT)))return error("UF","Informe uma UF válida.");
        return true;
    }
    private CreateCustomerRequest registrationRequest(){
        if(!validatePersonal()||!validateAddress()){status("Revise os dados pessoais e o endereço antes de concluir.");return null;}
        return new CreateCustomerRequest(value("Nome completo").trim(),RegistrationFields.digits(value("CPF")),value("E-mail").trim(),value("Senha"),
                RegistrationFields.phoneDigits(value("Telefone")),RegistrationFields.birthDateIso(value("Data de nascimento (opcional)")),flow.marketing,
                new CreateAddressRequest(RegistrationFields.digits(value("CEP")),value("Rua").trim(),value("Número").trim(),value("Complemento (opcional)").trim(),value("Bairro").trim(),value("Cidade").trim(),value("UF").trim().toUpperCase(Locale.ROOT)));
    }
    private void bindReview(){
        text(root,"customerName",value("Nome completo"));text(root,"customerEmail",value("E-mail"));text(root,"customerPhone",value("Telefone"));
        text(root,"reviewStreet",value("Rua")+", "+value("Número"));text(root,"reviewCity",value("Bairro")+", "+value("Cidade")+" — "+value("UF"));text(root,"reviewZip","CEP "+value("CEP"));
    }
    private CatalogViewModel catalog(){if(catalog==null)catalog=new ViewModelProvider(host).get(CatalogViewModel.class);return catalog;}
    private AssistantViewModel assistant(){if(assistant==null)assistant=new ViewModelProvider(host).get(AssistantViewModel.class);return assistant;}
    private void changePassword(){
        if(passwordBusy)return;
        com.google.firebase.auth.FirebaseAuth firebase=FirebaseSupport.authIfConfigured(host);
        com.google.firebase.auth.FirebaseUser user=firebase==null?null:firebase.getCurrentUser();
        String current=value("Senha atual"), fresh=value("Nova senha"), confirm=value("Confirmar nova senha");
        if(user==null||user.getEmail()==null){status("Entre novamente para alterar sua senha.");return;}
        boolean passwordProvider=false;
        for(com.google.firebase.auth.UserInfo info:user.getProviderData())if("password".equals(info.getProviderId()))passwordProvider=true;
        if(!passwordProvider){status("Esta conta usa Google. Altere a senha na sua conta Google.");return;}
        if(current.isEmpty()){error("Senha atual","Informe a senha atual.");return;}
        if(fresh.length()<8||fresh.length()>100){error("Nova senha","Use uma senha de 8 a 100 caracteres.");return;}
        if(!fresh.equals(confirm)){error("Confirmar nova senha","As senhas precisam ser iguais.");return;}
        passwordBusy=true;status("Verificando sua senha…");
        for(EditText field:fields.values())field.setEnabled(false);
        user.reauthenticate(com.google.firebase.auth.EmailAuthProvider.getCredential(user.getEmail(),current)).addOnCompleteListener(result->{
            if(!active)return;
            if(!result.isSuccessful()){passwordBusy=false;for(EditText field:fields.values())field.setEnabled(true);status("Não foi possível confirmar sua senha atual. Confira a senha e a conexão.");return;}
            status("Alterando senha…");user.updatePassword(fresh).addOnCompleteListener(updated->{
                if(!active)return;passwordBusy=false;for(EditText field:fields.values())field.setEnabled(true);
                if(updated.isSuccessful()){for(EditText field:fields.values())field.setText("");status("Senha alterada com sucesso.");}
                else status("Não foi possível alterar a senha. Confira as regras de senha e tente novamente.");
            });
        });
    }
    private void bindLocation(){
        View region=ClientContent.find(root,"Localização / Região de busca");
        if(region==null)return;
        if(region.getParent() instanceof LinearLayout){LinearLayout parent=(LinearLayout)region.getParent();TextView radius=label("Busca de lojas: até "+selectedRadius());radius.setTextSize(14);radius.setOnClickListener(v->host.showScreen("2143:10256",true));parent.addView(radius,parent.indexOfChild(region)+1);}
        catalog().location().observe(fragment.getViewLifecycleOwner(),state->{
            if(state==null)return;
            if(state.status==LoadState.Status.ERROR){text(root,"region","Localização indisponível");return;}
            if(state.status!=LoadState.Status.SUCCESS||state.data==null)return;
            android.location.Location location=state.data;
            new Thread(()->{
                String result="Localização indisponível";
                try{java.util.List<android.location.Address> addresses=new android.location.Geocoder(host,Locale.forLanguageTag("pt-BR")).getFromLocation(location.getLatitude(),location.getLongitude(),1);
                    if(addresses!=null&&!addresses.isEmpty()){android.location.Address a=addresses.get(0);String district=a.getSubLocality(),city=a.getLocality(),uf=a.getAdminArea();java.util.List<String> parts=new java.util.ArrayList<>();for(String part:new String[]{district,city,uf})if(part!=null&&!part.trim().isEmpty()&&!parts.contains(part))parts.add(part);if(!parts.isEmpty())result=android.text.TextUtils.join(", ",parts);}}
                catch(Exception ignored){} String finalResult=result;host.runOnUiThread(()->{if(active)text(root,"region",finalResult);});
            }).start();
        });
        if(catalog().location().getValue()==null)catalog().requestLocation();
    }
    private String selectedRadius(){
        android.content.SharedPreferences preferences=host.getSharedPreferences("cliente_preferences",0);
        String saved=preferences.getString("radius","5 km");
        if("5 km".equals(saved)||"10 km".equals(saved))return saved;
        String allowed="10 km";
        preferences.edit().putString("radius",allowed).apply();
        return allowed;
    }
    private void bindInlineSearch(){
        View trigger=ClientContent.find(root,"Buscar produtos");if(trigger==null||!(trigger.getParent() instanceof ViewGroup))return;
        ViewGroup parent=(ViewGroup)trigger.getParent();EditText search=new EditText(host);search.setSingleLine(true);search.setHint("Buscar produtos");search.setText(catalog().promotionQuery());search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);search.setVisibility(View.GONE);parent.addView(search,parent.indexOfChild(trigger)+1,new ViewGroup.LayoutParams(-1,-2));
        trigger.setOnClickListener(v->{trigger.setVisibility(View.GONE);search.setVisibility(View.VISIBLE);search.setAlpha(0);search.animate().alpha(1).setDuration(200).start();search.requestFocus();((android.view.inputmethod.InputMethodManager)host.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)).showSoftInput(search,0);});
        if(flow.openSearch){flow.openSearch=false;trigger.post(()->{if(active)trigger.performClick();});}
        search.setOnEditorActionListener((v,action,event)->{if(action!=EditorInfo.IME_ACTION_SEARCH)return false;catalog().loadPromotions(0,catalog().promotionStoreId(),search.getText().toString());((android.view.inputmethod.InputMethodManager)host.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(),0);return true;});
    }
    private void bindIdentity(){
        com.google.firebase.auth.FirebaseAuth firebase=FirebaseSupport.authIfConfigured(host);
        String email=firebase==null||firebase.getCurrentUser()==null?"":firebase.getCurrentUser().getEmail();
        text(root,"customerName","Seu perfil");text(root,"customerEmail",email==null?"":email);text(root,"customerPhone","");text(root,"greeting","Olá!");
    }
    private void bindPromotions(){
        View slot=ClientContent.find(root,screen.equals("2005:3812")?"Ofertas / Lista compacta":"Ofertas / Grade compacta");
        if(!(slot instanceof LinearLayout))return;
        LinearLayout list=(LinearLayout)slot;list.setOrientation(LinearLayout.VERTICAL);list.setOnClickListener(null);list.setClickable(false);
        catalog().promotions().observe(fragment.getViewLifecycleOwner(),state->{
            if(state==null)return;if(expired(state.httpStatus))return;list.removeAllViews();
            if(state.status==LoadState.Status.LOADING)list.addView(label("Carregando ofertas…"));
            if(state.status==LoadState.Status.ERROR){list.addView(label(state.message));button(list,"Tentar novamente",()->catalog().loadPromotions(catalog().promotionPage(),catalog().promotionStoreId(),catalog().promotionQuery()));}
            if(state.data==null)return;
            List<CatalogPromotion> items=state.data.content==null?Collections.emptyList():state.data.content;
            if(items.isEmpty()&&state.status==LoadState.Status.SUCCESS)list.addView(label("Nenhuma oferta disponível para esta busca."));
            LinearLayout row=null;boolean compact=screen.equals("2005:3812");
            for(int i=0;i<items.size();i++){
                CatalogPromotion promotion=items.get(i);if(promotion==null)continue;
                if(compact){View card=offerCard(promotion,true,list);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(16);list.addView(card,lp);}
                else {if(i%2==0){row=new LinearLayout(host);row.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.bottomMargin=dp(16);list.addView(row,rp);}
                    View card=offerCard(promotion,false,row);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-2,1);if(i%2==0)cp.setMarginEnd(dp(8));else cp.setMarginStart(dp(8));row.addView(card,cp);}
            }
            if(!compact&&items.size()%2!=0&&row!=null)row.addView(new View(host),new LinearLayout.LayoutParams(0,1,1));
            if(state.status!=LoadState.Status.LOADING&&state.data.number+1<state.data.totalPages)button(list,"Carregar mais",()->catalog().loadPromotions(catalog().nextPromotionPage(),catalog().promotionStoreId(),catalog().promotionQuery()));
        });
        if(catalog().promotions().getValue()==null)catalog().loadPromotions(0,null,"");
    }
    private View offerCard(CatalogPromotion promotion,boolean compact,ViewGroup parent){
        View card=LayoutInflater.from(host).inflate(compact?R.layout.design_item_offer_compact:R.layout.design_item_offer_grid,parent,false);
        disableChildActions(card);bindOffer(card,promotion);card.setClickable(true);card.setFocusable(true);card.setContentDescription(promotion.name);
        card.setOnClickListener(v->{flow.promotionId=promotion.id;flow.store=promotion.store;host.showScreen("2005:4104",true);});return card;
    }
    private void bindOffer(View card,CatalogPromotion promotion){
        Offer offer=new Offer(promotion);String storeName=offer.getStoreName();boolean distinctStore=storeName!=null&&!storeName.trim().equalsIgnoreCase(offer.getTitle().trim());text(card,"title",offer.getTitle());text(card,"store",distinctStore?storeName:"");text(card,"storeDistance","");
        text(card,"description",offer.getDescription());text(card,"expiry",promotion.endsAt==null?"Prazo não informado":"Até "+dateTime(promotion.endsAt));text(card,"period",period(promotion));
        text(card,"pickup","Consulte a retirada na loja");text(card,"distance","");
        CatalogPromotion.Item item=promotion.items!=null&&promotion.items.size()==1?promotion.items.get(0):null;
        text(card,"price",item!=null&&item.promotionalPrice!=null?money(item.promotionalPrice):offer.getPriceLabel()==null?"Consulte o preço na loja":offer.getPriceLabel());
        text(card,"originalPrice",item!=null&&item.originalPrice!=null?money(item.originalPrice):"");
        String discount="";
        if(item!=null&&item.originalPrice!=null&&item.promotionalPrice!=null&&item.originalPrice.signum()>0&&item.promotionalPrice.compareTo(item.originalPrice)<0)
            discount="−"+item.originalPrice.subtract(item.promotionalPrice).multiply(BigDecimal.valueOf(100)).divide(item.originalPrice,0,RoundingMode.DOWN)+"%";
        text(card,"discount",discount);
        if(promotion.store!=null)text(card,"address",new PartnerStore(promotion.store).getAddressLabel());else text(card,"address","Endereço indisponível");
        View illustration=ClientContent.find(card,"Imagem ilustrativa do produto");
        if(illustration==null)illustration=ClientContent.find(card,"Ilustração do produto");
        if(illustration==null)illustration=ClientContent.find(card,"Produto / cesta hortifruti");
        if(illustration instanceof DesignArtLayout&&illustration.getParent() instanceof ViewGroup){
            ViewGroup parent=(ViewGroup)illustration.getParent();int index=parent.indexOfChild(illustration);
            ViewGroup.LayoutParams params=illustration.getLayoutParams();Object tag=illustration.getTag();parent.removeViewAt(index);
            LinearLayout box=new LinearLayout(host);box.setOrientation(LinearLayout.VERTICAL);box.setGravity(android.view.Gravity.CENTER);box.setTag(tag);box.setPadding(0,0,0,0);box.setBackground(null);box.setClipChildren(true);box.setClipToOutline(true);parent.addView(box,index,params);illustration=box;
        }
        if(illustration instanceof ViewGroup){ViewGroup box=(ViewGroup)illustration;box.setPadding(0,0,0,0);box.setBackground(null);box.removeAllViews();
            int imageHeight=card==root?dp(220):box.getLayoutParams().width==dp(112)?dp(136):dp(88);
            ImageView image=new ImageView(host);image.setContentDescription(offer.getTitle());image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);box.addView(image,new ViewGroup.LayoutParams(-1,imageHeight));
            ProductImages.load(image,offer,null);}
    }
    private String period(CatalogPromotion p){
        if(p.startsAt==null||p.startsAt.trim().isEmpty())return p.endsAt==null?"Período não informado":"Até "+dateTime(p.endsAt);
        if(p.endsAt==null||p.endsAt.trim().isEmpty())return "A partir de "+dateTime(p.startsAt);
        return dateTime(p.startsAt)+" até "+dateTime(p.endsAt);
    }
    private String dateTime(String value){
        try{return java.time.LocalDateTime.parse(value).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm",Locale.forLanguageTag("pt-BR")));}
        catch(java.time.format.DateTimeParseException ignored){return value.replace('T',' ').replaceAll("\\.\\d+$","");}
    }
    private void bindPromotionDetail(){
        if(flow.promotionId==null)flow.promotionId=host.getIntent().getStringExtra("offer_id");
        clearOfferFixtures();
        if(flow.promotionId==null){status("Selecione uma oferta no catálogo.");return;}
        catalog().detail().observe(fragment.getViewLifecycleOwner(),state->{if(state==null||expired(state.httpStatus))return;
            if(state.status==LoadState.Status.LOADING)status("Carregando oferta…");
            if(state.status==LoadState.Status.ERROR){status(state.message);button(body(),"Tentar novamente",()->catalog().loadPromotion(flow.promotionId));}
            if(state.status==LoadState.Status.SUCCESS&&state.data!=null){hideStatus();flow.store=state.data.store;bindOffer(root,state.data);status(new Offer(state.data).getItemsLabel());}
        });catalog().loadPromotion(flow.promotionId);
    }
    private void clearOfferFixtures(){for(String key:new String[]{"title","store","storeDistance","price","originalPrice","description","discount","distance","expiry","period","pickup","address"})text(root,key,"");
        View image=ClientContent.find(root,"Produto / cesta hortifruti");if(image instanceof ViewGroup)((ViewGroup)image).removeAllViews();}
    private void bindStores(){
        View slot=ClientContent.find(root,"Lojas / Mercados próximos");if(!(slot instanceof LinearLayout))return;
        LinearLayout list=(LinearLayout)slot;
        View mapSlot=ClientContent.find(root,"Mapa / viewport quadrado / futuro SupportMapFragment");
        if(mapSlot!=null&&mapSlot.getParent() instanceof ViewGroup){ViewGroup parent=(ViewGroup)mapSlot.getParent();int index=parent.indexOfChild(mapSlot);ViewGroup.LayoutParams params=mapSlot.getLayoutParams();parent.removeViewAt(index);
            map=new PartnerMapView(host);parent.addView(map,index,params);map.createMap(null);if(!map.isConfigured())status("Não foi possível carregar o mapa agora.");
            map.setOnStoreSelected(store->{new CatalogRepository(host).store(store.getId(),result->{if(active&&result.status==LoadState.Status.SUCCESS){flow.store=result.data;host.showScreen("2005:4391",true);}});});}
        catalog().stores().observe(fragment.getViewLifecycleOwner(),state->{if(state==null||expired(state.httpStatus))return;list.removeAllViews();
            list.addView(label("Lojas próximas"));if(state.status==LoadState.Status.LOADING)list.addView(label("Carregando lojas…"));
            if(state.status==LoadState.Status.ERROR){list.addView(label(state.message));button(list,"Tentar novamente",()->catalog().requestLocation());}
            List<PartnerStore> pins=new ArrayList<>();
            if(state.data!=null&&state.data.content!=null){for(CatalogStore store:state.data.content){
                PartnerStore partner=new PartnerStore(store);pins.add(partner);
                View card=LayoutInflater.from(host).inflate(R.layout.design_item_store,list,false);card.setVisibility(View.VISIBLE);text(card,"store",partner.getName());text(card,"address",partner.getAddressLabel());disableChildActions(card);
                card.setOnClickListener(v->{flow.store=store;host.showScreen("2005:4391",true);});card.setContentDescription(partner.getName());
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(12);list.addView(card,lp);
                if(!partner.hasValidLocation())list.addView(label("Esta loja ainda não tem posição no mapa."));
            }
                if(state.data.content.isEmpty())list.addView(label("Nenhuma loja parceira encontrada neste raio."));
                if(state.status!=LoadState.Status.LOADING&&state.data.number+1<state.data.totalPages)button(list,"Carregar mais lojas",()->catalog().loadNearbyStores(catalog().nextStorePage(),""));}
            if(map!=null){map.setUserPosition(catalog().storeLatitude(),catalog().storeLongitude());map.setStores(pins);}
        });
        int radius=Integer.parseInt(selectedRadius().split(" ")[0]);catalog().useStoreRadius(OfferRadius.fromKilometers(radius));
        if(catalog().stores().getValue()==null)catalog().requestLocation();
    }
    private void bindStoreDetail(){
        text(root,"store","");text(root,"address","");text(root,"region","");
        if(flow.store==null){status("Selecione uma loja na lista ou no mapa.");return;}
        String id=flow.store.id;status("Carregando loja…");new CatalogRepository(host).store(id,result->{
            if(!active||expired(result.httpStatus))return;
            if(result.status==LoadState.Status.SUCCESS&&result.data!=null){flow.store=result.data;hideStatus();PartnerStore store=new PartnerStore(result.data);
                text(root,"store",store.getName());text(root,"address",store.getAddressLabel());if(!store.hasValidLocation())status("A localização desta loja será aberta pelo endereço.");}
            else {flow.store=null;status(result.message);}
        });
    }
    private void bindPoints(){
        LoyaltyViewModel points=new ViewModelProvider(host).get(LoyaltyViewModel.class);
        View slot=ClientContent.find(root,"Fidelidade / Saldo confirmado");
        points.account().observe(fragment.getViewLifecycleOwner(),state->{if(state==null||expired(state.httpStatus))return;
            String value=state.data==null?"Saldo indisponível":LoyaltyPresentation.points(state.data.pointsBalance);text(root,"points",value);
            if(slot instanceof ViewGroup){ViewGroup group=(ViewGroup)slot;group.removeAllViews();group.addView(label(state.status==LoadState.Status.LOADING?"Carregando saldo…":value));
                if(state.data!=null)group.addView(label(LoyaltyPresentation.status(state.data)));
                if(state.status==LoadState.Status.ERROR){group.addView(label(state.message));button(group,"Tentar novamente",points::loadAccount);}}
        });if(points.account().getValue()==null)points.loadAccount();
    }
    private void bindHistory(){
        LoyaltyViewModel points=new ViewModelProvider(host).get(LoyaltyViewModel.class);ViewGroup list=body();if(list==null)return;
        points.transactions().observe(fragment.getViewLifecycleOwner(),state->{if(state==null||expired(state.httpStatus))return;list.removeAllViews();
            list.addView(label("Histórico de pontos · últimos seis meses"));
            if(state.status==LoadState.Status.LOADING)list.addView(label("Carregando movimentações…"));
            if(state.status==LoadState.Status.ERROR){list.addView(label(state.message));button(list,"Tentar novamente",points::loadRecentTransactions);}
            if(state.data!=null){if(state.data.isEmpty())list.addView(label("Nenhuma movimentação no período."));
                for(LoyaltyTransaction item:state.data)list.addView(label((item.description==null?"Movimentação de fidelidade":item.description)+"\n"+LoyaltyPresentation.points(item.points)+" · "+(item.createdAt==null?"":item.createdAt)));}
        });if(points.transactions().getValue()==null)points.loadRecentTransactions();
    }
    private void bindProfile(){
        profile=new ViewModelProvider(fragment).get(ProfileViewModel.class);
        profile.state().observe(fragment.getViewLifecycleOwner(),state->{
            for(EditText field:fields.values())field.setEnabled(state.kind==ProfileViewModel.State.Kind.READY||state.kind==ProfileViewModel.State.Kind.ERROR&&state.profile!=null);
            if(state.message!=null)status(state.message);
            else if(state.kind==ProfileViewModel.State.Kind.READY)hideStatus();
            if(state.kind==ProfileViewModel.State.Kind.LOADING||state.kind==ProfileViewModel.State.Kind.SAVING)status("Aguarde…");
            if(state.profile!=null){if(state.kind==ProfileViewModel.State.Kind.READY){
                setField("Nome completo",state.profile.fullName);setField("Telefone",state.profile.phone);
                flow.marketing=Boolean.TRUE.equals(state.profile.marketingConsent);fragment.renderMarketingConsent(flow.marketing);
                if("Perfil atualizado.".equals(state.message))status("Dados atualizados.");}}
        });profile.load();
    }
    private void saveProfile(){
        if(profile==null){status("Aguarde o carregamento do perfil.");return;}
        ProfileViewModel.State state=profile.state().getValue();
        if(state==null||state.profile==null){status("Não foi possível carregar o perfil. Tente novamente.");return;}
        boolean communication=screen.equals("2650:73471");String name=communication?state.profile.fullName:value("Nome completo").trim();String phone=communication?state.profile.phone:value("Telefone");
        if(name==null||name.isEmpty()||name.length()>150){status("Informe seu nome nos dados pessoais antes de salvar.");return;}
        if(!RegistrationFields.validPhone(phone)){status("Informe um telefone válido com DDD nos dados pessoais.");return;}
        profile.save(new UpdateFirebaseCustomerProfileRequest(name,RegistrationFields.phoneDigits(phone),flow.marketing));
    }
    private void bindFilters(){
        View found=ClientContent.find(root,"Ofertas / Filtro por loja");ViewGroup list=found instanceof ViewGroup?(ViewGroup)found:null;if(list==null)return;
        catalog().stores().observe(fragment.getViewLifecycleOwner(),state->renderFilters(list,state));
        LoadState<CatalogPage<CatalogStore>> stores=catalog().stores().getValue();
        if(stores==null)catalog().requestLocation();else renderFilters(list,stores);
    }
    private void renderFilters(ViewGroup list,LoadState<CatalogPage<CatalogStore>> stores){
        list.removeAllViews();list.addView(label("Filtrar por loja"));button(list,"Todas as lojas",()->{catalog().loadPromotions(0,null,"");host.showScreen("2005:3914",true);});
        if(stores==null||stores.status==LoadState.Status.LOADING){list.addView(label("Carregando lojas…"));return;}
        if(stores.status==LoadState.Status.ERROR){list.addView(label("Não foi possível carregar as lojas."));button(list,"Tentar novamente",catalog()::requestLocation);return;}
        if(stores.data==null||stores.data.content==null||stores.data.content.isEmpty()){list.addView(label("Nenhuma loja encontrada perto de você."));return;}
        for(CatalogStore store:stores.data.content)if(store!=null&&store.id!=null)button(list,store.name==null?"Loja parceira":store.name,()->{catalog().loadPromotions(0,store.id,"");host.showScreen("2005:3914",true);});
    }
    private void bindAssistant(){
        AssistantViewModel model=assistant();
        if(screen.equals("2557:30504")){View found=ClientContent.find(root,"Histórico / Conteúdo");ViewGroup list=found instanceof ViewGroup?(ViewGroup)found:null;if(list==null)return;list.removeAllViews();
            model.sessions().observe(fragment.getViewLifecycleOwner(),sessions->{if(sessions==null)return;for(ChatSessions.Session item:sessions)button(list,item.title==null?"Conversa":item.title,()->{model.openConversation(item.sessionId);host.showScreen("2055:5379",true);});if(sessions.isEmpty())list.addView(label("Nenhuma conversa encontrada."));});model.loadSessions();
            if(!model.isAvailable())list.addView(label("O assistente não está disponível no momento."));return;}
        model.state().observe(fragment.getViewLifecycleOwner(),state->{if(state==null||expired(state.httpStatus))return;ViewGroup group=body();if(!(group instanceof LinearLayout))return;
            LinearLayout messages=(LinearLayout)group;messages.removeAllViews();messages.setMinimumHeight(0);messages.setGravity(android.view.Gravity.TOP);
            if(state.data!=null)for(ChatMessage message:state.data)fragment.addBubble(messages,message.getText(),message.getSender()==ChatMessage.Sender.USER);
            if(state.status==LoadState.Status.LOADING)messages.addView(label("Lô está respondendo…"));
            if(state.status==LoadState.Status.ERROR)messages.addView(label(state.message));
            if(state.status==LoadState.Status.SUCCESS&&(state.data==null||state.data.isEmpty()))messages.addView(label("Olá! Como posso ajudar?"));
        });
    }
    public void sendChat(){EditText composer=fields.get("Mensagem");if(composer==null)return;String message=composer.getText().toString().trim();if(message.isEmpty())return;
        if(assistant().send(message))composer.setText("");else status(assistant().isBusy()?"Aguarde a resposta anterior.":"O assistente está indisponível. Sua mensagem não foi enviada.");}
    private void sendSuggestion(String message){setField("Mensagem",message);sendChat();}
    private boolean expired(int status){if(status!=401)return false;session.clearClientSession();host.showScreen("2028:4492",false);return true;}
    private void text(View parent,String binding,String value){if(ClientContent.tag(parent).optString("binding").equals(binding)&&parent instanceof TextView){TextView text=(TextView)parent;text.setText(value==null?"":value);text.setVisibility(value==null||value.isEmpty()?View.GONE:View.VISIBLE);}if(parent instanceof ViewGroup)for(int i=0;i<((ViewGroup)parent).getChildCount();i++)text(((ViewGroup)parent).getChildAt(i),binding,value);}
    private void disableChildActions(View view){view.setOnClickListener(null);view.setClickable(false);view.setFocusable(false);if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)disableChildActions(((ViewGroup)view).getChildAt(i));}
    private TextView label(String value){return ClientContent.label(host,value==null?"Não foi possível carregar os dados.":value,16);}
    private ViewGroup body(){View scroll=root.findViewById(R.id.design_scroll);if(scroll instanceof ViewGroup&&((ViewGroup)scroll).getChildCount()>0&&((ViewGroup)scroll).getChildAt(0) instanceof ViewGroup)return (ViewGroup)((ViewGroup)scroll).getChildAt(0);if(root instanceof ViewGroup&&((ViewGroup)root).getChildCount()>0&&((ViewGroup)root).getChildAt(0) instanceof LinearLayout)return (ViewGroup)((ViewGroup)root).getChildAt(0);return root instanceof ViewGroup?(ViewGroup)root:null;}
    private void setAuthActionsEnabled(View view,boolean enabled){
        String action=ClientContent.tag(view).optString("action").trim();
        if(Arrays.asList("Entrar","Continuar com Google","Confirmar com Google","Criar conta","Continuar","Revisar cadastro","Enviar link").contains(action)){
            view.setEnabled(enabled);view.setAlpha(enabled?1f:.55f);
        }
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)setAuthActionsEnabled(((ViewGroup)view).getChildAt(i),enabled);
    }
    private void renderAuthLoading(AuthViewModel.State state){
        boolean busy=auth!=null&&auth.isBusy();
        if(!busy){if(authLoading!=null)authLoading.setVisibility(View.GONE);return;}
        if(authLoading==null||authLoading.getParent()==null){
            View actions=isRegistration()?findAction(root,"Criar conta"):ClientContent.find(root,"Entrar / Ações");
            ViewGroup parent=actions!=null&&actions.getParent() instanceof ViewGroup?(ViewGroup)actions.getParent():body();
            if(parent==null)return;
            authLoading=new LinearLayout(host);authLoading.setOrientation(LinearLayout.HORIZONTAL);
            authLoading.setGravity(android.view.Gravity.CENTER_VERTICAL);authLoading.setPadding(dp(16),dp(14),dp(16),dp(14));
            android.graphics.drawable.GradientDrawable loadingBackground=new android.graphics.drawable.GradientDrawable();
            loadingBackground.setColor(androidx.core.content.ContextCompat.getColor(host,R.color.design_canvas));
            loadingBackground.setCornerRadius(dp(16));loadingBackground.setStroke(dp(1),android.graphics.Color.parseColor("#D8E0EC"));
            authLoading.setBackground(loadingBackground);
            android.widget.ProgressBar spinner=new android.widget.ProgressBar(host,null,android.R.attr.progressBarStyleSmall);
            spinner.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(host,R.color.design_primary)));
            authLoading.addView(spinner,new LinearLayout.LayoutParams(dp(28),dp(28)));
            authLoadingMessage=ClientContent.label(host,"",15);authLoadingMessage.setPadding(dp(12),0,0,0);
            authLoadingMessage.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
            authLoading.addView(authLoadingMessage,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
            int index=actions==null?-1:parent.indexOfChild(actions);
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
            params.bottomMargin=dp(12);parent.addView(authLoading,Math.max(0,index),params);
        }
        String message=state.kind==AuthViewModel.State.Kind.SELECTING_GOOGLE
                ?"Abrindo o Google. Escolha a conta para continuar."
                :state.message;
        if(message==null||message.trim().isEmpty())message=isRegistration()
                ?"Estamos processando e salvando seu cadastro…"
                :"Estamos entrando na sua conta…";
        authLoadingMessage.setText(message);authLoading.setVisibility(View.VISIBLE);
    }
    private View findAction(View view,String expected){
        if(ClientContent.tag(view).optString("action").trim().equals(expected))return view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){
            View found=findAction(((ViewGroup)view).getChildAt(i),expected);
            if(found!=null)return found;
        }
        return null;
    }
    private void status(String message){
        if(message==null||message.isEmpty())return;
        ViewGroup parent=body();if(parent==null)return;
        if(feedback==null||feedback.getParent()==null){
            feedback=label(message);feedback.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
            View actions=ClientContent.find(root,"Entrar / Ações");
            if(auth!=null&&actions!=null&&actions.getParent() instanceof ViewGroup){
                ViewGroup container=(ViewGroup)actions.getParent();container.addView(feedback,container.indexOfChild(actions),new ViewGroup.LayoutParams(-1,-2));
            }else parent.addView(feedback,new ViewGroup.LayoutParams(-1,-2));
        }
        feedback.setText(message);feedback.setVisibility(View.VISIBLE);
        if(auth!=null)feedback.post(()->{if(active)feedback.requestRectangleOnScreen(new android.graphics.Rect(0,0,feedback.getWidth(),feedback.getHeight()),false);});
    }
    private void hideStatus(){if(feedback!=null)feedback.setVisibility(View.GONE);}
    private void notice(String message){android.widget.Toast.makeText(host,message,android.widget.Toast.LENGTH_SHORT).show();}
    private void button(ViewGroup parent,String value,Runnable action){if(parent==null)return;TextView button=label(value);button.setGravity(android.view.Gravity.CENTER);button.setMinHeight(dp(48));button.setTextColor(androidx.core.content.ContextCompat.getColor(host,R.color.design_on_primary));button.setBackgroundResource(R.drawable.design_02b1e49a85b6);button.setPadding(dp(16),dp(12),dp(16),dp(12));button.setClickable(true);button.setFocusable(true);button.setOnClickListener(v->action.run());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(12);parent.addView(button,lp);}
    private String money(BigDecimal value){return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(value);}
    private int dp(int value){return Math.round(value*host.getResources().getDisplayMetrics().density);}
    public void start(){if(map!=null)map.startMap();}
    public void resume(){
        if(!ClientRoutes.isPublic(screenName)&&!session.canBrowse()){host.showScreen("2028:4492",false);return;}
        if(!ClientRoutes.isPublic(screenName)&&!ClientRoutes.isLocationAccess(screenName)&&!session.isDemoMode()
            &&(!ClientAccessGuard.hasLocationPermission(host)||!ClientAccessGuard.isLocationEnabled(host))){host.showScreen("2557:30312",false);return;}
        if(map!=null)map.resumeMap();
    }
    public void pause(){if(map!=null)map.pauseMap();}
    public void stop(){if(map!=null)map.stopMap();}
    public void destroy(){active=false;if(googleRequest!=null){if(auth!=null)auth.googleSignInFailed("Seleção da conta interrompida. Tente novamente.");googleRequest.cancel();googleRequest=null;}if(map!=null){map.releaseMap();map=null;}fields.clear();}
}
