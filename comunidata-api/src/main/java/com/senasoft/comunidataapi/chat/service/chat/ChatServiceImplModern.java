package com.senasoft.comunidataapi.chat.service.chat;

import com.senasoft.comunidataapi.chat.dto.request.*;
import com.senasoft.comunidataapi.chat.dto.response.DynamicAnalysisResponseDTO;
import com.senasoft.comunidataapi.chat.dto.response.StringChatResponseDTO;
import com.senasoft.comunidataapi.chat.dto.response.ai.BaseDynamicResponseDTO;
import com.senasoft.comunidataapi.chat.dto.response.ai.SimpleTextResponseDTO;
import com.senasoft.comunidataapi.chat.entity.ChatHistory;
import com.senasoft.comunidataapi.chat.enums.ApiError;
import com.senasoft.comunidataapi.chat.enums.Model;
import com.senasoft.comunidataapi.exception.ComuniDataException;
import com.senasoft.comunidataapi.chat.mapper.ChatHistoryForConversationMapper;
import com.senasoft.comunidataapi.chat.mapper.ChatHistoryMapper;
import com.senasoft.comunidataapi.chat.repository.AiHistoryRepository;
import com.senasoft.comunidataapi.chat.service.function.list.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * Implementación moderna de ChatService usando Native Function Calling.
 *
 * <p>Con Spring AI 1.0+, las funciones se registran automáticamente como @Bean y el modelo decide
 * cuándo invocarlas. No necesitamos detección manual de funciones.
 */
@Slf4j
@Service
public class ChatServiceImplModern implements ChatService {

    private final ChatClient chatClient;
    private final AiHistoryRepository repository;
    private final ChatHistoryForConversationMapper chatHistoryForConversationMapper;
    private final ChatHistoryMapper chatHistoryMapper;

    public ChatServiceImplModern(
            @Qualifier(value = "openAiChatModel") ChatModel chatModel,
            AiHistoryRepository repository,
            ChatHistoryForConversationMapper chatHistoryForConversationMapper,
            ChatHistoryMapper chatHistoryMapper,
            // Inyectar todos los ToolCallback beans definidos en AiConfiguration
            ToolCallback filterByAgeFunctionCallback,
            ToolCallback filterByCityFunctionCallback,
            ToolCallback filterByCategoryProblemFunctionCallback,
            ToolCallback filterByUrgencyLevelFunctionCallback,
            ToolCallback filterByGovernmentAttentionFunctionCallback,
            ToolCallback filterByReportDateFunctionCallback,
            ToolCallback filterByZoneFunctionCallback,
            ToolCallback semanticSearchFunctionCallback,
            ToolCallback generateReportFunctionCallback) {

        this.repository = repository;
        this.chatHistoryForConversationMapper = chatHistoryForConversationMapper;
        this.chatHistoryMapper = chatHistoryMapper;

        // Crear ChatMemory
        ChatMemory memory =
                MessageWindowChatMemory.builder()
                        .chatMemoryRepository(new InMemoryChatMemoryRepository())
                        .maxMessages(100)
                        .build();

        // Construir ChatClient con todas las funciones registradas
        // En Spring AI 1.0.1, usamos defaultToolCallbacks() pasando instancias de ToolCallback
        // Los ToolCallback beans están definidos en AiConfiguration y se inyectan aquí
        this.chatClient =
                ChatClient.builder(chatModel)
                        .defaultAdvisors(
                                PromptChatMemoryAdvisor.builder(memory).build(),
                                MessageChatMemoryAdvisor.builder(memory).build())
                        // Registrar ToolCallback beans (Spring AI 1.0.1+)
                        .defaultToolCallbacks(
                                filterByAgeFunctionCallback,
                                filterByCityFunctionCallback,
                                filterByCategoryProblemFunctionCallback,
                                filterByUrgencyLevelFunctionCallback,
                                filterByGovernmentAttentionFunctionCallback,
                                filterByReportDateFunctionCallback,
                                filterByZoneFunctionCallback,
                                semanticSearchFunctionCallback,
                                generateReportFunctionCallback)
                        .defaultSystem(buildSystemPrompt())
                        .build();
    }

