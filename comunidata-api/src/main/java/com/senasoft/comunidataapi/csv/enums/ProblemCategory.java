package com.senasoft.comunidataapi.csv.enums;

import lombok.Getter;

@Getter
public enum ProblemCategory {
    SALUD("Salud"),
    EDUCACION("Educación"),
    MEDIO_AMBIENTE("Medio Ambiente"),
    SEGURIDAD("Seguridad");

    private final String displayName;

    ProblemCategory(String displayName) {
        this.displayName = displayName;
    }

    public static ProblemCategory fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "salud", "health" -> SALUD;
            case "educacion", "educación", "education" -> EDUCACION;
            case "medio ambiente", "medioambiente", "environment" -> MEDIO_AMBIENTE;
            case "seguridad", "security" -> SEGURIDAD;
            default -> null;
        };
    }
}
