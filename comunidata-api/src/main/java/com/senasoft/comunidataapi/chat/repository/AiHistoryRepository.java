package com.senasoft.comunidataapi.chat.repository;

import com.senasoft.comunidataapi.chat.entity.ChatHistory;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiHistoryRepository extends MongoRepository<ChatHistory, String> {
    List<ChatHistory> findByConversationId(String conversationId);

    void removeChatHistoriesByConversationId(String conversationId);
}
