package com.senasoft.comunidataapi.chat.repository;

import com.senasoft.comunidataapi.chat.entity.ChatHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiHistoryRepository extends MongoRepository<ChatHistory, String> {
    List<ChatHistory> findByConversationId(String conversationId);

    void removeChatHistoriesByConversationId(String conversationId);
}
