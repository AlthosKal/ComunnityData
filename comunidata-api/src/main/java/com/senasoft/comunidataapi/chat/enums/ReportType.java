package com.senasoft.comunidataapi.chat.enums;

/** Tipos de reportes disponibles en el sistema */
public enum ReportType {
    PDF("application/pdf", ".pdf"),
    EXCEL("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx");

    private final String mimeType;
    private final String extension;

    ReportType(String mimeType, String extension) {
        this.mimeType = mimeType;
        this.extension = extension;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getExtension() {
        return extension;
    }
}
