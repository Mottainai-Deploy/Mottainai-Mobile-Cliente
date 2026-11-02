package com.mottainai.cliente.repository;

/** Representa LoadState, responsável por o acesso e o tratamento dos dados do aplicativo. */
public class LoadState<T> {
    /** Agrupa as opções de Status, responsável por o acesso e o tratamento dos dados do aplicativo. */
    public enum Status { LOADING, SUCCESS, ERROR }

    public final Status status;
    public final T data;
    public final String message;
    public final int httpStatus;

    /** Inicializa LoadState com os dados e as dependências recebidos. */
    private LoadState(Status status, T data, String message, int httpStatus) {
        this.status = status;
        this.data = data;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    /** Cria o estado de carregamento utilizado pelos observadores da interface. */
    public static <T> LoadState<T> loading() {
        return new LoadState<>(Status.LOADING, null, null, 0);
    }

    /** Cria o estado de carregamento utilizado pelos observadores da interface. */
    public static <T> LoadState<T> loading(T previousData) {
        return new LoadState<>(Status.LOADING, previousData, null, 0);
    }

    /** Recebe o resultado bem-sucedido e dá continuidade à operação solicitada. */
    public static <T> LoadState<T> success(T data) {
        return new LoadState<>(Status.SUCCESS, data, null, 0);
    }

    /** Trata a falha recebida e informa o erro ao fluxo responsável. */
    public static <T> LoadState<T> error(String message, int httpStatus) {
        return new LoadState<>(Status.ERROR, null, message, httpStatus);
    }

    /** Trata a falha recebida e informa o erro ao fluxo responsável. */
    public static <T> LoadState<T> error(String message, int httpStatus, T previousData) {
        return new LoadState<>(Status.ERROR, previousData, message, httpStatus);
    }
}
