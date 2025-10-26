package com.senasoft.comunidataapi.chat.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ErrorDTO {
    private String description;
    private List<String> reasons;
}
