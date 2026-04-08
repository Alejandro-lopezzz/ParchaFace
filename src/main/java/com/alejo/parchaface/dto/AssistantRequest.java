package com.alejo.parchaface.dto;

import java.util.List;
import java.util.Map;

public record AssistantRequest(
        String message,
        String sessionId,
        String conversationId,
        String currentRoute,
        List<AssistantChatMessageDto> history,
        Map<String, Object> pageContext
) {}