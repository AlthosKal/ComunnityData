package com.senasoft.comunidataapi.chat.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.senasoft.comunidataapi.chat.dto.request.ChatHistoryForConversationDTO;
import com.senasoft.comunidataapi.chat.entity.ChatHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class ChatHistoryForConversationMapper {

    @Autowired private ObjectMapper objectMapper;

    @Mapping(target = "response", source = "response", qualifiedByName = "objectToString")
    public abstract ChatHistoryForConversationDTO toDTO(ChatHistory chatHistory);

    @Named("objectToString")
    protected String objectToString(Object response) {
        if (response == null) {
            return null;
        }

        if (response instanceof String) {
            return (String) response;
        }

        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            return response.toString();
        }
    }
}
