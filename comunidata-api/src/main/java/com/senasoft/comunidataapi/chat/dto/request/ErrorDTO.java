package com.senasoft.comunidataapi.chat.dto.request;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorDTO {
    private String description;
    private List<String> reasons;
}
