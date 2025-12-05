package com.example.backend.mappers;

import com.example.backend.dtos.response.MessageResponseDTO;
import com.example.backend.dtos.response.ConversationResponseDTO;
import com.example.backend.models.DirectMessage;
import com.example.backend.models.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageMapper {

    private final UserMapper userMapper;

    public MessageResponseDTO toMessaggioResponseDTO(DirectMessage message) {
        if (message == null) return null;

        return MessageResponseDTO.builder()
                .id(message.getId())
                .mittente(userMapper.toUtenteSummaryDTO(message.getSender()))
                .destinatario(userMapper.toUtenteSummaryDTO(message.getReceiver()))
                .contenuto(message.getContent())
                .isRead(message.isRead())
                .isDeletedBySender(message.isDeletedBySender())
                .createdAt(message.getCreatedAt())
                .build();
    }

    public ConversationResponseDTO toConversazioneResponseDTO(
            User altroUtente,
            DirectMessage ultimoMessaggio,
            Integer messaggiNonLetti) {

        return ConversationResponseDTO.builder()
                .altroUtente(userMapper.toUtenteSummaryDTO(altroUtente))
                .ultimoMessaggio(toMessaggioResponseDTO(ultimoMessaggio))
                .messaggiNonLetti(messaggiNonLetti)
                .ultimaAttivita(ultimoMessaggio != null ?
                        ultimoMessaggio.getCreatedAt() : null)
                .build();
    }
}