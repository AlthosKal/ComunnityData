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
public class FilterByAgeFunction implements Function<FilterByAgeFunction.Request, List<CitizenReport>> {

    private final CitizenReportRepository repository;

    @JsonClassDescription("Request para filtrar reportes por rango de edad")
    public record Request(
            @JsonProperty(required = true, value = "minAge")
                    @JsonPropertyDescription("Edad mínima del rango (ej: 18)")
                    Integer minAge,
            @JsonProperty(required = true, value = "maxAge")
                    @JsonPropertyDescription("Edad máxima del rango (ej: 65)")
                    Integer maxAge) {}

    @Override
    public List<CitizenReport> apply(Request request) {
        return repository.findByEdadBetween(request.minAge(), request.maxAge());
    }
}
