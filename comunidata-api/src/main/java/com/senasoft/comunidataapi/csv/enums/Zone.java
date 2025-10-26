package com.senasoft.comunidataapi.csv.enums;

import lombok.Getter;

@Getter
public enum Zone {
    RURAL("Rural"),
    URBANA("Urbana");

    private final String displayName;

    Zone(String displayName) {
        this.displayName = displayName;
    }

    public static Zone fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "rural", "0", "false" -> RURAL;
            case "urbana", "urbano", "urban", "1", "true" -> URBANA;
            default -> null;
        };
    }

    public static Zone fromBoolean(Boolean isRural) {
        if (isRural == null) return null;
        return isRural ? RURAL : URBANA;
    }
}
