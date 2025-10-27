package com.senasoft.comunidataapi.chat.controller;

import com.senasoft.comunidataapi.chat.dto.request.ChatHistoryDTO;
import com.senasoft.comunidataapi.chat.dto.request.ChatHistoryForConversationDTO;
import com.senasoft.comunidataapi.chat.service.chat.ChatService;
import com.senasoft.comunidataapi.exception.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/chat/history")
@RequiredArgsConstructor
@CrossOrigin
public class ChatHistoryController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatHistoryController.class);
    private final ChatService chatService;

    @GetMapping("/all")
    public ResponseEntity<?> getAllConversations(HttpServletRequest request) {
        List<ChatHistoryDTO> summaries =
                chatService.getAllConversationsOfAuthenticatedUser(request);
        return new ResponseEntity<>(
                ApiResponse.ok(
                        "Historial obtenido correctamente", summaries, request.getRequestURI()),
                HttpStatus.OK);
    }

    @GetMapping("/{conversationId}")
    public ResponseEntity<?> getHistory(
            @PathVariable String conversationId, HttpServletRequest request) {
        LOGGER.info("Fetching history for conversationId: {}", conversationId);
        List<ChatHistoryForConversationDTO> history =
                chatService.getHistoryByConversationId(conversationId);
        return new ResponseEntity<>(
                ApiResponse.ok(
                        "Historial obtenido correctamente", history, request.getRequestURI()),
                HttpStatus.OK);
    }

    @DeleteMapping("/delete/{conversationId}")
    public ResponseEntity<?> deleteHistory(@PathVariable String conversationId) {
        chatService.removeChatHistoryByConversationId(conversationId);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
