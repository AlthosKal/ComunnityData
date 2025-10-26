package com.senasoft.comunidataapi.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Falta adaptar la respuesta para el contexto de este proyecto
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CharDataDTO {
    private String label; // Ej: "Abril", "2025-06-01", "Semana 22"
    private double value; // monto numérico
}
