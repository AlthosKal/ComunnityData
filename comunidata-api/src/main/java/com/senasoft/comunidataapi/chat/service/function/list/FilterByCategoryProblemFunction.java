package com.senasoft.comunidataapi.chat.service.function.list;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.senasoft.comunidataapi.csv.entity.CitizenReport;
import com.senasoft.comunidataapi.csv.enums.ProblemCategory;
import com.senasoft.comunidataapi.csv.repository.CitizenReportRepository;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FilterByCategoryProblemFunction
        implements Function<FilterByCategoryProblemFunction.Request, List<CitizenReport>> {

    private final CitizenReportRepository repository;

    @JsonClassDescription("Request para filtrar reportes por categoría del problema")
    public record Request(
            @JsonProperty(required = true, value = "category")
                    @JsonPropertyDescription(
                            "Categoría del problema: 'Salud', 'Educación', 'Medio Ambiente', 'Seguridad'")
                    String category) {}

    @Override
    public List<CitizenReport> apply(Request request) {
        ProblemCategory categoria = ProblemCategory.fromString(request.category());
        if (categoria == null) {
            return List.of();
        }
        return repository.findByCategoriaProblema(categoria);
    }
}
