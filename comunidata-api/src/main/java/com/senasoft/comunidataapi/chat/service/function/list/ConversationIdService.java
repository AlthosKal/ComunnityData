package com.senasoft.comunidataapi.chat.service.function.list;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ConversationIdService {
    public String generateConversationId() {
        return "conv_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
