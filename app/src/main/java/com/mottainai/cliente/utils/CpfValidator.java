package com.mottainai.cliente.utils;

public final class CpfValidator {
    private CpfValidator() { }

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
