package com.senasoft.comunidataapi.chat.service.chat;

import com.senasoft.comunidataapi.chat.dto.request.*;
import com.senasoft.comunidataapi.chat.dto.response.DynamicAnalysisResponseDTO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface ChatService {
    DynamicAnalysisResponseDTO queryAi(ChatDTO dto, HttpServletRequest request);

    List<ChatHistoryForConversationDTO> getHistoryByConversationId(String conversationId);

    List<ChatHistoryDTO> getAllConversationsOfAuthenticatedUser(HttpServletRequest request);

    void removeChatHistoryByConversationId(String conversationId);
}
