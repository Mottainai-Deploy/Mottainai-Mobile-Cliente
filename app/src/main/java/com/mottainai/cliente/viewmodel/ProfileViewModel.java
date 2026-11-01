package com.mottainai.cliente.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mottainai.cliente.network.dto.CustomerProfileResponse;
import com.mottainai.cliente.network.dto.UpdateFirebaseCustomerProfileRequest;
import com.mottainai.cliente.repository.AuthRepository;

public final class ProfileViewModel extends AndroidViewModel {
    public interface SaveResult {
        void success(CustomerProfileResponse profile);
        void error(String message);
    }
    public static final class State {
        public enum Kind { IDLE, LOADING, SAVING, READY, ERROR }

        public final Kind kind;
        public final CustomerProfileResponse profile;
        public final String message;

        private State(Kind kind, CustomerProfileResponse profile, String message) {
            this.kind = kind;
            this.profile = profile;
            this.message = message;
        }
    }

    private final AuthRepository repository;
    private final MutableLiveData<State> state = new MutableLiveData<>(
            new State(State.Kind.IDLE, null, null));

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        repository = new AuthRepository(application);
    }

    public LiveData<State> state() {
        return state;
    }

    public void load() {
        State current = state.getValue();
        if (current != null && (current.kind == State.Kind.LOADING
                || current.kind == State.Kind.SAVING)) return;
        CustomerProfileResponse previous = current == null ? null : current.profile;
        state.setValue(new State(State.Kind.LOADING, previous, null));
        repository.loadProfile(new AuthRepository.Result<CustomerProfileResponse>() {
            @Override public void success(CustomerProfileResponse profile) {
                state.setValue(new State(State.Kind.READY, profile, null));
            }

            @Override public void error(String message) {
                state.setValue(new State(State.Kind.ERROR, previous, message));
            }
        });
    }

    public void save(UpdateFirebaseCustomerProfileRequest request) {
        State current = state.getValue();
        if (current == null || current.profile == null || current.kind == State.Kind.SAVING) return;
        CustomerProfileResponse previous = current.profile;
        state.setValue(new State(State.Kind.SAVING, previous, null));
        repository.updateProfile(request, new AuthRepository.Result<CustomerProfileResponse>() {
            @Override public void success(CustomerProfileResponse profile) {
                state.setValue(new State(State.Kind.READY, profile, "Perfil atualizado."));
            }

            @Override public void error(String message) {
                state.setValue(new State(State.Kind.ERROR, previous, message));
            }
        });
    }

    public void saveAvatar(String avatarUrl, SaveResult result) {
        State current = state.getValue();
        CustomerProfileResponse profile = current == null ? null : current.profile;
        if (profile == null) {
            repository.loadProfile(new AuthRepository.Result<CustomerProfileResponse>() {
                @Override public void success(CustomerProfileResponse loaded) {
                    saveAvatarFromProfile(loaded, avatarUrl, result);
                }
                @Override public void error(String message) { result.error(message); }
            });
            return;
        }
        saveAvatarFromProfile(profile, avatarUrl, result);
    }

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
            @Override public void success(CustomerProfileResponse updated) {
                state.setValue(new State(State.Kind.READY, updated, "Foto atualizada."));
                result.success(updated);
            }
            @Override public void error(String message) {
                state.setValue(new State(State.Kind.ERROR, profile, message));
                result.error(message);
            }
        });
    }
}
