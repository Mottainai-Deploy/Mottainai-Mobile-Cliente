package com.mottainai.cliente.utils;

import org.junit.Test;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import static org.junit.Assert.*;

/** Reúne os cenários de teste de RegistrationFields. */
public class RegistrationFieldsTest {
    /** Verifica a máscara de CPF sem alterar os dígitos informados. */
    @Test public void masksCpfWithoutChangingItsDigits() {
        String display = RegistrationFields.mask("12345678901", "###.###.###-##");
        assertEquals("123.456.789-01", display);
        assertEquals("12345678901", RegistrationFields.digits(display));
        assertEquals("123.4", RegistrationFields.mask("1234", "###.###.###-##"));
    }
    /** Verifica a preservação de zeros e a colagem no campo de CEP. */
    @Test public void cepPreservesLeadingZeroAndSupportsPaste() {
        assertEquals("01001-000", RegistrationFields.mask("01001-000", "#####-###"));
        assertEquals("", RegistrationFields.mask("", "#####-###"));
    }
    /** Verifica a aceitação de telefones brasileiros fixos e celulares. */
    @Test public void phoneAcceptsBrazilianMobileAndLandlineStructure() {
        assertTrue(RegistrationFields.validPhone("(11) 98765-4321"));
        assertTrue(RegistrationFields.validPhone("(11) 3456-7890"));
        assertTrue(RegistrationFields.validPhone("+55 (11) 98765-4321"));
    }
    /** Verifica a rejeição de letras, números curtos, estruturas inválidas e dígitos repetidos no telefone. */
    @Test public void phoneRejectsLettersShortInvalidStructureAndRepeatedDigits() {
        for (String invalid : new String[]{null, "", "abc", "119999", "1198765432",
                "11887654321", "00987654321", "11111111111", "abc11987654321",
                "++5511987654321", "11+987654321", "+11987654321"}) {
            assertFalse(String.valueOf(invalid), RegistrationFields.validPhone(invalid));
        }
    }
    /** Verifica se o telefone enviado contém somente o número nacional normalizado. */
    @Test public void nationalPayloadDoesNotContainDisplayPunctuationOrCountryPrefix() {
        assertEquals("11987654321", RegistrationFields.phoneDigits("+55 (11) 98765-4321"));
        assertEquals("1134567890", RegistrationFields.phoneDigits("(11) 3456-7890"));
    }
    /** Verifica a apresentação brasileira da data e o envio no formato ISO. */
    @Test public void dateUsesBrazilianDisplayAndIsoPayload() {
        assertEquals("2000-02-29", RegistrationFields.birthDateIso("29/02/2000"));
        assertNull(RegistrationFields.birthDateIso(""));
    }
    /** Verifica a rejeição de datas inexistentes no calendário. */
    @Test(expected = DateTimeParseException.class) public void impossibleDateIsRejected() {
        RegistrationFields.birthDateIso("31/02/2000");
    }
    /** Verifica a rejeição de 29 de fevereiro fora de ano bissexto. */
    @Test(expected = DateTimeParseException.class) public void invalidLeapYearIsRejected() {
        RegistrationFields.birthDateIso("29/02/2001");
    }
    /** Verifica a rejeição de datas de nascimento futuras. */
    @Test(expected = IllegalArgumentException.class) public void futureDateIsRejected() {
        RegistrationFields.birthDateIso(LocalDate.now().plusDays(1)
                .format(DateTimeFormatter.ofPattern("dd/MM/uuuu")));
    }
}
