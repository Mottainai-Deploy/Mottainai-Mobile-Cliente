package com.mottainai.cliente.design;

import androidx.lifecycle.ViewModel;
import com.mottainai.cliente.network.dto.CatalogStore;
import java.util.HashMap;
import java.util.Map;

public final class ClientFlowState extends ViewModel {
    public final Map<String, String> registration = new HashMap<>();
    public boolean openSearch;
    public String promotionId;
    public CatalogStore store;
    public boolean marketing;
    public void clearRegistration() { registration.clear(); marketing=false; }
    @Override protected void onCleared() { clearRegistration(); store=null; }
}
