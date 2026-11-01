package com.mottainai.cliente.design;

public final class ClientRoutes {
    private ClientRoutes() {}
    public static boolean isPublic(String name) {
        return name.startsWith("Cliente / Entrar") || name.startsWith("Cliente / Cadastro /")
                || name.startsWith("Cliente / Concluir cadastro /")
                || name.startsWith("Cliente / Recuperar senha /")
                || name.startsWith("Cliente / Apresentação /")
                || name.startsWith("Cliente / Abertura /")
                || name.startsWith("Cliente / Acesso /");
    }
    public static boolean isLocationAccess(String name) {
        return name.startsWith("Cliente / Acesso /") || name.startsWith("Cliente / Localização /");
    }
}
