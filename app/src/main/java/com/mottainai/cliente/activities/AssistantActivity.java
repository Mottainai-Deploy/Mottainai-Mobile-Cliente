package com.mottainai.cliente.activities;
import android.os.Bundle;
import com.mottainai.cliente.MainActivity;
/** Ponto de entrada de compatibilidade que usa o mesmo tema nativo e host de navegação. */
public class AssistantActivity extends MainActivity {
    /** Inicializa a tela, recupera seu estado e prepara a navegação de entrada. */
    @Override protected void onCreate(Bundle state) { getIntent().putExtra("screen","2055:5379");super.onCreate(state); }
}
