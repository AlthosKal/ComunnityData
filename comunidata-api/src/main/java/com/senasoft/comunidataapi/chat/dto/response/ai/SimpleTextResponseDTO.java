package com.senasoft.comunidataapi.chat.dto.response.ai;

import com.senasoft.comunidataapi.chat.enums.ResponseType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SimpleTextResponseDTO extends BaseDynamicResponseDTO {
    private String message;

    public SimpleTextResponseDTO(String summary, String analysis, String message) {
        super();
        this.setType(ResponseType.SIMPLE_TEXT);
        this.setSummary(summary);
        this.setAnalysis(analysis);
        this.message = message;
    }
}
