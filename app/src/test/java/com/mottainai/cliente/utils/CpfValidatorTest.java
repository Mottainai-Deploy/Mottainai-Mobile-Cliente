package com.mottainai.cliente.utils;

import org.junit.Test;

import static org.junit.Assert.*;

/** Reúne os cenários de teste de CpfValidator. */
public class CpfValidatorTest {
    /** Verifica a aceitação de CPFs com dígitos verificadores válidos. */
    @Test public void acceptsValidCheckDigits() {
        assertTrue(CpfValidator.isValid("52998224725"));
        assertTrue(CpfValidator.isValid("11144477735"));
    }

    /** Verifica a preservação dos zeros iniciais do valor informado. */
    @Test public void preservesLeadingZeros() {
        assertTrue(CpfValidator.isValid("01234567890"));
    }

    /** Verifica a rejeição de qualquer dígito verificador incorreto do CPF. */
    @Test public void rejectsEitherIncorrectCheckDigit() {
        assertFalse(CpfValidator.isValid("52998224715"));
        assertFalse(CpfValidator.isValid("52998224724"));
    }

    /** Verifica a rejeição de CPFs formados por dígitos repetidos. */
    @Test public void rejectsAllRepeatedDigitSequences() {
        for (int digit = 0; digit <= 9; digit++) {
            assertFalse(CpfValidator.isValid(String.valueOf(digit).repeat(11)));
        }
    }

    /** Verifica a rejeição de valores ausentes ou com quantidade incorreta de dígitos. */
    @Test public void rejectsMissingOrIncorrectLength() {
        assertFalse(CpfValidator.isValid(null));
        assertFalse(CpfValidator.isValid(""));
        assertFalse(CpfValidator.isValid("5299822472"));
        assertFalse(CpfValidator.isValid("529982247250"));
    }

    /** Verifica se a validação recebe apenas dígitos numéricos após normalizar o formulário. */
    @Test public void expectsOnlyAsciiDigitsAfterFormNormalization() {
        assertFalse(CpfValidator.isValid("5299822472a"));
        assertFalse(CpfValidator.isValid("529.982.247-25"));
        assertFalse(CpfValidator.isValid("５２９９８２２４７２５"));
    }
}
