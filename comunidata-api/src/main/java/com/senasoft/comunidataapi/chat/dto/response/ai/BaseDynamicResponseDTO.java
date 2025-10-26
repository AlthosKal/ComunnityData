package com.senasoft.comunidataapi.chat.dto.response.ai;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.senasoft.comunidataapi.chat.enums.ResponseType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = SimpleTextResponseDTO.class, name = "SIMPLE_TEXT"),
    @JsonSubTypes.Type(value = ReportDownloadResponseDTO.class, name = "REPORT_DOWNLOAD")
})
public abstract class BaseDynamicResponseDTO {
    private ResponseType type;
    private String summary;
    private String analysis;
}
