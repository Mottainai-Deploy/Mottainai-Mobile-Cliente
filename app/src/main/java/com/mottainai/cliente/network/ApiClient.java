package com.mottainai.cliente.network;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mottainai.cliente.activities.LoginClienteActivity;
import com.mottainai.cliente.utils.FirebaseSupport;
import com.mottainai.cliente.utils.SessionManager;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {

    private ApiClient() {
    }

    @NonNull
    public static ApiService create(@NonNull Context context, @NonNull String baseUrl) {
        return create(context, baseUrl, ApiService.class);
    }

    @NonNull
    public static <T> T create(@NonNull Context context, @NonNull String baseUrl,
                               @NonNull Class<T> serviceClass) {
        return create(context, baseUrl, serviceClass, true);
    }

    @NonNull
    public static <T> T create(@NonNull Context context, @NonNull String baseUrl,
                               @NonNull Class<T> serviceClass, boolean invalidateAppSessionOnUnauthorized) {
        if (baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("A base URL da API deve ser configurada pelo ambiente.");
        }

        SessionManager sessionManager = new SessionManager(context);
        boolean profileService = serviceClass == ApiService.class;
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(new FirebaseTokenInterceptor(context.getApplicationContext()))
                .authenticator(new FirebaseTokenAuthenticator(context.getApplicationContext(), sessionManager,
                        invalidateAppSessionOnUnauthorized))
                .connectTimeout(profileService ? 5 : 10, TimeUnit.SECONDS)
                .readTimeout(profileService ? 10 : 20, TimeUnit.SECONDS)
                .callTimeout(profileService ? 20 : 40, TimeUnit.SECONDS)
                // Nunca reenvia silenciosamente escritas de perfil após falha ambígua de conexão.
                .retryOnConnectionFailure(!profileService)
                .build();
        Gson gson = new GsonBuilder().create();

        return new Retrofit.Builder()
                .baseUrl(normalizeBaseUrl(baseUrl))
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(serviceClass);
    }

    private static String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl.trim();
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    private static final class FirebaseTokenInterceptor implements Interceptor {

        private final Context context;

        private FirebaseTokenInterceptor(Context context) {
            this.context = context;
        }

        @Override
        @NonNull
        public Response intercept(@NonNull Chain chain) throws IOException {
            Request request = chain.request();
            FirebaseAuth auth = FirebaseSupport.authIfConfigured(context);
            FirebaseUser user = auth == null ? null : auth.getCurrentUser();
            String expectedUid = request.tag(String.class);
            if (expectedUid != null && (user == null || !expectedUid.equals(user.getUid()))) {
                throw new IOException("Firebase account changed before request");
            }
            if (user == null || request.header("Authorization") != null) return chain.proceed(request);
            try {
                String token = Tasks.await(user.getIdToken(false), 15, TimeUnit.SECONDS).getToken();
                if (token == null || token.isEmpty()) throw new IOException("Firebase session token unavailable");
                FirebaseUser current = auth.getCurrentUser();
                if (current == null || !current.getUid().equals(user.getUid())) {
                    throw new IOException("Firebase account changed before request");
                }
                return chain.proceed(request.newBuilder()
                        .tag(FirebaseUser.class, user)
                        .header("Authorization", "Bearer " + token).build());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException("Firebase session token unavailable", exception);
            } catch (ExecutionException | TimeoutException exception) {
                throw new IOException("Firebase session token unavailable", exception);
            }
        }
    }

    private static final class FirebaseTokenAuthenticator implements Authenticator {

        private final Context context;
        private final SessionManager sessionManager;
        private final boolean invalidateAppSessionOnUnauthorized;

        private FirebaseTokenAuthenticator(Context context, SessionManager sessionManager,
                                           boolean invalidateAppSessionOnUnauthorized) {
            this.context = context;
            this.sessionManager = sessionManager;
            this.invalidateAppSessionOnUnauthorized = invalidateAppSessionOnUnauthorized;
        }

        @Override
        public Request authenticate(okhttp3.Route route, Response response) {
            FirebaseAuth auth = FirebaseSupport.authIfConfigured(context);
            FirebaseUser user = auth == null ? null : auth.getCurrentUser();
            FirebaseUser requestedUser = response.request().tag(FirebaseUser.class);
            if (user == null) {
                expireSession(requestedUser == null ? null : requestedUser.getUid(), true);
                return null;
            }
            if (requestedUser == null || !requestedUser.getUid().equals(user.getUid())) return null;
            if (responseCount(response) >= 2) {
                expireSession(user.getUid(), false);
                return null;
            }
            try {
                // Todos os serviços de API compartilham este bloqueio para que 401 simultâneos reutilizem um token renovado.
                synchronized (FirebaseTokenAuthenticator.class) {
                    FirebaseUser current = auth.getCurrentUser();
                    if (current == null || !current.getUid().equals(user.getUid())) return null;
                    String token = Tasks.await(user.getIdToken(false), 15, TimeUnit.SECONDS).getToken();
                    if (token == null || ("Bearer " + token).equals(response.request().header("Authorization"))) {
                        token = Tasks.await(user.getIdToken(true), 15, TimeUnit.SECONDS).getToken();
                    }
                    if (token == null || token.isEmpty()) return null;
                    current = auth.getCurrentUser();
                    if (current == null || !current.getUid().equals(user.getUid())) return null;
                    return response.request().newBuilder().header("Authorization", "Bearer " + token).build();
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return null;
            } catch (ExecutionException exception) {
                if (exception.getCause() instanceof FirebaseAuthInvalidUserException) {
                    expireSession(user.getUid(), true);
                }
                return null;
            } catch (TimeoutException exception) {
                return null;
            }
        }

        private void expireSession(String expectedUid, boolean signOut) {
            if (!invalidateAppSessionOnUnauthorized) return;
            new Handler(Looper.getMainLooper()).post(() -> {
                FirebaseAuth auth = FirebaseSupport.authIfConfigured(context);
                FirebaseUser current = auth == null ? null : auth.getCurrentUser();
                if (current != null && (expectedUid == null || !current.getUid().equals(expectedUid))) return;
                if (sessionManager.getClientId() == null) return;
                if (signOut) sessionManager.clearSession();
                else sessionManager.clearClientSession();
                Intent login = new Intent(context, LoginClienteActivity.class);
                login.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                context.startActivity(login);
            });
        }

        private static int responseCount(Response response) {
            int count = 0;
            while (response != null) {
                if (response.code() == 401) count++;
                response = response.priorResponse();
            }
            return count;
        }
    }
}
