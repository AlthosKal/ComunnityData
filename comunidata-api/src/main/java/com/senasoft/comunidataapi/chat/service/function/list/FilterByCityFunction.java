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
public class FilterByCityFunction implements Function<FilterByCityFunction.Request, List<CitizenReport>> {

    private final CitizenReportRepository repository;

    @JsonClassDescription("Request para filtrar reportes por ciudad")
    public record Request(
            @JsonProperty(required = true, value = "city")
                    @JsonPropertyDescription("Nombre de la ciudad (ej: 'Manizales', 'Bogotá')")
                    String city) {}

    @Override
    public List<CitizenReport> apply(Request request) {
        return repository.findByCiudad(request.city());
    }
}
