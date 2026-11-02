package com.mottainai.cliente.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Locale;

/** Formatos brasileiros de exibição; cargas de negócio mantêm dígitos e datas ISO. */
public final class RegistrationFields {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);
    /** Impede a criação de instâncias desta classe utilitária. */
    private RegistrationFields() {}

    /** Extrai somente os dígitos do valor informado. */
    public static String digits(String value) {
        return value == null ? "" : value.replaceAll("[^0-9]", "");
    }

    /** Normaliza os dígitos do telefone para o formato esperado no cadastro. */
    public static String phoneDigits(String value) {
        String digits = digits(value);
        return (digits.length() == 12 || digits.length() == 13) && digits.startsWith("55")
                ? digits.substring(2) : digits;
    }

    /** Valida a estrutura do telefone informado no formulário. */
    public static boolean validPhone(String value) {
        if (value == null || !value.trim().matches("\\+?[0-9().\\s-]+")) return false;
        String originalDigits = digits(value);
        if (value.trim().startsWith("+") && (!originalDigits.startsWith("55")
                || originalDigits.length() < 12 || originalDigits.length() > 13)) return false;
        String digits = phoneDigits(value);
        if (digits.matches("(\\d)\\1+")) return false;
        return digits.matches("[1-9][0-9](?:[2-5][0-9]{7}|9[0-9]{8})");
    }

    /** Aplica a máscara de apresentação ao valor do campo. */
    public static String mask(String value, String pattern) {
        String digits = digits(value);
        StringBuilder result = new StringBuilder();
        int index = 0;
        for (int i = 0; i < pattern.length() && index < digits.length(); i++) {
            char character = pattern.charAt(i);
            result.append(character == '#' ? digits.charAt(index++) : character);
        }
        return result.toString();
    }

    /** Valida a data de nascimento e converte para o formato enviado ao serviço. */
    public static String birthDateIso(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        LocalDate date = LocalDate.parse(value.trim(), DATE);
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("A data de nascimento não pode ser futura.");
        }
        return date.toString();
    }
}
