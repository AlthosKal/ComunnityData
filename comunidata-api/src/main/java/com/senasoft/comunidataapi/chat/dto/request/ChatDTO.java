package com.senasoft.comunidataapi.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatDTO {

    private String conversationId;

    @NotBlank(message = "The prompt must be defined")
    private String prompt;

    public boolean needsConversationId() {
        return conversationId == null || conversationId.trim().isEmpty();
    }
}
