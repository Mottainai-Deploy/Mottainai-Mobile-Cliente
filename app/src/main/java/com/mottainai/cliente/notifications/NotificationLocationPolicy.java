package com.mottainai.cliente.notifications;

/** Limites compartilhados com a API para não enviar uma posição antiga como se fosse atual. */
final class NotificationLocationPolicy {
    /** Impede instanciar a política de validação. */
    private NotificationLocationPolicy() {}
    /** Aceita somente coordenadas, precisão e instante dentro do contrato do servidor. */
    static boolean valid(double latitude, double longitude, double accuracy, long observedAt, long now) {
        long age=now-observedAt;
        return Double.isFinite(latitude) && Double.isFinite(longitude) && Double.isFinite(accuracy)
            && Math.abs(latitude)<=90 && Math.abs(longitude)<=180 && accuracy>=0 && accuracy<=10000
            && observedAt>0 && age>=-30000 && age<=120000;
    }
}