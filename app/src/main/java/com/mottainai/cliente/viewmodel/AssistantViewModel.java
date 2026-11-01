package com.mottainai.cliente.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.mottainai.cliente.models.ChatMessage;
import com.mottainai.cliente.network.dto.ChatHistory;
import com.mottainai.cliente.network.dto.ChatRequest;
import com.mottainai.cliente.network.dto.ChatSessions;
import com.mottainai.cliente.repository.AssistantReplyEngine;
import com.mottainai.cliente.repository.AssistantRepository;
import com.mottainai.cliente.repository.LoadState;
import com.mottainai.cliente.repository.MockOfferRepository;
import com.mottainai.cliente.repository.MockUserRepository;
import com.mottainai.cliente.utils.SessionManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class AssistantViewModel extends AndroidViewModel {
    private final AssistantRepository repository;
    private final AssistantReplyEngine demoEngine;
    private final List<ChatMessage> messages = new ArrayList<>();
    private final MutableLiveData<LoadState<List<ChatMessage>>> state = new MutableLiveData<>();
    private final MutableLiveData<List<ChatSessions.Session>> sessions = new MutableLiveData<>();
    private String sessionId = UUID.randomUUID().toString();
    private boolean closed;

    public AssistantViewModel(@NonNull Application application) {
        super(application);
        repository = new AssistantRepository(application);
        demoEngine = new SessionManager(application).isDemoMode()
                ? new AssistantReplyEngine(new MockOfferRepository(), new MockUserRepository()) : null;
        resetConversation();
    }

    public LiveData<LoadState<List<ChatMessage>>> state() { return state; }
    public LiveData<List<ChatSessions.Session>> sessions() { return sessions; }
    public boolean isDemo() { return demoEngine != null; }
    public boolean isAvailable() { return isDemo() || repository.isConfigured(); }
    public boolean isClosed() { return closed; }
    public boolean isBusy() { return state.getValue() != null && state.getValue().status == LoadState.Status.LOADING; }
    public void consumeSessionChoices() { sessions.setValue(null); }

    private List<ChatMessage> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(messages));
    }

    public boolean send(String text) {
        if (isBusy() || !isAvailable() || closed) return false;
        final ChatRequest request;
        try {
            request = new ChatRequest(text, sessionId);
        } catch (IllegalArgumentException invalid) {
            state.setValue(LoadState.error(invalid.getMessage(), 0, snapshot()));
            return false;
        }
        messages.add(new ChatMessage(ChatMessage.Sender.USER, request.message));
        if (isDemo()) {
            messages.add(demoEngine.reply(request.message));
            state.setValue(LoadState.success(snapshot()));
            return true;
        }
        state.setValue(LoadState.loading(snapshot()));
        repository.send(request, result -> {
            if (result.status == LoadState.Status.SUCCESS && result.data.isValidFor(sessionId)) {
                messages.add(new ChatMessage(ChatMessage.Sender.BOT, result.data.response));
                state.setValue(LoadState.success(snapshot()));
            } else fail(result.status == LoadState.Status.ERROR ? result.message
                    : "O assistente retornou uma resposta incompleta ou de outra conversa.", result.httpStatus);
        });
        return true;
    }

    public void loadHistory() {
        if (isBusy() || !isAvailable() || isDemo()) return;
        state.setValue(LoadState.loading(snapshot()));
        repository.history(sessionId, result -> {
            if (result.status != LoadState.Status.SUCCESS) {
                fail(result.httpStatus == 404 ? "Conversa ainda não encontrada no serviço. Nenhuma mensagem foi reenviada."
                        : result.message, result.httpStatus);
                return;
            }
            ChatHistory history = result.data;
            if (!history.isValidFor(sessionId)) {
                fail("Histórico incompleto ou de outra conversa.", 0);
                return;
            }
            messages.clear();
            for (ChatHistory.Message message : history.messages) {
                if (message == null || message.content == null || message.content.trim().isEmpty()) continue;
                if ("user".equals(message.role)) messages.add(new ChatMessage(ChatMessage.Sender.USER, message.content));
                else if ("assistant".equals(message.role)) messages.add(new ChatMessage(ChatMessage.Sender.BOT, message.content));
                // Não exibe registros de sistema/ferramenta como conversa visível para o cliente.
            }
            closed = "closed".equalsIgnoreCase(history.status) || "expired".equalsIgnoreCase(history.status);
            state.setValue(LoadState.success(snapshot()));
        });
    }

    public void loadSessions() {
        if (isBusy() || !isAvailable() || isDemo()) return;
        state.setValue(LoadState.loading(snapshot()));
        repository.sessions(result -> {
            if (result.status != LoadState.Status.SUCCESS || result.data.sessions == null) {
                fail(result.status == LoadState.Status.ERROR ? result.message : "Lista de conversas incompleta.", result.httpStatus);
                return;
            }
            List<ChatSessions.Session> valid = new ArrayList<>();
            for (ChatSessions.Session session : result.data.sessions) {
                if (session != null && ChatRequest.isValidSessionId(session.sessionId)) valid.add(session);
            }
            state.setValue(LoadState.success(snapshot()));
            sessions.setValue(valid);
        });
    }

    public void openConversation(String id) {
        if (isBusy() || !ChatRequest.isValidSessionId(id)) return;
        sessionId = id;
        closed = false;
        messages.clear();
        loadHistory();
    }

    public void newConversation() {
        if (isBusy()) return;
        sessionId = UUID.randomUUID().toString();
        resetConversation();
    }

    private void resetConversation() {
        messages.clear();
        closed = false;
        if (isDemo()) messages.add(new ChatMessage(ChatMessage.Sender.BOT,
                "Demonstração: posso mostrar ofertas e pontos fictícios. Nenhuma resposta vem da IA real."));
        state.setValue(isAvailable() ? LoadState.success(snapshot())
                : LoadState.error("O endereço do assistente ainda não foi configurado para este aplicativo.", 0, snapshot()));
    }

    private void fail(String message, int httpStatus) {
        if (httpStatus == 401) messages.clear();
        state.setValue(LoadState.error(message, httpStatus, snapshot()));
    }

    @Override protected void onCleared() {
        repository.cancel();
        messages.clear();
        super.onCleared();
    }
}
