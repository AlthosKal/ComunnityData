package com.senasoft.comunidataapi.csv.service.processing;

import com.senasoft.comunidataapi.csv.entity.CitizenReport;
import java.util.List;

/**
 * Servicio para generación de embeddings de reportes ciudadanos.
 *
 * <p>Genera embeddings vectoriales del comentario usando OpenAI text-embedding-3-small para
 * búsquedas semánticas con RAG.
 */
public interface EmbeddingGenerationService {

    /**
     * Genera embeddings para una lista de reportes.
     *
     * @param reports Reportes que necesitan embeddings
     * @return Reportes con embeddings generados
     */
    List<CitizenReport> generateEmbeddings(List<CitizenReport> reports);

    /**
     * Genera embedding para un reporte individual.
     *
     * @param report Reporte que necesita embedding
     * @return Reporte con embedding generado
     */
    CitizenReport generateEmbedding(CitizenReport report);
}
