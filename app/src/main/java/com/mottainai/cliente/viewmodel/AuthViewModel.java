package com.mottainai.cliente.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.mottainai.cliente.network.dto.CreateCustomerRequest;
import com.mottainai.cliente.network.dto.ViaCepAddress;
import com.mottainai.cliente.repository.AuthRepository;

/** Representa AuthViewModel, responsável por o estado observado pela interface. */
public final class AuthViewModel extends ViewModel {
    /** Representa State, responsável por o estado observado pela interface. */
    public static final class State {
        /** Agrupa as opções de Kind, responsável por o estado observado pela interface. */
        public enum Kind { IDLE, SELECTING_GOOGLE, LOADING, AUTHENTICATED, PROFILE_REQUIRED,
            PROFILE_SYNC_ERROR, REGISTERED, RESET_SENT, ERROR }
        public final Kind kind;
        public final String message;
        public final String email;

        /** Inicializa State com os dados e as dependências recebidos. */
        private State(Kind kind, String message) {
            this(kind, message, null);
        }

        /** Inicializa State com os dados e as dependências recebidos. */
        private State(Kind kind, String message, String email) {
            this.kind = kind;
            this.message = message;
            this.email = email;
        }
    }

    private final MutableLiveData<State> state = new MutableLiveData<>(new State(State.Kind.IDLE, null));
    private final MutableLiveData<ViaCepAddress> address = new MutableLiveData<>();
    private final MutableLiveData<String> cepError = new MutableLiveData<>();
    private int cepRequest;

    /** Retorna o estado atual observado pelo fluxo. */
    public LiveData<State> state() { return state; }
    /** Retorna o estado observado do endereço consultado pelo CEP. */
    public LiveData<ViaCepAddress> address() { return address; }
    /** Retorna o erro observado da consulta de CEP. */
    public LiveData<String> cepError() { return cepError; }

    /** Inicia a autenticação com e-mail e senha e o fluxo de consulta do perfil. */
    public void login(AuthRepository repository, String email, String password) {
        if (isBusy()) return;
        state.setValue(new State(State.Kind.LOADING, null));
        repository.login(email, password, loginResult(repository));
    }

    /** Inicia a autenticação usando a credencial recebida do Google. */
    public void loginWithGoogle(AuthRepository repository, String idToken) {
        if (state.getValue().kind == State.Kind.LOADING) return;
        state.setValue(new State(State.Kind.LOADING, null));
        repository.loginWithGoogle(idToken, loginResult(repository));
    }

    /** Indica se existe uma operação em andamento que bloqueia novo envio. */
    public boolean isBusy() {
        return state.getValue().kind == State.Kind.LOADING
                || state.getValue().kind == State.Kind.SELECTING_GOOGLE;
    }

    /** Marca o início da seleção da conta e ativa o carregamento do login. */
    public boolean beginGoogleSignIn() {
        if (isBusy()) return false;
        state.setValue(new State(State.Kind.SELECTING_GOOGLE, null));
        return true;
    }

    /** Encerra o carregamento e informa a falha da entrada com Google. */
    public void googleSignInFailed(String message) {
        if (state.getValue().kind == State.Kind.SELECTING_GOOGLE) {
            state.setValue(new State(State.Kind.ERROR, message));
        }
    }

    /** Solicita novamente a sincronização do perfil da conta já autenticada. */
    public void retryProfileSync(AuthRepository repository) {
        if (isBusy()) return;
        state.setValue(new State(State.Kind.LOADING, null));
        repository.retryProfileSync(loginResult(repository));
    }

    /** Encaminha o resultado do login para o estado observado pela interface. */
    private AuthRepository.Result<AuthRepository.LoginResult> loginResult(AuthRepository repository) {
        return new AuthRepository.Result<AuthRepository.LoginResult>() {
            /** Informa o andamento da operação para atualizar a indicação de carregamento. */
            @Override public void progress(String message) {
                state.setValue(new State(State.Kind.LOADING, message));
            }
            /** Recebe o resultado bem-sucedido e dá continuidade à operação solicitada. */
            @Override public void success(AuthRepository.LoginResult result) {
                state.setValue(result.profileRequired
                        ? new State(State.Kind.PROFILE_REQUIRED, null, result.email)
                        : new State(State.Kind.AUTHENTICATED, null));
            }
            /** Trata a falha recebida e informa o erro ao fluxo responsável. */
            @Override public void error(String message) {
                state.setValue(new State(repository.pendingProfile() != null
                        ? State.Kind.PROFILE_SYNC_ERROR : State.Kind.ERROR, message));
            }
        };
    }

    /** Solicita o envio das instruções para recuperação da senha. */
    public void resetPassword(AuthRepository repository, String email) {
        if (isBusy()) return;
        state.setValue(new State(State.Kind.LOADING, null));
        repository.resetPassword(email, new AuthRepository.Result<Void>() {
            /** Recebe o resultado bem-sucedido e dá continuidade à operação solicitada. */
            @Override public void success(Void value) {
                state.setValue(new State(State.Kind.RESET_SENT,
                        "Se houver uma conta para esse e-mail, você receberá as instruções para redefinir a senha."));
            }
            /** Trata a falha recebida e informa o erro ao fluxo responsável. */
            @Override public void error(String message) {
                state.setValue(new State(State.Kind.ERROR, message));
            }
        });
    }

    /** Restaura o estado inicial do formulário de autenticação. */
    public void reset() {
        cepRequest++;
        address.setValue(null);
        cepError.setValue(null);
        state.setValue(new State(State.Kind.IDLE, null));
    }

    /** Executa o cadastro e acompanha o resultado da criação do perfil. */
    public void register(AuthRepository repository, CreateCustomerRequest request) {
        if (isBusy()) return;
        state.setValue(new State(State.Kind.LOADING, null));
        repository.register(request, new AuthRepository.Result<Void>() {
            /** Informa o andamento da operação para atualizar a indicação de carregamento. */
            @Override public void progress(String message) {
                state.setValue(new State(State.Kind.LOADING, message));
            }
            /** Recebe o resultado bem-sucedido e dá continuidade à operação solicitada. */
            @Override public void success(Void value) {
                state.setValue(new State(State.Kind.REGISTERED, null));
            }
            /** Trata a falha recebida e informa o erro ao fluxo responsável. */
            @Override public void error(String message) {
                state.setValue(new State(repository.pendingProfile() != null
                        ? State.Kind.PROFILE_SYNC_ERROR : State.Kind.ERROR, message));
            }
        });
    }

    /** Consulta o CEP e encaminha os dados do endereço ao formulário. */
    public void lookupCep(AuthRepository repository, String cep) {
        int request = ++cepRequest;
        cepError.setValue(null);
        repository.lookupCep(cep, new AuthRepository.Result<ViaCepAddress>() {
            /** Recebe o resultado bem-sucedido e dá continuidade à operação solicitada. */
            @Override public void success(ViaCepAddress value) {
                if (request == cepRequest) address.setValue(value);
            }
            /** Trata a falha recebida e informa o erro ao fluxo responsável. */
            @Override public void error(String message) {
                if (request == cepRequest) cepError.setValue(message);
            }
        });
    }
}
