package com.senasoft.comunidataapi.chat.service.function.list;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.senasoft.comunidataapi.csv.entity.CitizenReport;
import com.senasoft.comunidataapi.csv.repository.CitizenReportRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FilterByReportDateFunction
        implements Function<FilterByReportDateFunction.Request, List<CitizenReport>> {

    private final CitizenReportRepository repository;

    @JsonClassDescription("Request para filtrar reportes por rango de fechas")
    public record Request(
            @JsonProperty(required = true, value = "startDate")
                    @JsonPropertyDescription("Fecha de inicio en formato YYYY-MM-DD (ej: '2023-01-01')")
                    String startDate,
            @JsonProperty(required = true, value = "endDate")
                    @JsonPropertyDescription("Fecha de fin en formato YYYY-MM-DD (ej: '2023-12-31')")
                    String endDate) {}

    @Override
    public List<CitizenReport> apply(Request request) {
        LocalDate start = LocalDate.parse(request.startDate());
        LocalDate end = LocalDate.parse(request.endDate());
        return repository.findByFechaReporteBetween(start, end);
    }
}
