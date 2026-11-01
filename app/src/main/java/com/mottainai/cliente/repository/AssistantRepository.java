package com.mottainai.cliente.repository;

import android.content.Context;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mottainai.cliente.BuildConfig;
import com.mottainai.cliente.network.ApiClient;
import com.mottainai.cliente.network.AssistantService;
import com.mottainai.cliente.network.dto.ChatHistory;
import com.mottainai.cliente.network.dto.ChatRequest;
import com.mottainai.cliente.network.dto.ChatResponse;
import com.mottainai.cliente.network.dto.ChatSessions;
import com.mottainai.cliente.utils.FirebaseSupport;
import com.mottainai.cliente.utils.SessionManager;
import java.io.IOException;
import java.util.function.Consumer;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class AssistantRepository {
    private final Context context;
    private final AssistantService service;
    private final String ownerUid;
    private Call<?> pending;

    public AssistantRepository(Context context) {
        this.context = context.getApplicationContext();
        FirebaseAuth auth = FirebaseSupport.authIfConfigured(this.context);
        FirebaseUser user = auth == null ? null : auth.getCurrentUser();
        ownerUid = user == null ? null : user.getUid();
        AssistantService configured = null;
        String url = BuildConfig.ASSISTANT_BASE_URL.trim();
        if (!url.isEmpty() && (url.startsWith("https://") || BuildConfig.DEBUG && url.startsWith("http://"))) {
            try {
                configured = ApiClient.create(this.context, url, AssistantService.class, false);
            } catch (IllegalArgumentException invalidConfiguration) {
                // Não expõe URL configurada nem credenciais na interface ou nos logs.
            }
        }
        service = configured;
    }

    public boolean isConfigured() { return service != null; }

    public void send(ChatRequest request, Consumer<LoadState<ChatResponse>> result) {
        if (!ready(result)) return;
        enqueue(service.send(request, ownerUid), result);
    }

    public void history(String sessionId, Consumer<LoadState<ChatHistory>> result) {
        if (!ready(result)) return;
        enqueue(service.history(sessionId, ownerUid), result);
    }

    public void sessions(Consumer<LoadState<ChatSessions>> result) {
        if (!ready(result)) return;
        enqueue(service.sessions(ownerUid), result);
    }

    private boolean isOwnerActive() {
        FirebaseAuth auth = FirebaseSupport.authIfConfigured(context);
        FirebaseUser user = auth == null ? null : auth.getCurrentUser();
        return ownerUid != null && user != null && ownerUid.equals(user.getUid())
                && new SessionManager(context).isLoggedIn();
    }

    private <T> boolean ready(Consumer<LoadState<T>> result) {
        if (service == null) {
            result.accept(LoadState.error("O assistente não está disponível no momento.", 0));
            return false;
        }
        if (!isOwnerActive()) {
            result.accept(LoadState.error("Sua sessão mudou. Entre novamente para abrir a conversa.", 401));
            return false;
        }
        return true;
    }

    private <T> void enqueue(Call<T> call, Consumer<LoadState<T>> result) {
        cancel();
        pending = call;
        call.enqueue(new Callback<T>() {
            @Override public void onResponse(Call<T> completed, Response<T> response) {
                if (pending != completed) return;
                pending = null;
                if (!isOwnerActive()) {
                    result.accept(LoadState.error("Sua sessão mudou. Abra novamente a conversa.", 401));
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    result.accept(LoadState.success(response.body()));
                } else {
                    String message = response.code() == 401 || response.code() == 403
                            ? "O assistente não está disponível para esta conta no momento."
                            : response.code() == 429 ? "Muitas mensagens. Aguarde antes de tentar novamente."
                            : "Não foi possível concluir a consulta ao assistente.";
                    result.accept(LoadState.error(message, response.code()));
                }
            }

            @Override public void onFailure(Call<T> completed, Throwable failure) {
                if (pending != completed || completed.isCanceled()) return;
                pending = null;
                if (!isOwnerActive()) {
                    result.accept(LoadState.error("Sua sessão mudou. Abra novamente a conversa.", 401));
                    return;
                }
                result.accept(LoadState.error(failure instanceof IOException
                        ? "Falha de conexão. O envio pode ter sido recebido; atualize a conversa antes de reenviar."
                        : "Resposta inválida do assistente. Tente atualizar a conversa.", 0));
            }
        });
    }

    public void cancel() {
        Call<?> call = pending;
        pending = null;
        if (call != null) call.cancel();
    }
}
