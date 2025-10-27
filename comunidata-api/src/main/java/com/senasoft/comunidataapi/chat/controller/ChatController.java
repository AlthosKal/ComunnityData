package com.senasoft.comunidataapi.chat.controller;

import com.senasoft.comunidataapi.chat.dto.request.ChatDTO;
import com.senasoft.comunidataapi.chat.dto.response.DynamicAnalysisResponseDTO;
import com.senasoft.comunidataapi.chat.service.chat.ChatService;
import com.senasoft.comunidataapi.chat.service.function.list.ConversationIdService;
import com.senasoft.comunidataapi.chat.service.report.ReportGenerationService;
import com.senasoft.comunidataapi.exception.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@CrossOrigin
public class ChatController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;
    private final ConversationIdService conversationIdService;
    private final ReportGenerationService reportGenerationService;

    @PostMapping(value = "/chat")
    public ResponseEntity<?> askAi(@RequestBody @Valid ChatDTO dto, HttpServletRequest request) {
        if (dto.needsConversationId()) {
            dto.setConversationId(conversationIdService.generateConversationId());
        }

        LOGGER.info("Processing the prompt with {}", dto);
        DynamicAnalysisResponseDTO response = chatService.queryAi(dto, request);

        return new ResponseEntity<>(
                ApiResponse.ok(
                        "Respuesta generada correctamente", response, request.getRequestURI()),
                HttpStatus.OK);
    }

    @GetMapping("/reports/download/{reportId}")
    public ResponseEntity<?> downloadReport(@PathVariable String reportId) {
        return reportGenerationService.downloadReport(reportId);
    }
}
