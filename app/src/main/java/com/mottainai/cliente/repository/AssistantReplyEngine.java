package com.mottainai.cliente.repository;

import com.mottainai.cliente.models.ChatMessage;
import com.mottainai.cliente.models.Offer;
import com.mottainai.cliente.models.UserProfile;

import java.util.Locale;

public class AssistantReplyEngine {

    private final MockOfferRepository offerRepository;
    private final MockUserRepository userRepository;

    public AssistantReplyEngine(MockOfferRepository offerRepository, MockUserRepository userRepository) {
        this.offerRepository = offerRepository;
        this.userRepository = userRepository;
    }

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
