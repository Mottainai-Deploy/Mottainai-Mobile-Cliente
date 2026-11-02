package com.mottainai.cliente.design;

import androidx.lifecycle.ViewModel;
import com.mottainai.cliente.network.dto.CatalogStore;
import java.util.HashMap;
import java.util.Map;

/** Rascunhos de cadastro ficam apenas em memória, inclusive entre mudanças de configuração. */
public final class ClientFlowState extends ViewModel {
    public final Map<String, String> registration = new HashMap<>();
    public boolean openSearch;
    public String promotionId;
    public CatalogStore store;
    public boolean marketing;
    /** Remove os dados temporários acumulados durante o cadastro. */
    public void clearRegistration() { registration.clear(); marketing=false; }
    /** Libera os recursos mantidos pelo estado quando ele deixa de ser utilizado. */
    @Override protected void onCleared() { clearRegistration(); store=null; }
}
