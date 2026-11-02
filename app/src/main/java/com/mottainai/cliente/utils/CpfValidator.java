package com.mottainai.cliente.utils;

/** Valida apenas formato e dígitos verificadores locais; não comprova identidade nem situação cadastral. */
public final class CpfValidator {
    /** Impede a criação de instâncias desta classe utilitária. */
    private CpfValidator() { }

    /** Valida o dado recebido de acordo com as regras deste componente. */
    public static boolean isValid(String digits) {
        if (digits == null || !digits.matches("[0-9]{11}")
                || digits.matches("([0-9])\\1{10}")) return false;
        for (int checkIndex = 9; checkIndex <= 10; checkIndex++) {
            int sum = 0;
            for (int index = 0; index < checkIndex; index++) {
                sum += (digits.charAt(index) - '0') * (checkIndex + 1 - index);
            }
            int remainder = sum % 11;
            int expected = remainder < 2 ? 0 : 11 - remainder;
            if (digits.charAt(checkIndex) - '0' != expected) return false;
        }
        return true;
    }
}
