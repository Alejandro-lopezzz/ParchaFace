package com.alejo.parchaface.dto;

import java.util.Map;

public record AssistantActionDto(
        String type,
        String route,
        String targetId,
        String url,
        Map<String, Object> query
) {}