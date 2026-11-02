package com.mottainai.cliente.notifications;

import android.content.Context;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mottainai.cliente.BuildConfig;
import com.mottainai.cliente.network.ApiClient;
import com.mottainai.cliente.utils.FirebaseSupport;
import com.mottainai.cliente.utils.SessionManager;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Consulta a API Redis com credencial própria, sem enviar token Firebase a ela ou invalidar o login do app. */
public final class RedisNotificationRepository {
    private final Context context;
    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS).retryOnConnectionFailure(false).build();

    /** Mantém apenas o contexto da aplicação para consultas em primeiro e segundo plano. */
    public RedisNotificationRepository(Context context) { this.context = context.getApplicationContext(); }

    /** Exige a URL Redis e um caminho relativo de troca de token explicitamente configurado na API Cliente. */
    public static boolean configured() {
        HttpUrl url = HttpUrl.parse(BuildConfig.NOTIFICATIONS_BASE_URL);
        return url != null && url.username().isEmpty() && url.password().isEmpty()
                && (url.isHttps() || BuildConfig.DEBUG)
                && !BuildConfig.API_BASE_URL.trim().isEmpty()
                && BuildConfig.NOTIFICATIONS_TOKEN_PATH.matches("api/v1/client/[A-Za-z0-9_/-]+")
                && !BuildConfig.NOTIFICATIONS_TOKEN_PATH.contains("..");
    }

    /** Identifica o usuário Firebase e o perfil SQL da sessão, sem guardar um token de acesso. */
    public static String owner(Context context) {
        SessionManager session = new SessionManager(context);
        FirebaseAuth auth = FirebaseSupport.authIfConfigured(context);
        FirebaseUser user = auth == null ? null : auth.getCurrentUser();
        return !session.canBrowse() || user == null ? null : user.getUid() + ":" + session.getClientId();
    }

    /** Descarta respostas e ações que deixaram de pertencer à conta atual. */
    public static boolean owns(Context context, String owner) {
        return owner != null && owner.equals(owner(context));
    }

    /** Consulta os avisos com limite máximo publicado de cem itens, fora da thread da interface. */
    public RedisNotificationApi.Inbox load(String owner) throws IOException {
        String token = token(owner);
        Response<RedisNotificationApi.Inbox> response = api().list("Bearer " + token, 100).execute();
        checkOwner(owner);
        RedisNotificationApi.Inbox inbox = response.body();
        if (!response.isSuccessful() || inbox == null || inbox.notifications == null || inbox.unreadCount < 0)
            throw new IOException("Não foi possível buscar seus avisos agora. Tente novamente.");
        return inbox;
    }

    /** Marca como lido somente após uma ação do usuário e valida a confirmação retornada. */
    public void read(String owner, String id) throws IOException {
        if (!RedisNotificationApi.validId(id)) throw new IOException("Este aviso não está mais disponível.");
        String token = token(owner);
        Response<RedisNotificationApi.Receipt> response = api().read("Bearer " + token, id).execute();
        checkOwner(owner);
        RedisNotificationApi.Receipt receipt = response.body();
        if (!response.isSuccessful() || receipt == null || !id.equals(receipt.id) || !"read".equals(receipt.status))
            throw new IOException("Não foi possível confirmar a leitura. Tente novamente.");
    }

    /** Obtém uma credencial curta da API Cliente; não usa login legado, segredo compartilhado ou token fixo. */
    private String token(String owner) throws IOException {
        if (!configured()) throw new IOException("Os avisos ainda não estão disponíveis nesta versão.");
        checkOwner(owner);
        FirebaseAuth auth = FirebaseSupport.authIfConfigured(context);
        FirebaseUser user = auth == null ? null : auth.getCurrentUser();
        if (user == null) throw new IOException("Entre na sua conta para consultar os avisos.");
        RedisNotificationApi.TokenApi broker = ApiClient.create(context, BuildConfig.API_BASE_URL,
                RedisNotificationApi.TokenApi.class, false);
        Response<RedisNotificationApi.Access> response = broker.issue(BuildConfig.NOTIFICATIONS_TOKEN_PATH, user.getUid()).execute();
        checkOwner(owner);
        RedisNotificationApi.Access access = response.body();
        if (!response.isSuccessful() || access == null || !access.valid())
            throw new IOException("Não foi possível autorizar seus avisos agora. Tente novamente mais tarde.");
        return access.accessToken;
    }

    /** Cria o cliente Redis separado do interceptor Firebase da API Cliente. */
    private RedisNotificationApi api() {
        String base = BuildConfig.NOTIFICATIONS_BASE_URL;
        return new Retrofit.Builder().baseUrl(base.endsWith("/") ? base : base + "/")
                .client(HTTP).addConverterFactory(GsonConverterFactory.create()).build().create(RedisNotificationApi.class);
    }

    /** Interrompe a operação cancelada ou pertencente a uma conta que já saiu do aplicativo. */
    private void checkOwner(String owner) throws IOException {
        if (Thread.currentThread().isInterrupted() || !owns(context, owner))
            throw new IOException("Entre novamente para consultar os avisos desta conta.");
    }
}
