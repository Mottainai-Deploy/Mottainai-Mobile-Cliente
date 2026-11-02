package com.mottainai.cliente.network.dto;

import java.util.Collections;
import java.util.List;

/** Representa CatalogPage, responsável por a comunicação com os serviços externos. */
public class CatalogPage<T> {
    public List<T> content;
    public int number;
    public int size;
    public long totalElements;
    public int totalPages;

    /** Retorna os itens disponíveis na resposta paginada. */
    public List<T> items() {
        return content == null ? Collections.emptyList() : content;
    }
}
