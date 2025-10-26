package com.senasoft.comunidataapi.csv.enums;

import lombok.Getter;

@Getter
public enum ProcessingStatus {
    PENDIENTE("Pendiente"),
    NORMALIZANDO("Normalizando"),
    PROCESANDO_IA("Procesando con IA"),
    GENERANDO_EMBEDDINGS("Generando Embeddings"),
    COMPLETADO("Completado"),
    ERROR("Error");

    private final String displayName;

    ProcessingStatus(String displayName) {
        this.displayName = displayName;
    }
}