    @Override
    @Cacheable(value = "chats", key = "#dto.prompt")
    public DynamicAnalysisResponseDTO queryAi(ChatDTO dto, HttpServletRequest request) {
        try {
            log.info(
                    "Processing chat request. ConversationId: {}, Prompt: {}",
                    dto.getConversationId(),
                    dto.getPrompt());

            // Con native function calling, simplemente llamamos al modelo
            // Spring AI automáticamente detectará y ejecutará las funciones necesarias
            String response =
                    chatClient
                            .prompt()
                            .user(dto.getPrompt())
                            .advisors(a -> a.param(CONVERSATION_ID, dto.getConversationId()))
                            .call()
                            .content();

            log.debug("Received response from GPT-5: {}", response);

            // Guardar historial
            if (Objects.nonNull(dto.getConversationId())) {
                repository.save(
                        new ChatHistory(dto.getConversationId(), dto.getPrompt(), response));
                log.debug("Chat history saved for conversation: {}", dto.getConversationId());
            }

            // Crear respuesta
            StringChatResponseDTO stringResponse =
                    new StringChatResponseDTO(dto.getConversationId(), response, null);

            BaseDynamicResponseDTO dynamicResponse =
                    new SimpleTextResponseDTO(
                            "Respuesta del asistente",
                            "Análisis de reportes ciudadanos",
                            response);

            return new DynamicAnalysisResponseDTO(stringResponse, dynamicResponse);

        } catch (Exception e) {
            log.error(
                    "Error processing chat request with model {}: {}",
                    Model.OPENAI,
                    e.getMessage(),
                    e);
            throw handleChatException(e, dto);
        }
    }

    @Override
    public List<ChatHistoryForConversationDTO> getHistoryByConversationId(String conversationId) {
        log.debug("Fetching history for conversation: {}", conversationId);
        return repository.findByConversationId(conversationId).stream()
                .map(chatHistoryForConversationMapper::toDTO)
                .toList();
    }

    @Override
    public List<ChatHistoryDTO> getAllConversationsOfAuthenticatedUser(HttpServletRequest request) {
        log.debug("Fetching all conversations");

        List<ChatHistory> allHistory = repository.findAll();

        // Agrupa por conversationId y obtiene el PRIMER mensaje de cada conversación
        Map<String, Optional<ChatHistory>> firstByConversation =
                allHistory.stream()
                        .collect(
                                Collectors.groupingBy(
                                        ChatHistory::getConversationId,
                                        Collectors.minBy(
                                                Comparator.comparing(ChatHistory::getDate))));

        return firstByConversation.values().stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(chatHistoryMapper::toDTO)
                .sorted(Comparator.comparing(ChatHistoryDTO::getDate).reversed())
                .toList();
    }

    @Override
    public void removeChatHistoryByConversationId(String conversationId) {
        log.info("Removing chat history for conversation: {}", conversationId);
        repository.removeChatHistoriesByConversationId(conversationId);
    }

    // ==================== Helper Methods ====================

