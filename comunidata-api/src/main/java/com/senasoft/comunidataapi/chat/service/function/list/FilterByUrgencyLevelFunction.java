package com.senasoft.comunidataapi.chat.service.function.list;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.senasoft.comunidataapi.csv.entity.CitizenReport;
import com.senasoft.comunidataapi.csv.enums.UrgencyLevel;
import com.senasoft.comunidataapi.csv.repository.CitizenReportRepository;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FilterByUrgencyLevelFunction
        implements Function<FilterByUrgencyLevelFunction.Request, List<CitizenReport>> {

    private final CitizenReportRepository repository;

    @JsonClassDescription("Request para filtrar reportes por nivel de urgencia")
    public record Request(
            @JsonProperty(required = true, value = "urgencyLevel")
                    @JsonPropertyDescription(
                            "Nivel de urgencia: 'Urgente', 'Alta', 'Media', 'Baja'")
                    String urgencyLevel) {}

    @Override
    public List<CitizenReport> apply(Request request) {
        UrgencyLevel urgencia = UrgencyLevel.fromString(request.urgencyLevel());
        if (urgencia == null) {
            return List.of();
        }
        return repository.findByNivelUrgencia(urgencia);
    }
}
