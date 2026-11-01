package com.mottainai.cliente.utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.telephony.PhoneNumberFormattingTextWatcher;

import java.util.concurrent.CompletableFuture;

public final class InputMasks {
    private InputMasks() {}

    public static void applyPhone(EditText field) {
        CompletableFuture.supplyAsync(() -> new PhoneNumberFormattingTextWatcher("BR"))
                .thenAccept(watcher -> field.post(() -> {
                    field.addTextChangedListener(watcher);
                    watcher.afterTextChanged(field.getText());
                }));
    }

    public static void apply(EditText field, String pattern) {
        field.addTextChangedListener(new TextWatcher() {
            private boolean formatting;
            @Override public void beforeTextChanged(CharSequence value, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence value, int start, int before, int count) {}
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
