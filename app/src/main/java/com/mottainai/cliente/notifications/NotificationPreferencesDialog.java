package com.mottainai.cliente.notifications;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mottainai.cliente.R;
import com.mottainai.cliente.design.ClientContent;
import com.mottainai.cliente.design.LoadingStateView;

/** Controla os avisos em um único popup, sem criar uma página de configurações ou uma caixa de entrada. */
public final class NotificationPreferencesDialog extends DialogFragment {
    private static final String TAG="notification_preferences";
    private String owner;
    private TextView message,error;
    private LoadingStateView loading;
    private boolean busy,openSettings,initialEnabled;
    private Boolean desired;
    private final ActivityResultLauncher<String> permission=registerForActivityResult(
        new ActivityResultContracts.RequestPermission(),allowed->{
            if(allowed)save(true);
            else fail("Para receber avisos, permita as notificações do Mottainai no celular.",true);
        });

    /** Evita abrir dois popups ou apresentar uma escolha fora da sessão autenticada. */
    public static void show(AppCompatActivity activity){
        if(activity.isFinishing()||activity.isDestroyed()||activity.getSupportFragmentManager().isStateSaved()
            ||activity.getSupportFragmentManager().findFragmentByTag(TAG)!=null)return;
        if(RedisNotificationRepository.owner(activity)==null)return;
        new NotificationPreferencesDialog().showNow(activity.getSupportFragmentManager(),TAG);
    }
    /** Usa o tema Material, as cores e a tipografia já utilizadas pelo aplicativo. */
    @NonNull @Override public Dialog onCreateDialog(Bundle state){
        owner=RedisNotificationRepository.owner(requireContext());
        initialEnabled=CustomerNotifications.enabled(requireContext());
        LinearLayout content=new LinearLayout(requireContext());content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24),dp(4),dp(24),dp(4));
        message=ClientContent.label(requireContext(),description(),15);message.setPadding(0,dp(4),0,dp(12));
        content.addView(message,new LinearLayout.LayoutParams(-1,-2));
        error=ClientContent.label(requireContext(),"",14);error.setTextColor(ContextCompat.getColor(requireContext(),R.color.accent_red));
        error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);error.setVisibility(View.GONE);
        content.addView(error,new LinearLayout.LayoutParams(-1,-2));
        loading=new LoadingStateView(requireContext(),"Salvando sua preferência…",false);loading.setVisibility(View.GONE);
        content.addView(loading,new LinearLayout.LayoutParams(-1,-2));
        GradientDrawable background=new GradientDrawable();background.setColor(ContextCompat.getColor(requireContext(),R.color.surface_card));
        background.setCornerRadius(dp(24));
        return new MaterialAlertDialogBuilder(requireContext()).setTitle("Avisos de ofertas").setBackground(background)
            .setView(content).setPositiveButton(action(),null).setNegativeButton("Cancelar",null).create();
    }
    /** Mantém o popup aberto durante a gravação e só confirma depois da resposta do servidor. */
    @Override public void onStart(){
        super.onStart();AlertDialog dialog=(AlertDialog)requireDialog();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(view->choose());
        TextView title=dialog.findViewById(androidx.appcompat.R.id.alertTitle);
        if(title!=null){title.setTextColor(ContextCompat.getColor(requireContext(),R.color.design_text));
            title.setFontVariationSettings("'wght' 650, 'opsz' 14");}
    }
    /** Apresenta o estado atual sem informações internas sobre os serviços. */
    private String description(){return initialEnabled
        ?"Os avisos estão ativados. Você recebe ofertas de lojas próximas de você."
        :"Os avisos estão desativados. Ative para receber ofertas de lojas próximas de você.";}
    /** Usa uma ação que explicita o efeito da escolha. */
    private String action(){return initialEnabled?"Desativar avisos":"Ativar avisos";}
    /** Pede autorização do Android apenas quando necessária para ativar a entrega. */
    private void choose(){
        if(busy||!RedisNotificationRepository.owns(requireContext(),owner))return;
        if(openSettings){startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE,requireContext().getPackageName()));return;}
        boolean next=desired!=null?desired:!initialEnabled;desired=next;
        if(next&&!CustomerNotifications.permitted(requireContext())){
            if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.POST_NOTIFICATIONS)
                !=android.content.pm.PackageManager.PERMISSION_GRANTED)permission.launch(Manifest.permission.POST_NOTIFICATIONS);
            else fail("Permita as notificações do Mottainai nas configurações do celular.",true);
            return;
        }
        save(next);
    }
    /** Salva a adesão por conta e mantém um indicador de carregamento durante a operação. */
    private void save(boolean next){
        if(!isAdded()||busy)return;busy=true;setCancelable(false);loading.setVisibility(View.VISIBLE);error.setVisibility(View.GONE);
        AlertDialog dialog=(AlertDialog)requireDialog();dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);
        android.content.Context app=requireContext().getApplicationContext();
        CustomerNotifications.setEnabled(app,next,saved->{
            if(!isAdded()||getDialog()==null)return;
            if(!RedisNotificationRepository.owns(app,owner)){dismissAllowingStateLoss();return;}
            busy=false;setCancelable(true);loading.setVisibility(View.GONE);
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(true);
            if(saved){
                android.widget.Toast.makeText(app,next?"Avisos ativados":"Avisos desativados",android.widget.Toast.LENGTH_SHORT).show();
                AppCompatActivity activity=(AppCompatActivity)requireActivity();dismiss();
                if(next)NotificationBackgroundAccess.offer(activity);
            }else fail("Não conseguimos salvar sua escolha. Confira a conexão e tente novamente.",false);
        });
    }
    /** Oferece nova tentativa ou o ajuste de permissão, sem exibir mensagens técnicas. */
    private void fail(String text,boolean settings){
        if(!isAdded()||getDialog()==null)return;openSettings=settings;error.setText(text);error.setVisibility(View.VISIBLE);
        ((AlertDialog)requireDialog()).getButton(DialogInterface.BUTTON_POSITIVE).setText(settings?"Abrir configurações":"Tentar novamente");
    }
    /** Revalida a autorização ao voltar das configurações nativas do aparelho. */
    @Override public void onResume(){super.onResume();if(openSettings&&CustomerNotifications.permitted(requireContext())){
        openSettings=false;error.setVisibility(View.GONE);((AlertDialog)requireDialog()).getButton(DialogInterface.BUTTON_POSITIVE).setText("Ativar avisos");}}
    /** Converte o espaçamento para a densidade do aparelho. */
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}