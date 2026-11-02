package com.mottainai.cliente.activities;
import android.os.Bundle;
import com.mottainai.cliente.MainActivity;
/** Ponto de entrada de compatibilidade; os fluxos usam a navegação e o tema compartilhados do aplicativo. */
public class LoginClienteActivity extends MainActivity {

    /** Inicializa a tela, recupera seu estado e prepara a navegação de entrada. */
    @Override protected void onCreate(Bundle state) {getIntent().putExtra("screen","2028:4492");super.onCreate(state);}
}
