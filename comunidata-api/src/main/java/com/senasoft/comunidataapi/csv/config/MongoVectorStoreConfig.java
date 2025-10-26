package com.senasoft.comunidataapi.csv.config;

import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.mongodb.atlas.MongoDBAtlasVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Configuración para MongoDB Atlas Vector Store y modelo de embeddings.
 *
 * <p>Configura el sistema RAG (Retrieval-Augmented Generation) usando: - MongoDB Atlas para
 * almacenamiento vectorial - OpenAI text-embedding-3-small para generar embeddings
 */
@Configuration
public class MongoVectorStoreConfig {

    @Value("${spring.ai.openai.api-key}")
    private String openAiApiKey;

    /**
     * Configura el modelo de embeddings de OpenAI.
     *
     * <p>Usa text-embedding-3-small que genera vectores de 1536 dimensiones, optimizado para
     * búsquedas semánticas de alta calidad con buen rendimiento.
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(openAiApiKey)
                .build();

        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model("text-embedding-3-small")
                .dimensions(1536)
                .build();

        return new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED, options);
    }

    /**
     * NOTA: El VectorStore ahora se configura automáticamente por Spring AI.
     *
     * <p>La configuración se realiza mediante propiedades en application.yml:
     * <pre>
     * spring:
     *   ai:
     *     vectorstore:
     *       mongodb:
     *         collection-name: citizen_reports
     *         path-name: embedding
     *         initialize-schema: true
     *         vector-index-name: report_embeddings_index
     * </pre>
     *
     * <p>IMPORTANTE: Antes de usar este VectorStore, debes crear un índice vectorial en MongoDB
     * Atlas:
     * <ol>
     *   <li>Ve a tu cluster en MongoDB Atlas</li>
     *   <li>Selecciona la base de datos</li>
     *   <li>En la colección 'citizen_reports', crea un Search Index</li>
     *   <li>Usa la siguiente configuración JSON:</li>
     * </ol>
     *
     * <pre>{@code
     * {
     *   "mappings": {
     *     "dynamic": true,
     *     "fields": {
     *       "embedding": {
     *         "type": "knnVector",
     *         "dimensions": 1536,
     *         "similarity": "cosine"
     *       }
     *     }
     *   }
     * }
     * }</pre>
     *
     * <p>5. Nombra el índice como: "report_embeddings_index"
     */

    // Bean comentado - ahora se usa autoconfiguración de Spring AI
    // El VectorStore se crea automáticamente con las propiedades de application.yml
    /*
    @Bean
    public VectorStore vectorStore(MongoTemplate mongoTemplate, EmbeddingModel embeddingModel) {
        return MongoDBAtlasVectorStore.builder(mongoTemplate, embeddingModel)
                .collectionName("citizen_reports")
                .vectorIndexName("report_embeddings_index")
                .pathName("embedding")
                .initializeSchema(true)
                .build();
    }
    */
}
