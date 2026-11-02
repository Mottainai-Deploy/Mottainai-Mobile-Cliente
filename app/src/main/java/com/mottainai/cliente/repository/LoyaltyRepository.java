package com.mottainai.cliente.repository;

import android.content.Context;

import com.mottainai.cliente.BuildConfig;
import com.mottainai.cliente.network.ApiClient;
import com.mottainai.cliente.network.LoyaltyService;
import com.mottainai.cliente.network.dto.LoyaltyAccount;
import com.mottainai.cliente.network.dto.LoyaltyTransaction;
import com.mottainai.cliente.utils.SessionManager;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Representa LoyaltyRepository, responsável por o acesso e o tratamento dos dados do aplicativo. */
public class LoyaltyRepository {
    private final LoyaltyService service;
    private final SessionManager session;

    /** Inicializa LoyaltyRepository com os dados e as dependências recebidos. */
    public LoyaltyRepository(Context context) {
        session = new SessionManager(context);
        service = BuildConfig.API_BASE_URL.trim().isEmpty() ? null
                : ApiClient.create(context.getApplicationContext(), BuildConfig.API_BASE_URL,
                LoyaltyService.class);
    }

    /** Obtém os dados da conta de fidelidade do cliente. */
    public void account(CatalogRepository.Result<LoyaltyAccount> result) {
        if (session.isDemoMode()) {
            result.accept(LoadState.success(DemoData.loyaltyAccount()));
            return;
        }
        if (unavailable(result)) return;
        enqueue(service.account(), result);
    }

    /** Obtém os lançamentos de pontos do período solicitado. */
    public void transactions(LocalDate month,
                             CatalogRepository.Result<List<LoyaltyTransaction>> result) {
        if (session.isDemoMode()) {
            result.accept(LoadState.success(DemoData.transactions(month)));
            return;
        }
        if (unavailable(result)) return;
        LoyaltyPeriod period = new LoyaltyPeriod(month);
        enqueue(service.transactions(period.from.toString(), period.to.toString()), result);
    }

    /** Informa que o serviço necessário à consulta não está disponível. */
    private <T> boolean unavailable(CatalogRepository.Result<T> result) {
        if (service != null) return false;
        result.accept(LoadState.error("Serviço indisponível. Tente mais tarde.", 0));
        return true;
    }

    /** Executa a requisição assíncrona e encaminha a resposta ou a falha. */
    private static <T> void enqueue(Call<T> call, CatalogRepository.Result<T> result) {
        call.enqueue(new Callback<T>() {
            /** Trata a resposta da requisição e encaminha o resultado ao fluxo que a solicitou. */
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.accept(LoadState.success(response.body()));
                } else {
                    result.accept(LoadState.error("Não foi possível carregar seus pontos.", response.code()));
                }
            }

            /** Trata a falha recebida e informa o erro ao fluxo responsável. */
            @Override
            public void onFailure(Call<T> call, Throwable throwable) {
                result.accept(LoadState.error(throwable instanceof IOException
                        ? "Sem conexão. Verifique sua internet."
                        : "Não foi possível carregar seus pontos.", 0));
            }
        });
    }
}
