package com.senasoft.comunidataapi.chat.dto.response.ai;

import com.senasoft.comunidataapi.chat.dto.response.CharDataDTO;
import com.senasoft.comunidataapi.chat.enums.ResponseType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ChartDataResponseDTO extends BaseDynamicResponseDTO {
    private String chartType; // line, bar, pie, etc.
    private List<CharDataDTO> data;
    private String xAxisLabel;
    private String yAxisLabel;

    public ChartDataResponseDTO(
            String summary,
            String analysis,
            String chartType,
            List<CharDataDTO> data,
            String xAxisLabel,
            String yAxisLabel) {
        super();
        this.setType(ResponseType.CHART_DATA);
        this.setSummary(summary);
        this.setAnalysis(analysis);
        this.chartType = chartType;
        this.data = data;
        this.xAxisLabel = xAxisLabel;
        this.yAxisLabel = yAxisLabel;
    }
}
