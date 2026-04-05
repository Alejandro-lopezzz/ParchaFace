package com.alejo.parchaface.dto;

import java.util.List;

public record AssistantResponse(
        String reply,
        List<AssistantActionDto> actions,
        List<AssistantOptionDto> options
) {}