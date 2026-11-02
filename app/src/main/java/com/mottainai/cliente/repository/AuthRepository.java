package com.mottainai.cliente.repository;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.mottainai.cliente.BuildConfig;
import com.mottainai.cliente.network.ApiClient;
import com.mottainai.cliente.network.ApiService;
import com.mottainai.cliente.network.dto.CreateCustomerRequest;
import com.mottainai.cliente.network.dto.CreateFirebaseCustomerProfileRequest;
import com.mottainai.cliente.network.dto.CustomerProfileResponse;
import com.mottainai.cliente.network.dto.UpdateFirebaseCustomerProfileRequest;
import com.mottainai.cliente.network.dto.ViaCepAddress;
import com.mottainai.cliente.utils.FirebaseSupport;
import com.mottainai.cliente.utils.CloudinaryProfilePhoto;
import com.mottainai.cliente.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;

/** Representa AuthRepository, responsável por o acesso e o tratamento dos dados do aplicativo. */
public final class AuthRepository {
    private static final String PROFILE_CACHE = "customer_profile_cache";
    /** Define o contrato de Result, responsável por o acesso e o tratamento dos dados do aplicativo. */
    public interface Result<T> {
        /** Recebe o resultado bem-sucedido e dá continuidade à operação solicitada. */
        void success(T value);
        /** Trata a falha recebida e informa o erro ao fluxo responsável. */
        void error(String message);
        /** Informa o andamento da operação para atualizar a indicação de carregamento. */
        default void progress(String message) {}
    }

    /** Representa LoginResult, responsável por o acesso e o tratamento dos dados do aplicativo. */
    public static final class LoginResult {
        public final boolean profileRequired;
        public final String email;

        /** Inicializa LoginResult com os dados e as dependências recebidos. */
        private LoginResult(boolean profileRequired, String email) {
            this.profileRequired = profileRequired;
            this.email = email;
        }
    }

    /** Define o contrato de ViaCepService, responsável por o acesso e o tratamento dos dados do aplicativo. */
    private interface ViaCepService {
        /** Localiza o elemento ou resultado correspondente aos critérios informados. */
        @GET("ws/{cep}/json/")
        Call<ViaCepAddress> find(@Path("cep") String cep);
    }

    private final ApiService api;
    private final ViaCepService viaCep;
    private final SessionManager session;
    private final FirebaseAuth firebaseAuth;
    private final SharedPreferences profileCache;
    private final Context appContext;

    /** Inicializa AuthRepository com os dados e as dependências recebidos. */
    public AuthRepository(Context context) {
        appContext = context.getApplicationContext();
        session = new SessionManager(context);
        api = BuildConfig.API_BASE_URL.trim().isEmpty()
                ? null : ApiClient.create(context, BuildConfig.API_BASE_URL);
        firebaseAuth = FirebaseSupport.authIfConfigured(context);
        profileCache = context.getApplicationContext().getSharedPreferences(PROFILE_CACHE, Context.MODE_PRIVATE);
        viaCep = new Retrofit.Builder()
                .baseUrl("https://viacep.com.br/")
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(ViaCepService.class);
    }

