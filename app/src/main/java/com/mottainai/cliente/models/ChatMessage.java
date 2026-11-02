package com.mottainai.cliente.models;

/** Representa ChatMessage, responsável por os dados apresentados pela interface. */
public class ChatMessage {

    /** Agrupa as opções de Sender, responsável por os dados apresentados pela interface. */
    public enum Sender {
        BOT, USER
    }

    private final Sender sender;
    private final String text;
    private final Offer attachedOffer;

    /** Inicializa ChatMessage com os dados e as dependências recebidos. */
    public ChatMessage(Sender sender, String text) {
        this(sender, text, null);
    }

    /** Inicializa ChatMessage com os dados e as dependências recebidos. */
    public ChatMessage(Sender sender, String text, Offer attachedOffer) {
        this.sender = sender;
        this.text = text;
        this.attachedOffer = attachedOffer;
    }

    /** Retorna quem enviou a mensagem. */
    public Sender getSender() {
        return sender;
    }

    /** Retorna o texto da mensagem. */
    public String getText() {
        return text;
    }

    /** Retorna a oferta anexada à mensagem. */
    public Offer getAttachedOffer() {
        return attachedOffer;
    }

    /** Indica se a mensagem contém uma oferta anexada. */
    public boolean hasAttachedOffer() {
        return attachedOffer != null;
    }
}
