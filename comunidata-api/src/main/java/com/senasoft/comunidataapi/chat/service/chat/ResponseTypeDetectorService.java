package com.senasoft.comunidataapi.chat.service.chat;

import com.senasoft.comunidataapi.chat.dto.response.ai.BaseDynamicResponseDTO;

public interface ResponseTypeDetectorService {
    BaseDynamicResponseDTO detectAndCreateResponse(String prompt, String functionName, Object data);
}
