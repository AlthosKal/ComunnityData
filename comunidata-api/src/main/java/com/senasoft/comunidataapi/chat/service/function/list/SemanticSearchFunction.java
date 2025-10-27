package com.senasoft.comunidataapi.chat.service.function.list;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

/**
 * Función para búsqueda semántica usando RAG.
 *
 * <p>Busca reportes similares usando embeddings vectoriales almacenados en MongoDB Atlas.
 */
@Component
@RequiredArgsConstructor
public class SemanticSearchFunction
        implements Function<SemanticSearchFunction.Request, List<String>> {

    private final VectorStore vectorStore;

    @JsonClassDescription("Request para búsqueda semántica en reportes ciudadanos")
    public record Request(
            @JsonProperty(required = true, value = "query")
                    @JsonPropertyDescription(
                            "Consulta en lenguaje natural (ej: 'problemas de salud en hospitales')")
                    String query,
            @JsonProperty(value = "topK")
                    @JsonPropertyDescription("Número de resultados a retornar (default: 5)")
                    Integer topK) {}

    @Override
    public List<String> apply(Request request) {
        int k = request.topK() != null ? request.topK() : 5;

        // Crear request de búsqueda
        SearchRequest searchRequest =
                SearchRequest.builder()
                        .query(request.query())
                        .topK(k)
                        .similarityThreshold(0.7) // Umbral de similitud
                        .build();

        // Realizar búsqueda vectorial
        List<Document> results = vectorStore.similaritySearch(searchRequest);

        // Extraer IDs de los documentos desde metadata
        return results.stream().map(doc -> doc.getId()).collect(Collectors.toList());
    }
}
