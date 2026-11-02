package com.mottainai.cliente.utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.telephony.PhoneNumberFormattingTextWatcher;

import java.util.concurrent.CompletableFuture;

/** Representa InputMasks, responsável por as operações auxiliares deste recurso. */
public final class InputMasks {
    /** Impede a criação de instâncias desta classe utilitária. */
    private InputMasks() {}

    /** O formatador nativo carrega metadados de telefone fora da thread da interface. Anexe e formate na thread da visualização. */
    public static void applyPhone(EditText field) {
        CompletableFuture.supplyAsync(() -> new PhoneNumberFormattingTextWatcher("BR"))
                .thenAccept(watcher -> field.post(() -> {
                    field.addTextChangedListener(watcher);
                    watcher.afterTextChanged(field.getText());
                }));
    }

    /** Aplica a configuração visual ou de edição prevista pelo componente. */
    public static void apply(EditText field, String pattern) {
        field.addTextChangedListener(new TextWatcher() {
            private boolean formatting;
            /** Recebe o aviso anterior à alteração do texto; mantém o comportamento previsto pelo observador. */
            @Override public void beforeTextChanged(CharSequence value, int start, int count, int after) {}
            /** Recebe a alteração do campo e atualiza o estado associado ao texto. */
            @Override public void onTextChanged(CharSequence value, int start, int before, int count) {}
            /** Processa o texto final do campo após a edição do usuário. */
            @Override public void afterTextChanged(Editable value) {
                if (formatting) return;
                String original = value.toString();
                String formatted = RegistrationFields.mask(original, pattern);
                if (formatted.equals(original)) return;
                int cursor = Math.max(0, Math.min(field.getSelectionStart(), original.length()));
                int digitsBeforeCursor = RegistrationFields.digits(original.substring(0, cursor)).length();
                formatting = true;
                try {
                    field.setText(formatted);
                    int position = 0, digits = 0;
                    while (position < formatted.length() && digits < digitsBeforeCursor) {
                        if (Character.isDigit(formatted.charAt(position))) digits++;
                        position++;
                    }
                    field.setSelection(position);
                } finally {
                    formatting = false;
                }
            }
        });
    }
}
