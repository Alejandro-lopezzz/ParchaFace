package com.alejo.parchaface.service;

import com.alejo.parchaface.dto.AssistantRequest;
import com.alejo.parchaface.dto.AssistantResponse;

import java.security.Principal;

public interface AiAssistantService {
    AssistantResponse chat(AssistantRequest request, Principal principal);
}