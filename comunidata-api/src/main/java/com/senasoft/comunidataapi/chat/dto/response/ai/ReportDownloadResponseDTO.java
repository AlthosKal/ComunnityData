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
public class ReportDownloadResponseDTO extends BaseDynamicResponseDTO {
    private String reportId;
    private String fileName;
    private String reportType; // PDF o EXCEL
    private String reportCategory; // BALANCE, DEBT, SPENDING, etc.
    private long fileSizeBytes;
    private String expirationDate;

    public ReportDownloadResponseDTO(
            String summary,
            String analysis,
            String reportId,
            String fileName,
            String reportType,
            String reportCategory,
            long fileSizeBytes,
            String expirationDate) {
        super();
        this.setType(ResponseType.REPORT_DOWNLOAD);
        this.setSummary(summary);
        this.setAnalysis(analysis);
        this.reportId = reportId;
        this.fileName = fileName;
        this.reportType = reportType;
        this.reportCategory = reportCategory;
        this.fileSizeBytes = fileSizeBytes;
        this.expirationDate = expirationDate;
    }
}
