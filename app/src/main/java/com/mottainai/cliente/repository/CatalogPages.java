package com.mottainai.cliente.repository;

import com.mottainai.cliente.network.dto.CatalogPage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/** Representa CatalogPages, responsável por o acesso e o tratamento dos dados do aplicativo. */
public final class CatalogPages {
    /** Impede a criação de instâncias desta classe utilitária. */
    private CatalogPages() { }

    /** Combina as páginas recebidas sem duplicar os registros já carregados. */
    public static <T> CatalogPage<T> merge(CatalogPage<T> previous, CatalogPage<T> next,
                                            Function<T, String> identity) {
        if (next.number < 0 || (next.number > 0
                && (previous == null || next.number > previous.number + 1))) {
            throw new IllegalArgumentException("Catalog pages must be loaded in order");
        }
        Map<String, T> items = new LinkedHashMap<>();
        if (next.number > 0 && previous != null) {
            for (T item : previous.items()) items.put(identity.apply(item), item);
        }
        for (T item : next.items()) {
            if (item == null) continue;
            String id = identity.apply(item);
            if (id != null && !id.trim().isEmpty()) items.put(id, item);
        }
        CatalogPage<T> result = new CatalogPage<>();
        result.content = new ArrayList<>(items.values());
        result.number = next.number;
        result.size = next.size;
        result.totalElements = next.totalElements;
        result.totalPages = next.totalPages;
        return result;
    }
}
