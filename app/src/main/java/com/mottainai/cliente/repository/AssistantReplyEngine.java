package com.mottainai.cliente.repository;

import com.mottainai.cliente.models.ChatMessage;
import com.mottainai.cliente.models.Offer;
import com.mottainai.cliente.models.UserProfile;

import java.util.Locale;

/**
 * Pequeno gerador de respostas por palavras-chave usado no lugar do backend
 * real da assistente. Suficiente para manter a tela de chat utilizável enquanto o
 * modelo de linguagem ainda não está conectado.
 */
public class AssistantReplyEngine {

    private final MockOfferRepository offerRepository;
    private final MockUserRepository userRepository;

    /** Inicializa AssistantReplyEngine com os dados e as dependências recebidos. */
    public AssistantReplyEngine(MockOfferRepository offerRepository, MockUserRepository userRepository) {
        this.offerRepository = offerRepository;
        this.userRepository = userRepository;
    }

    /** Produz a resposta da assistente para a mensagem informada. */
    public ChatMessage reply(String userText) {
        String normalized = userText.toLowerCase(Locale.getDefault());

        if (normalized.contains("café") || normalized.contains("cafe") || normalized.contains("oferta")
                || normalized.contains("desconto")) {
            Offer offer = offerRepository.findById("cafe-em-po");
            return new ChatMessage(ChatMessage.Sender.BOT,
                    "Encontrei uma opção a menos de 1 km que ainda está disponível hoje.", offer);
        }

        if (normalized.contains("ponto")) {
            UserProfile user = userRepository.getCurrentUser();
            return new ChatMessage(ChatMessage.Sender.BOT, "Você tem "
                    + String.format(Locale.forLanguageTag("pt-BR"), "%,d", user.getPointsBalance())
                    + " pontos acumulados — isso é " + user.getLevelLabel() + ", faltam "
                    + (100 - user.getLevelProgressPercent()) + "% para o próximo nível.");
        }

        if (normalized.contains("impacto")) {
            UserProfile user = userRepository.getCurrentUser();
            return new ChatMessage(ChatMessage.Sender.BOT, "Até agora você evitou "
                    + String.format(Locale.getDefault(), "%.1f", user.getKgAvoided()).replace('.', ',')
                    + " kg de alimentos desperdiçados e economizou R$ "
                    + String.format(Locale.getDefault(), "%.2f", user.getMoneySavedBrl()).replace('.', ',')
                    + " com suas escolhas.");
        }

        return new ChatMessage(ChatMessage.Sender.BOT,
                "Ainda estou aprendendo sobre isso, mas já registrei sua pergunta! "
                        + "Tente perguntar sobre ofertas, pontos ou impacto.");
    }
}
