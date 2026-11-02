package com.mottainai.cliente.notifications;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Tag;
import retrofit2.http.Url;

/** Contrato da caixa de entrada HTTP publicada pela API Redis. Não cria notificações no servidor. */
public interface RedisNotificationApi {
    /** Consulta os avisos da identidade verificada pelo servidor. */
    @GET("v1/notifications") Call<Inbox> list(@Header("Authorization") String bearer, @Query("limit") int limit);

    /** Confirma a leitura do aviso pertencente à mesma identidade. */
    @POST("v1/notifications/{id}/read") Call<Receipt> read(@Header("Authorization") String bearer, @Path("id") String id);

    /** Contrato proposto para a API Cliente; só é chamado quando o caminho estiver configurado. */
    interface TokenApi {
        /** Solicita um token específico de notificações usando a sessão Firebase atual. */
        @POST Call<Access> issue(@Url String path, @Tag String firebaseUid);
    }

    /** Token curto emitido no servidor; nunca é salvo em preferências ou incluído no APK. */
    final class Access {
        public String accessToken;
        public long expiresIn;
        /** Confere o formato de transporte e a validade máxima, sem tentar validar uma assinatura no app. */
        public boolean valid() {
            return accessToken != null && accessToken.length() <= 8192
                    && accessToken.matches("[A-Za-z0-9_-]+[.][A-Za-z0-9_-]+[.][A-Za-z0-9_-]+")
                    && expiresIn > 0 && expiresIn <= 3600;
        }
    }

    /** Página retornada pelo endpoint, cujo contador pode ser maior que o limite da lista. */
    final class Inbox { public int unreadCount; public List<Item> notifications; }

    /** Conteúdo da caixa Redis com metadados opcionais da promoção e seu vencimento. */
    final class Item {
        public String id, title, body, status;
        public int priority;
        public long createdAt;
        public Integer promotionId, storeId, productId;
        public Long expiresAt;
        /** Converte o contrato em segundos sem alterar avisos antigos que usavam milissegundos. */
        public long createdAtMillis() { return createdAt < 100000000000L ? createdAt * 1000 : createdAt; }
        /** Oculta avisos de promoções cujo período já terminou. */
        public boolean expired() { return expiresAt != null && expiresAt <= System.currentTimeMillis(); }
        /** Valida os campos que podem ser apresentados e usados na ação de leitura. */
        public boolean valid() {
            return validId(id) && title != null && !title.trim().isEmpty() && title.length() <= 120
                    && body != null && body.length() <= 1000 && priority >= 1 && priority <= 3
                    && ("read".equals(status) || "unread".equals(status)) && createdAt > 0
                    && ((promotionId == null && storeId == null && productId == null && expiresAt == null)
                        || (promotionId != null && promotionId > 0 && storeId != null && storeId > 0
                            && productId != null && productId > 0 && expiresAt != null && expiresAt > 100000000000L));
        }
    }

    /** Confirmação idempotente de leitura enviada pela API Redis. */
    final class Receipt { public String id, status; }

    /** Aceita identificadores simples para impedir interpretação como caminho ou chave Redis. */
    static boolean validId(String id) {
        return id != null && !id.equals(".") && !id.equals("..") && id.matches("[A-Za-z0-9._-]{1,128}");
    }
}
