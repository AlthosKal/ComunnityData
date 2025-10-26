package com.senasoft.comunidataapi.chat.service.function.list;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.senasoft.comunidataapi.csv.entity.CitizenReport;
import com.senasoft.comunidataapi.csv.repository.CitizenReportRepository;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FilterByGovernmentAttentionFunction
        implements Function<FilterByGovernmentAttentionFunction.Request, List<CitizenReport>> {

    private final CitizenReportRepository repository;

    @JsonClassDescription("Request para filtrar reportes por atención previa del gobierno")
    public record Request(
            @JsonProperty(required = true, value = "hasAttention")
                    @JsonPropertyDescription(
                            "Si el problema ha recibido atención previa del gobierno: true o false")
                    Boolean hasAttention) {}

    @Override
    public List<CitizenReport> apply(Request request) {
        return repository.findByAtencionPreviaGobierno(request.hasAttention());
    }
}
