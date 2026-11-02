package com.mottainai.cliente.activities;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.mottainai.cliente.MainActivity;
/** A tela inicial delega para o host compartilhado de navegação tematizada. */
public class OnboardingActivity extends AppCompatActivity {
    /** Inicializa a tela, recupera seu estado e prepara a navegação de entrada. */
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        startActivity(new Intent(this,MainActivity.class));
        finish();
    }
}
