package com.mottainai.cliente.design;

/** Apenas autenticação, introdução e explicações de acesso ao dispositivo são rotas públicas. */
public final class ClientRoutes {
    /** Impede a criação de instâncias desta classe utilitária. */
    private ClientRoutes() {}
    /** Identifica as telas que podem ser abertas antes da autenticação. */
    public static boolean isPublic(String name) {
        return name.startsWith("Cliente / Entrar") || name.startsWith("Cliente / Cadastro /")
                || name.startsWith("Cliente / Concluir cadastro /")
                || name.startsWith("Cliente / Recuperar senha /")
                || name.startsWith("Cliente / Apresentação /")
                || name.startsWith("Cliente / Abertura /")
                || name.startsWith("Cliente / Acesso /");
    }
    /** Identifica as telas responsáveis pela liberação da localização. */
    public static boolean isLocationAccess(String name) {
        return name.startsWith("Cliente / Acesso /") || name.startsWith("Cliente / Localização /");
    }
}
