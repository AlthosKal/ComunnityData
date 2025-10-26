package com.senasoft.comunidataapi.chat.service.report;

import com.senasoft.comunidataapi.chat.dto.response.ai.BaseDynamicResponseDTO;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

public interface ReportGenerationService {
    BaseDynamicResponseDTO generateReportResponse(
            String prompt, String functionName, Object data, String reportType);

    ResponseEntity<Resource> downloadReport(String reportId);
}
