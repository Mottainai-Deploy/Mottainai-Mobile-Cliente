package com.mottainai.cliente.repository;

import com.mottainai.cliente.network.dto.CatalogPage;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/** Reúne os cenários de teste de CatalogPages. */
public class CatalogPagesTest {
    /** Verifica se itens e páginas repetidos não duplicam os resultados. */
    @Test public void repeatedItemsAndRetriedPagesDoNotDuplicateResults() {
        CatalogPage<String> first = page(0, "a", "b");
        CatalogPage<String> second = page(1, "b", "c");
        CatalogPage<String> merged = CatalogPages.merge(first, second, item -> item);
        CatalogPage<String> retried = CatalogPages.merge(merged, second, item -> item);
        assertEquals(Arrays.asList("a", "b", "c"), merged.items());
        assertEquals(merged.items(), retried.items());
        assertEquals(Arrays.asList("a", "b"), first.items());
        assertEquals(1, merged.number);
        assertEquals(4L, merged.totalElements);
    }

    /** Verifica a substituição dos resultados ao mudar os filtros e carregar a primeira página. */
    @Test public void firstPageReplacesResultsAfterChangingFilter() {
        CatalogPage<String> result = CatalogPages.merge(page(1, "old"), page(0, "new"), item -> item);
        assertEquals(Arrays.asList("new"), result.items());
    }

    /** Verifica se a paginação impede saltar páginas ainda não carregadas. */
    @Test(expected = IllegalArgumentException.class)
    public void cannotSkipUnloadedPages() {
        CatalogPages.merge(page(0, "a"), page(2, "c"), item -> item);
    }

    /** Verifica se registros sem identificação válida não permitem abrir detalhes. */
    @Test public void missingAndUnidentifiableRecordsAreNotNavigable() {
        CatalogPage<String> result = CatalogPages.merge(null, page(0, "a", null, ""), item -> item);
        assertEquals(Arrays.asList("a"), result.items());
        CatalogPage<String> empty = new CatalogPage<>();
        assertEquals(0, CatalogPages.merge(result, empty, item -> item).items().size());
    }

    /** Verifica a preservação dos dados já carregados durante novas consultas e falhas. */
    @Test public void loadingAndFailureKeepPreviouslyLoadedData() {
        CatalogPage<String> data = page(0, "a");
        assertSame(data, LoadState.loading(data).data);
        assertSame(data, LoadState.error("Sem conexão", 0, data).data);
    }

    /** Prepara a página utilizada no carregamento ou na verificação do catálogo. */
    private CatalogPage<String> page(int number, String... values) {
        CatalogPage<String> page = new CatalogPage<>();
        page.number = number;
        page.content = Arrays.asList(values);
        page.totalElements = 4;
        page.totalPages = 2;
        return page;
    }
}
