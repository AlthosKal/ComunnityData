package com.senasoft.comunidataapi.chat.service.function.list;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.senasoft.comunidataapi.csv.entity.CitizenReport;
import com.senasoft.comunidataapi.csv.enums.Zone;
import com.senasoft.comunidataapi.csv.repository.CitizenReportRepository;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FilterByZoneFunction
        implements Function<FilterByZoneFunction.Request, List<CitizenReport>> {

    private final CitizenReportRepository repository;

    @JsonClassDescription("Request para filtrar reportes por zona")
    public record Request(
            @JsonProperty(required = true, value = "zone")
                    @JsonPropertyDescription("Zona: 'Rural' o 'Urbana'")
                    String zone) {}

    @Override
    public List<CitizenReport> apply(Request request) {
        Zone zona = Zone.fromString(request.zone());
        if (zona == null) {
            return List.of();
        }
        return repository.findByZona(zona);
    }
}
