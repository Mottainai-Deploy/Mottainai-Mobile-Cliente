package com.mottainai.cliente.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mottainai.cliente.network.dto.LoyaltyAccount;
import com.mottainai.cliente.network.dto.LoyaltyTransaction;
import com.mottainai.cliente.repository.LoadState;
import com.mottainai.cliente.repository.LoyaltyRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Representa LoyaltyViewModel, responsável por o estado observado pela interface. */
public class LoyaltyViewModel extends AndroidViewModel {
    private final LoyaltyRepository repository;
    private final MutableLiveData<LoadState<LoyaltyAccount>> account = new MutableLiveData<>();
    private final MutableLiveData<LoadState<List<LoyaltyTransaction>>> transactions = new MutableLiveData<>();
    private int historyRequest;
    private LoyaltyAccount cachedAccount;
    private List<LoyaltyTransaction> cachedTransactions;

    /** Inicializa LoyaltyViewModel com os dados e as dependências recebidos. */
    public LoyaltyViewModel(@NonNull Application application) {
        super(application);
        repository = new LoyaltyRepository(application);
    }

    /** Obtém os dados da conta de fidelidade do cliente. */
    public LiveData<LoadState<LoyaltyAccount>> account() { return account; }
    /** Obtém os lançamentos de pontos do período solicitado. */
    public LiveData<LoadState<List<LoyaltyTransaction>>> transactions() { return transactions; }

    /** Carrega o saldo e os dados da conta de fidelidade. */
    public void loadAccount() {
        if (account.getValue() != null && account.getValue().status == LoadState.Status.LOADING) return;
        account.setValue(LoadState.loading(cachedAccount));
        repository.account(state -> {
            if (state.status == LoadState.Status.SUCCESS) {
                cachedAccount = state.data;
                account.setValue(state);
            } else {
                account.setValue(LoadState.error(state.message, state.httpStatus, cachedAccount));
            }
        });
    }

    /** Carrega as janelas recentes do histórico de pontos. */
    public void loadRecentTransactions() {
        if (transactions.getValue() != null && transactions.getValue().status == LoadState.Status.LOADING) return;
        int request = ++historyRequest;
        transactions.setValue(LoadState.loading(cachedTransactions));
        List<LoyaltyTransaction> collected = new ArrayList<>();
        int[] remaining = {6};
        LoadState<?>[] firstError = {null};
        LocalDate month = LocalDate.now().withDayOfMonth(1).minusMonths(5);
        for (int i = 0; i < 6; i++) {
            repository.transactions(month.plusMonths(i), state -> {
                if (request != historyRequest) return;
                if (state.status == LoadState.Status.SUCCESS) {
                    collected.addAll(state.data);
                } else if (firstError[0] == null) {
                    firstError[0] = state;
                }
                if (--remaining[0] == 0) {
                    collected.sort(Comparator.comparing(
                            (LoyaltyTransaction item) -> item.createdAt == null ? "" : item.createdAt)
                            .reversed());
                    if (firstError[0] != null) {
                        transactions.setValue(LoadState.error(
                                "Não foi possível carregar todo o histórico. " + firstError[0].message,
                                firstError[0].httpStatus, cachedTransactions != null ? cachedTransactions : collected));
                    } else {
                        cachedTransactions = collected;
                        transactions.setValue(LoadState.success(collected));
                    }
                }
            });
        }
    }
}
