package com.mottainai.cliente.network;

import com.mottainai.cliente.network.dto.LoyaltyAccount;
import com.mottainai.cliente.network.dto.LoyaltyTransaction;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/** Define o contrato de LoyaltyService, responsável por a comunicação com os serviços externos. */
public interface LoyaltyService {
    /** Obtém os dados da conta de fidelidade do cliente. */
    @GET("api/v1/client/loyalty/balance")
    Call<LoyaltyAccount> account();

    /** Obtém os lançamentos de pontos do período solicitado. */
    @GET("api/v1/client/loyalty/transactions")
    Call<List<LoyaltyTransaction>> transactions(@Query("from") String from,
                                                  @Query("to") String to);
}
