package com.luistech.socorre.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.luistech.socorre.domain.caso_de_uso.ValidadorTelefone;

import org.junit.Test;

public class ValidadorTelefoneTest {

    @Test
    public void removeFormatacao() {
        assertEquals("18999990000", ValidadorTelefone.normalizar("(18) 99999-0000"));
    }

    @Test
    public void mantemCodigoInternacional() {
        assertEquals("+5518999990000", ValidadorTelefone.normalizar("+55 18 99999-0000"));
    }

    @Test
    public void rejeitaInvalidos() {
        assertNull(ValidadorTelefone.normalizar(null));
        assertNull(ValidadorTelefone.normalizar("123"));
        assertNull(ValidadorTelefone.normalizar("1234567890123456"));
        assertNull(ValidadorTelefone.normalizar("abc"));
    }

    @Test
    public void mascaraBrasileira() {
        assertEquals("(18) 99999-0000", ValidadorTelefone.mascarar("18999990000"));
        assertEquals("(18) 3333-0000", ValidadorTelefone.mascarar("1833330000"));
        assertEquals("(18) 99999-0000", ValidadorTelefone.mascarar("+55 18 99999-0000"));
        assertEquals("(18) 9", ValidadorTelefone.mascarar("189"));
        assertEquals("(1", ValidadorTelefone.mascarar("1"));
        assertEquals("", ValidadorTelefone.mascarar("("));
        assertEquals("18999990000", ValidadorTelefone.normalizar(ValidadorTelefone.mascarar("18999990000")));
    }
}
