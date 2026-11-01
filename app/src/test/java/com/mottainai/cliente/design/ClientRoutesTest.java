package com.mottainai.cliente.design;

import org.junit.Test;
import static org.junit.Assert.*;

public class ClientRoutesTest {
    @Test public void authenticationAndSetupDoNotRequireAnExistingSqlProfile() {
        for(String name:new String[]{"Cliente / Entrar", "Cliente / Cadastro / Região",
                "Cliente / Concluir cadastro / Dados pessoais", "Cliente / Recuperar senha / E-mail enviado",
                "Cliente / Apresentação / Ofertas perto de você", "Cliente / Acesso / Permitir localização"})
            assertTrue(name,ClientRoutes.isPublic(name));
    }
    @Test public void businessAndAccountScreensRequireAuthentication() {
        for(String name:new String[]{"Cliente / Início", "Cliente / Ofertas", "Cliente / Loja / Detalhes",
                "Cliente / Perfil", "Cliente / Configurações da conta", "Cliente / Alterar e-mail",
                "Cliente / Pontos de impacto / Histórico", "Cliente / Conversa com a Lô / Conversas"})
            assertFalse(name,ClientRoutes.isPublic(name));
    }
}
