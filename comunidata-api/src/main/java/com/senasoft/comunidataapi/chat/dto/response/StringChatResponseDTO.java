package com.senasoft.comunidataapi.chat.dto.response;

import com.senasoft.comunidataapi.chat.dto.response.ai.ChartDataResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StringChatResponseDTO {
    private String conversationId;
    private String response;
    private ChartDataResponseDTO chartData;
}
