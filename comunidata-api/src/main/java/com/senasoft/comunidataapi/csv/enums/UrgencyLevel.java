package com.senasoft.comunidataapi.csv.enums;

import lombok.Getter;

@Getter
public enum UrgencyLevel {
    URGENTE("Urgente"),
    ALTA("Alta"),
    MEDIA("Media"),
    BAJA("Baja");

    private final String displayName;

    UrgencyLevel(String displayName) {
        this.displayName = displayName;
    }

    public static UrgencyLevel fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "urgente", "urgent", "crítico", "critico" -> URGENTE;
            case "alta", "high" -> ALTA;
            case "media", "medium", "moderada" -> MEDIA;
            case "baja", "low" -> BAJA;
            default -> null;
        };
    }
}