    /** Inicia a autenticação com e-mail e senha e o fluxo de consulta do perfil. */
    public void login(String email, String password, Result<LoginResult> result) {
        if (!ready(result)) return;
        result.progress("Entrando…");
        session.clearSession();
        firebaseAuth.signInWithEmailAndPassword(email.trim(), password).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                result.error(firebaseError(task.getException(), "Não foi possível entrar."));
                return;
            }
            loadCurrentProfile(result);
        });
    }

    /** Inicia a autenticação usando a credencial recebida do Google. */
    public void loginWithGoogle(String idToken, Result<LoginResult> result) {
        if (!ready(result)) return;
        result.progress("Entrando com Google…");
        session.clearSession();
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                result.error(firebaseError(task.getException(), "Não foi possível entrar com Google."));
                return;
            }

            loadCurrentProfile(result);
        });
    }

    /** Recupera os dados disponíveis para concluir o perfil da conta autenticada. */
    @Nullable
    public LoginResult pendingProfile() {
        FirebaseUser user = firebaseAuth == null ? null : firebaseAuth.getCurrentUser();
        return user != null && !session.isLoggedIn()
                // Uma identidade Firebase sem sessão local não comprova que o perfil SQL esteja ausente.
                ? new LoginResult(false, user.getEmail()) : null;
    }

    /** Encerra a autenticação e limpa os dados de sessão associados à conta. */
    public void signOut() {
        FirebaseUser user = firebaseAuth == null ? null : firebaseAuth.getCurrentUser();
        if (user != null) clearCachedProfile(user.getUid());
        session.clearSession();
        if (firebaseAuth != null) firebaseAuth.signOut();
    }

    /** Solicita novamente a sincronização do perfil da conta já autenticada. */
    public void retryProfileSync(Result<LoginResult> result) {
        if (ready(result)) loadCurrentProfile(result);
    }

    /** Solicita o envio das instruções para recuperação da senha. */
    public void resetPassword(String email, Result<Void> result) {
        if (firebaseAuth == null) {
            result.error("O acesso à sua conta está indisponível no momento.");
            return;
        }
        firebaseAuth.sendPasswordResetEmail(email.trim()).addOnCompleteListener(task -> {
            if (task.isSuccessful() || (task.getException() instanceof FirebaseAuthException
                    && "ERROR_USER_NOT_FOUND".equals(
                    ((FirebaseAuthException) task.getException()).getErrorCode()))) {
                result.success(null);
            } else {
                result.error(firebaseError(task.getException(), "Não foi possível enviar as instruções. Tente novamente."));
            }
        });
    }

    /** Executa o cadastro e acompanha o resultado da criação do perfil. */
    public void register(CreateCustomerRequest request, Result<Void> result) {
        if (!ready(result)) return;
        FirebaseUser current = firebaseAuth.getCurrentUser();
        if (current != null && request.email.equalsIgnoreCase(current.getEmail())) {
            ensureSqlProfile(request, result, true);
            return;
        }
        session.clearClientSession();
        if (current != null) firebaseAuth.signOut();
        result.progress("Criando sua conta…");
        firebaseAuth.createUserWithEmailAndPassword(request.email.trim(), request.password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        result.error(firebaseError(task.getException(), "Não foi possível criar sua conta."));
                        return;
                    }
                    ensureSqlProfile(request, result, true);
                });
    }

    /** Consulta o perfil vinculado ao usuário autenticado. */
    public void loadProfile(Result<CustomerProfileResponse> result) {
        if (session.isDemoMode()) {
            result.success(DemoData.customerProfile());
            return;
        }
        if (api == null) {
            result.error("Não foi possível carregar seu perfil. Tente novamente.");
            return;
        }
        if (firebaseAuth == null || firebaseAuth.getCurrentUser() == null || !session.isLoggedIn()) {
            result.error("Sua sessão terminou. Entre novamente.");
            return;
        }
        String uid = firebaseAuth.getCurrentUser().getUid();
        api.profile(uid).enqueue(new Callback<CustomerProfileResponse>() {
            /** Trata a resposta da requisição e encaminha o resultado ao fluxo que a solicitou. */
            @Override public void onResponse(@NonNull Call<CustomerProfileResponse> call,
                                             @NonNull Response<CustomerProfileResponse> response) {
                CustomerProfileResponse body = response.body();
                if (!isCurrentUser(uid)) {
                    result.error("Sua conta mudou. Entre novamente.");
                    return;
                }
                ProfileSyncStatus status = ProfileSyncStatus.from(response.code(), body);
                if (status == ProfileSyncStatus.READY) {
                    populateLoginEmail(body);
                    hydrateCachedProfile(uid, body);
                    result.success(body);
                } else {
                    result.error(response.code() == 404
                            ? "Seu perfil ainda não foi sincronizado. Conclua o cadastro novamente."
                            : profileFailure(status, response.code()));
                }
            }
            /** Trata a falha recebida e informa o erro ao fluxo responsável. */
            @Override public void onFailure(@NonNull Call<CustomerProfileResponse> call,
                                            @NonNull Throwable throwable) {
                result.error(syncError(throwable));
            }
        });
    }

    /** Envia as alterações dos dados do perfil para persistência. */
    public void updateProfile(UpdateFirebaseCustomerProfileRequest request,
                              Result<CustomerProfileResponse> result) {
        if (session.isDemoMode()) {
            result.error("A edição de perfil fica indisponível na demonstração.");
            return;
        }
        if (api == null) {
            result.error("Não foi possível salvar seus dados. Tente novamente.");
            return;
        }
        if (firebaseAuth == null || firebaseAuth.getCurrentUser() == null || !session.isLoggedIn()) {
            result.error("Sua sessão terminou. Entre novamente.");
            return;
        }
        String uid = firebaseAuth.getCurrentUser().getUid();
        api.updateProfile(request, uid).enqueue(new Callback<CustomerProfileResponse>() {
            /** Trata a resposta da requisição e encaminha o resultado ao fluxo que a solicitou. */
            @Override public void onResponse(@NonNull Call<CustomerProfileResponse> call,
                                             @NonNull Response<CustomerProfileResponse> response) {
                if (!isCurrentUser(uid)) {
                    result.error("Sua conta mudou. Entre novamente.");
                } else {
                    CustomerProfileResponse profile = response.body();
                    ProfileSyncStatus status = ProfileSyncStatus.from(response.code(), profile);
                    if (status == ProfileSyncStatus.READY) {
                        // A resposta atual da API omite estes campos protegidos; mantém os valores confirmados.
                        profile.fullName = request.fullName;
                        profile.phone = request.phone;
                        profile.marketingConsent = request.marketingConsent;
                        if (request.avatarUrl != null) profile.avatarUrl = request.avatarUrl;
                        populateLoginEmail(profile);
                        cacheProfile(uid, profile);
                        result.success(profile);
                    } else {
                        result.error(response.code() == 404
                                ? "Perfil não encontrado. Entre novamente para sincronizar sua conta."
                                : profileFailure(status, response.code()));
                    }
                }
            }
            /** Trata a falha recebida e informa o erro ao fluxo responsável. */
            @Override public void onFailure(@NonNull Call<CustomerProfileResponse> call,
                                            @NonNull Throwable throwable) {
                result.error(syncError(throwable));
            }
        });
    }

    /** Consulta o CEP e encaminha os dados do endereço ao formulário. */
    public void lookupCep(String cep, Result<ViaCepAddress> result) {
        viaCep.find(cep).enqueue(new Callback<ViaCepAddress>() {
            /** Trata a resposta da requisição e encaminha o resultado ao fluxo que a solicitou. */
            @Override public void onResponse(@NonNull Call<ViaCepAddress> call,
                                             @NonNull Response<ViaCepAddress> response) {
                ViaCepAddress body = response.body();
                if (!response.isSuccessful() || body == null || Boolean.TRUE.equals(body.error)
                        || body.logradouro == null || body.logradouro.trim().isEmpty()) {
                    result.error("CEP não encontrado. Confira o número ou preencha o endereço.");
                } else {
                    result.success(body);
                }
            }
            /** Trata a falha recebida e informa o erro ao fluxo responsável. */
            @Override public void onFailure(@NonNull Call<ViaCepAddress> call,
                                            @NonNull Throwable throwable) {
                result.error("Não foi possível consultar o CEP. Preencha o endereço manualmente.");
            }
        });
    }

    /** Confere os requisitos necessários antes de iniciar a operação. */
    private boolean ready(Result<?> result) {
        if (firebaseAuth == null) {
            result.error("O acesso à sua conta está indisponível no momento.");
            return false;
        }
        if (api == null) {
            result.error("Não foi possível conectar agora. Tente novamente.");
            return false;
        }
        return true;
    }

    /** Consulta o perfil vinculado ao usuário autenticado. */
    private void loadCurrentProfile(Result<LoginResult> result) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            result.error("Sua sessão terminou. Entre novamente.");
            return;
        }
        String uid = user.getUid();
        result.progress("Carregando seu perfil…");
        api.profile(uid).enqueue(new Callback<CustomerProfileResponse>() {
            /** Trata a resposta da requisição e encaminha o resultado ao fluxo que a solicitou. */
            @Override public void onResponse(@NonNull Call<CustomerProfileResponse> call,
                                             @NonNull Response<CustomerProfileResponse> response) {
                CustomerProfileResponse profile = response.body();
                if (!isCurrentUser(uid)) {
                    result.error("A conta mudou durante o acesso. Entre novamente.");
                    return;
                }
                ProfileSyncStatus status = ProfileSyncStatus.from(response.code(), profile);
                if (status == ProfileSyncStatus.READY) {
                    session.saveClientSession(String.valueOf(profile.id), uid);
                    result.success(new LoginResult(false, null));
                } else if (status == ProfileSyncStatus.PROFILE_REQUIRED) {
                    result.success(new LoginResult(true, user.getEmail()));
                } else {
                    result.error(profileFailure(status, response.code()));
                }
            }
            /** Trata a falha recebida e informa o erro ao fluxo responsável. */
            @Override public void onFailure(@NonNull Call<CustomerProfileResponse> call,
                                            @NonNull Throwable throwable) {
                result.error(syncError(throwable));
            }
        });
    }

    /** Confere o perfil SQL após a autenticação e decide como continuar o cadastro. */
    private void ensureSqlProfile(CreateCustomerRequest request, Result<Void> result, boolean createIfMissing) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null || !request.email.trim().equalsIgnoreCase(user.getEmail())) {
            result.error("Sua conta mudou. Entre novamente para concluir o cadastro.");
            return;
        }
        String uid = user.getUid();
        result.progress("Verificando seu perfil…");
        api.profile(uid).enqueue(new Callback<CustomerProfileResponse>() {
            /** Trata a resposta da requisição e encaminha o resultado ao fluxo que a solicitou. */
            @Override public void onResponse(@NonNull Call<CustomerProfileResponse> call,
                                             @NonNull Response<CustomerProfileResponse> response) {
                if (!isCurrentUser(uid)) {
                    result.error("Sua conta mudou. Entre novamente.");
                    return;
                }
                ProfileSyncStatus status = ProfileSyncStatus.from(response.code(), response.body());
                if (status == ProfileSyncStatus.READY) {
                    session.saveClientSession(String.valueOf(response.body().id), uid);
                    result.success(null);
                } else if (status.allowsProfileCreation(createIfMissing)) {
                    createSqlProfile(request, result);
                } else if (status == ProfileSyncStatus.PROFILE_REQUIRED) {
                    result.error("Não foi possível concluir seu cadastro. Confira os dados e tente novamente.");
                } else {
                    result.error(profileFailure(status, response.code()));
                }
            }
            /** Trata a falha recebida e informa o erro ao fluxo responsável. */
            @Override public void onFailure(@NonNull Call<CustomerProfileResponse> call,
                                            @NonNull Throwable throwable) {
                result.error(syncError(throwable));
            }
        });
    }

    /** Envia os dados para criar o perfil SQL vinculado à conta Firebase. */
    private void createSqlProfile(CreateCustomerRequest request, Result<Void> result) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null || !request.email.trim().equalsIgnoreCase(user.getEmail())) {
            result.error("Sua conta mudou. Entre novamente para concluir o cadastro.");
            return;
        }
        String uid = user.getUid();
        result.progress("Salvando seu cadastro…");
        CreateFirebaseCustomerProfileRequest payload = new CreateFirebaseCustomerProfileRequest(
                request.fullName, request.cpf, request.phone, request.birthDate,
                request.marketingConsent, request.address);
        api.createProfile(payload, uid).enqueue(new Callback<CustomerProfileResponse>() {
            /** Trata a resposta da requisição e encaminha o resultado ao fluxo que a solicitou. */
            @Override public void onResponse(@NonNull Call<CustomerProfileResponse> call,
                                             @NonNull Response<CustomerProfileResponse> response) {
                CustomerProfileResponse profile = response.body();
                if (!isCurrentUser(uid)) {
                    result.error("Sua conta mudou. Entre novamente para concluir o cadastro.");
                    return;
                }
                ProfileSyncStatus status = ProfileSyncStatus.from(response.code(), profile);
                if (status == ProfileSyncStatus.READY) {
                    session.saveClientSession(String.valueOf(profile.id), uid);
                    profile.fullName = request.fullName;
                    profile.phone = request.phone;
                    profile.marketingConsent = request.marketingConsent;
                    cacheProfile(uid, profile);
                    result.success(null);
                } else if (status == ProfileSyncStatus.INACTIVE
                        || status == ProfileSyncStatus.INVALID_RESPONSE) {
                    result.error(profileFailure(status, response.code()));
                } else if (response.code() == 409) {
                    // Um POST anterior que expirou pode já ter sido concluído. Lê uma vez e nunca repete o POST em loop.
                    ensureSqlProfile(request, result, false);
                } else {
                    result.error("Não foi possível concluir seu cadastro. Confira os dados e tente novamente.");
                }
            }
            /** Trata a falha recebida e informa o erro ao fluxo responsável. */
            @Override public void onFailure(@NonNull Call<CustomerProfileResponse> call,
                                            @NonNull Throwable throwable) {
                    result.error("Não foi possível confirmar seu cadastro. Tente novamente.");
            }
        });
    }

    /** Confere se a resposta ainda pertence ao usuário autenticado. */
    private boolean isCurrentUser(String uid) {
        FirebaseUser current = firebaseAuth.getCurrentUser();
        return current != null && current.getUid().equals(uid);
    }

    /** Complementa os dados do perfil com o e-mail disponível na autenticação. */
    private void populateLoginEmail(CustomerProfileResponse profile) {
        FirebaseUser current = firebaseAuth.getCurrentUser();
        if (current == null) return;
        profile.email = current.getEmail();
    }

    /** Completa a apresentação do perfil com os dados preservados para a conta. */
    private void hydrateCachedProfile(String uid, CustomerProfileResponse profile) {
        if (profile.fullName == null || profile.fullName.trim().isEmpty()) {
            profile.fullName = profileCache.getString(uid + ".fullName", null);
        }
        if (profile.phone == null || profile.phone.trim().isEmpty()) {
            profile.phone = profileCache.getString(uid + ".phone", null);
        }
        if (profile.avatarUrl != null && profile.avatarUrl.startsWith("https://")) {
            CloudinaryProfilePhoto.saveUrl(appContext, profile.avatarUrl);
        }
    }

    /** Atualiza o cache dos dados do perfil da conta atual. */
    private void cacheProfile(String uid, CustomerProfileResponse profile) {
        profileCache.edit().putString(uid + ".fullName", profile.fullName)
                .putString(uid + ".phone", profile.phone).apply();
        if (profile.avatarUrl != null && profile.avatarUrl.startsWith("https://")) {
            CloudinaryProfilePhoto.saveUrl(appContext, profile.avatarUrl);
        }
    }

    /** Remove os dados de perfil preservados para a conta. */
    private void clearCachedProfile(String uid) {
        profileCache.edit().remove(uid + ".fullName").remove(uid + ".phone").apply();
    }

    /** Encaminha a falha da consulta de perfil ao tratamento de sincronização. */
    private String profileFailure(ProfileSyncStatus status, int httpStatus) {
        if (status == ProfileSyncStatus.INACTIVE) {
            session.clearClientSession();
            return "Seu cadastro está inativo. Entre em contato com o suporte.";
        }
        if (status == ProfileSyncStatus.INVALID_RESPONSE) {
            return "Não foi possível carregar seu perfil. Tente novamente.";
        }
        if (httpStatus == 401) return "Sua sessão expirou. Entre novamente.";
        if (httpStatus == 403) return "Não foi possível acessar este perfil.";
        if (httpStatus >= 500) return "Não foi possível concluir agora. Tente novamente.";
        return "Não foi possível confirmar seu perfil. Tente novamente.";
    }

    /** Traduz a falha de sincronização do perfil para a mensagem apresentada ao usuário. */
    private String syncError(Throwable failure) {
        String host = null;
        try { host = java.net.URI.create(BuildConfig.API_BASE_URL.trim()).getHost(); }
        catch (IllegalArgumentException ignored) {}
        return ProfileSyncError.message(failure, "10.0.2.2".equals(host));
    }

    /** Traduz a falha de autenticação Firebase para a mensagem do formulário. */
    private static String firebaseError(Exception exception, String fallback) {
        if (exception instanceof FirebaseNetworkException) return "Sem conexão. Verifique sua internet.";
        if (!(exception instanceof FirebaseAuthException)) return fallback;
        String code = ((FirebaseAuthException) exception).getErrorCode();
        switch (code) {
            case "ERROR_INVALID_EMAIL": return "Informe um e-mail válido.";
            case "ERROR_INVALID_CREDENTIAL":
            case "ERROR_WRONG_PASSWORD":
            case "ERROR_USER_NOT_FOUND": return "E-mail ou senha incorretos.";
            case "ERROR_EMAIL_ALREADY_IN_USE": return "Este e-mail já possui conta. Entre ou conclua o cadastro existente.";
            case "ERROR_WEAK_PASSWORD": return "Use uma senha mais forte (mínimo de 8 caracteres).";
            case "ERROR_TOO_MANY_REQUESTS": return "Muitas tentativas. Aguarde um pouco e tente novamente.";
            case "ERROR_OPERATION_NOT_ALLOWED":
                return "Este método de acesso não está disponível no momento.";
            case "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL":
                return "Este e-mail já usa outro método de acesso. Entre com o método original.";
            default: return fallback;
        }
    }
}
