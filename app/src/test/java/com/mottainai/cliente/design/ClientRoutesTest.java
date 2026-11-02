package com.mottainai.cliente.design;

import org.junit.Test;
import static org.junit.Assert.*;

/** Reúne os cenários de teste de ClientRoutes. */
public class ClientRoutesTest {
    /** Verifica se as etapas de autenticação e configuração dispensam perfil SQL prévio. */
    @Test public void authenticationAndSetupDoNotRequireAnExistingSqlProfile() {
        for(String name:new String[]{"Cliente / Entrar", "Cliente / Cadastro / Região",
                "Cliente / Concluir cadastro / Dados pessoais", "Cliente / Recuperar senha / E-mail enviado",
                "Cliente / Apresentação / Ofertas perto de você", "Cliente / Acesso / Permitir localização"})
            assertTrue(name,ClientRoutes.isPublic(name));
    }
    /** Verifica se as telas funcionais e da conta exigem autenticação. */
    @Test public void businessAndAccountScreensRequireAuthentication() {
        for(String name:new String[]{"Cliente / Início", "Cliente / Ofertas", "Cliente / Loja / Detalhes",
                "Cliente / Perfil", "Cliente / Configurações da conta", "Cliente / Alterar e-mail",
                "Cliente / Pontos de impacto / Histórico", "Cliente / Conversa com a Lô / Conversas"})
            assertFalse(name,ClientRoutes.isPublic(name));
    }
}
