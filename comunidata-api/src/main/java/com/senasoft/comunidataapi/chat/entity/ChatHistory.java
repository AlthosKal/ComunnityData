package com.senasoft.comunidataapi.chat.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "chat_history")
public class ChatHistory {
    @Id private String id;
    private String conversationId;
    private String prompt;
    private Object response;
    private LocalDateTime date = LocalDateTime.now();

    public ChatHistory(String conversationId, String prompt, Object response) {
        this.conversationId = conversationId;
        this.prompt = prompt;
        this.response = response;
        this.date = LocalDateTime.now();
    }
}
