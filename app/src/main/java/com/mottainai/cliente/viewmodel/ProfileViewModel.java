package com.mottainai.cliente.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mottainai.cliente.network.dto.CustomerProfileResponse;
import com.mottainai.cliente.network.dto.UpdateFirebaseCustomerProfileRequest;
import com.mottainai.cliente.repository.AuthRepository;

/** Representa ProfileViewModel, responsável por o estado observado pela interface. */
public final class ProfileViewModel extends AndroidViewModel {
    /** Define o contrato de SaveResult, responsável por o estado observado pela interface. */
    public interface SaveResult {
        /** Recebe o resultado bem-sucedido e dá continuidade à operação solicitada. */
        void success(CustomerProfileResponse profile);
        /** Trata a falha recebida e informa o erro ao fluxo responsável. */
        void error(String message);
    }
    /** Representa State, responsável por o estado observado pela interface. */
    public static final class State {
        /** Agrupa as opções de Kind, responsável por o estado observado pela interface. */
        public enum Kind { IDLE, LOADING, SAVING, READY, ERROR }

        public final Kind kind;
        public final CustomerProfileResponse profile;
        public final String message;

        /** Inicializa State com os dados e as dependências recebidos. */
        private State(Kind kind, CustomerProfileResponse profile, String message) {
            this.kind = kind;
            this.profile = profile;
            this.message = message;
        }
    }

    private final AuthRepository repository;
    private final MutableLiveData<State> state = new MutableLiveData<>(
            new State(State.Kind.IDLE, null, null));

    /** Inicializa ProfileViewModel com os dados e as dependências recebidos. */
    public ProfileViewModel(@NonNull Application application) {
        super(application);
        repository = new AuthRepository(application);
    }

    /** Retorna o estado atual observado pelo fluxo. */
    public LiveData<State> state() {
        return state;
    }

    /** Inicia o carregamento dos dados ou da imagem para o componente solicitado. */
    public void load() {
        State current = state.getValue();
        if (current != null && (current.kind == State.Kind.LOADING
                || current.kind == State.Kind.SAVING)) return;
        CustomerProfileResponse previous = current == null ? null : current.profile;
        state.setValue(new State(State.Kind.LOADING, previous, null));
        repository.loadProfile(new AuthRepository.Result<CustomerProfileResponse>() {
            /** Recebe o resultado bem-sucedido e dá continuidade à operação solicitada. */
            @Override public void success(CustomerProfileResponse profile) {
                state.setValue(new State(State.Kind.READY, profile, null));
            }

            /** Trata a falha recebida e informa o erro ao fluxo responsável. */
            @Override public void error(String message) {
                state.setValue(new State(State.Kind.ERROR, previous, message));
            }
        });
    }

    /** Salva os dados informados e acompanha o resultado da operação. */
    public void save(UpdateFirebaseCustomerProfileRequest request) {
        State current = state.getValue();
        if (current == null || current.profile == null || current.kind == State.Kind.SAVING) return;
        CustomerProfileResponse previous = current.profile;
        state.setValue(new State(State.Kind.SAVING, previous, null));
        repository.updateProfile(request, new AuthRepository.Result<CustomerProfileResponse>() {
            /** Recebe o resultado bem-sucedido e dá continuidade à operação solicitada. */
            @Override public void success(CustomerProfileResponse profile) {
                state.setValue(new State(State.Kind.READY, profile, "Perfil atualizado."));
            }

            /** Trata a falha recebida e informa o erro ao fluxo responsável. */
            @Override public void error(String message) {
                state.setValue(new State(State.Kind.ERROR, previous, message));
            }
        });
    }

    /** Solicita a atualização da URL da foto no perfil autenticado. */
    public void saveAvatar(String avatarUrl, SaveResult result) {
        State current = state.getValue();
        CustomerProfileResponse profile = current == null ? null : current.profile;
        if (profile == null) {
            repository.loadProfile(new AuthRepository.Result<CustomerProfileResponse>() {
                /** Recebe o resultado bem-sucedido e dá continuidade à operação solicitada. */
                @Override public void success(CustomerProfileResponse loaded) {
                    saveAvatarFromProfile(loaded, avatarUrl, result);
                }
                /** Trata a falha recebida e informa o erro ao fluxo responsável. */
                @Override public void error(String message) { result.error(message); }
            });
            return;
        }
        saveAvatarFromProfile(profile, avatarUrl, result);
    }

    /** Preserva os dados atuais do perfil ao salvar a referência da foto. */
    private void saveAvatarFromProfile(CustomerProfileResponse profile, String avatarUrl,
                                       SaveResult result) {
        if (profile.fullName == null || profile.fullName.trim().isEmpty()) {
            result.error("Não foi possível identificar seu perfil.");
            return;
        }
        state.setValue(new State(State.Kind.SAVING, profile, null));
        UpdateFirebaseCustomerProfileRequest request = new UpdateFirebaseCustomerProfileRequest(
                profile.fullName, profile.phone, profile.marketingConsent, avatarUrl);
        repository.updateProfile(request, new AuthRepository.Result<CustomerProfileResponse>() {
            /** Recebe o resultado bem-sucedido e dá continuidade à operação solicitada. */
            @Override public void success(CustomerProfileResponse updated) {
                state.setValue(new State(State.Kind.READY, updated, "Foto atualizada."));
                result.success(updated);
            }
            /** Trata a falha recebida e informa o erro ao fluxo responsável. */
            @Override public void error(String message) {
                state.setValue(new State(State.Kind.ERROR, profile, message));
                result.error(message);
            }
        });
    }
}
