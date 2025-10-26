package com.senasoft.comunidataapi.chat.mapper;

import com.senasoft.comunidataapi.chat.dto.request.ChatHistoryDTO;
import com.senasoft.comunidataapi.chat.entity.ChatHistory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChatHistoryMapper {

    ChatHistoryDTO toDTO(ChatHistory chatHistory);
}