    /**
     * Construye el system prompt que guía al modelo GPT-5.
     *
     * <p>Este prompt le indica al modelo: - Qué es ComuniData - Qué funciones tiene disponibles -
     * Cuándo debe usar cada función - Cómo debe responder
     */
    private String buildSystemPrompt() {
        return """
                Eres el asistente inteligente de ComuniData, un sistema para análisis de reportes ciudadanos.

                Tu misión es ayudar a analizar reportes sobre problemas comunitarios en 4 áreas:
                1. Salud (hospitales, medicamentos, atención médica)
                2. Educación (escuelas, profesores, infraestructura educativa)
                3. Medio Ambiente (contaminación, basuras, agua, aire)
                4. Seguridad (delincuencia, iluminación, vigilancia)

                FUNCIONES DISPONIBLES:
                Tienes acceso a las siguientes funciones que puedes invocar automáticamente:

                1. filterByAge - Filtra reportes por rango de edad
                   Ejemplo: "Muéstrame reportes de personas entre 18 y 35 años"

                2. filterByCity - Filtra reportes por ciudad
                   Ejemplo: "Dame reportes de Manizales"

                3. filterByCategoryProblem - Filtra por categoría (Salud, Educación, Medio Ambiente, Seguridad)
                   Ejemplo: "Muéstrame todos los problemas de salud"

                4. filterByUrgencyLevel - Filtra por urgencia (Urgente, Alta, Media, Baja)
                   Ejemplo: "Dame los reportes urgentes"

                5. filterByGovernmentAttention - Filtra por atención del gobierno
                   Ejemplo: "Muéstrame problemas que no han sido atendidos"

                6. filterByReportDate - Filtra por fechas
                   Ejemplo: "Dame reportes del último mes"

                7. filterByZone - Filtra por zona (Rural/Urbana)
                   Ejemplo: "Muéstrame reportes de zonas rurales"

                8. semanticSearch - Búsqueda semántica con IA
                   Ejemplo: "Busca reportes similares a 'falta de medicamentos'"
                   IMPORTANTE: Usa esta función cuando el usuario busque por concepto, no por filtros exactos

                9. generateReport - Genera reportes PDF
                   Ejemplo: "Genera un reporte PDF de análisis por categoría"

                INSTRUCCIONES:
                - Usa las funciones cuando sea apropiado basándote en la consulta del usuario
                - Puedes combinar múltiples funciones si es necesario
                - Para búsquedas conceptuales o semánticas, USA semanticSearchFunction
                - Para filtros específicos (ciudad, edad, categoría), usa las funciones de filtro
                - Responde de forma clara, profesional y orientada a la acción
                - Incluye métricas y números cuando estén disponibles
                - Sugiere insights y recomendaciones basadas en los datos

                PRIVACIDAD:
                - Los datos están anonimizados (Ley 1581/2012)
                - No menciones información personal identificable
                - Los reportes han sido validados para detectar sesgos

                Responde siempre en español, de forma concisa pero informativa.
                """;
    }

    /**
     * Maneja excepciones de manera inteligente.
     */
    private ComuniDataException handleChatException(Exception e, ChatDTO dto) {
        Throwable rootCause = e;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }

        // Timeout
        if (rootCause instanceof io.netty.handler.timeout.ReadTimeoutException) {
            log.warn("Timeout calling {} model for prompt: {}", Model.OPENAI.name(), dto.getPrompt());
            return new ComuniDataException(
                    ApiError.AI_PROVIDER_TIMEOUT.getHttpStatus(),
                    ApiError.AI_PROVIDER_TIMEOUT.getMessage(),
                    List.of(
                            "El modelo " + Model.OPENAI.name() + " no respondió a tiempo",
                            "Esto puede ocurrir con consultas complejas",
                            "Intenta de nuevo o simplifica tu consulta"));
        }

        // Errores de conectividad
        if (e instanceof org.springframework.web.client.ResourceAccessException
                || rootCause instanceof java.net.ConnectException
                || rootCause instanceof java.net.UnknownHostException) {
            log.warn("Network error calling {} model: {}", Model.OPENAI.name(), rootCause.getMessage());
            return new ComuniDataException(
                    ApiError.AI_PROVIDER_UNAVAILABLE.getHttpStatus(),
                    ApiError.AI_PROVIDER_UNAVAILABLE.getMessage(),
                    List.of(
                            "No se puede conectar con " + Model.OPENAI.name(),
                            "Verifica tu conexión a internet",
                            "El servicio puede estar temporalmente no disponible"));
        }

        // Validación de prompt vacío
        if (dto.getPrompt() == null || dto.getPrompt().trim().isEmpty()) {
            log.warn("Empty or null prompt provided");
            return new ComuniDataException(
                    ApiError.BAD_FORMAT.getHttpStatus(),
                    ApiError.BAD_FORMAT.getMessage(),
                    List.of("El mensaje no puede estar vacío", "Por favor proporciona una consulta válida"));
        }

        // Error genérico
        log.error("Unexpected error processing chat request with {} model", Model.OPENAI.name(), e);
        return new ComuniDataException(
                ApiError.INTERNAL_ERROR.getHttpStatus(),
                ApiError.INTERNAL_ERROR.getMessage(),
                List.of(
                        "Ocurrió un error inesperado procesando tu solicitud",
                        "Tipo de error: " + e.getClass().getSimpleName(),
                        "Intenta de nuevo o contacta soporte si el problema persiste"));
    }
}
