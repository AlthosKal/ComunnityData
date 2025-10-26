package com.senasoft.comunidataapi.chat.dto.response;

import com.senasoft.comunidataapi.chat.dto.response.ai.BaseDynamicResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DynamicAnalysisResponseDTO {
    private StringChatResponseDTO body;
    private BaseDynamicResponseDTO response;
}
