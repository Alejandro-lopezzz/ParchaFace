package com.alejo.parchaface.controller;

import com.alejo.parchaface.dto.AssistantRequest;
import com.alejo.parchaface.dto.AssistantResponse;
import com.alejo.parchaface.service.AiAssistantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/assistant")
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    public AiAssistantController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    @PostMapping("/chat")
    public ResponseEntity<AssistantResponse> chat(
            @RequestBody AssistantRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(aiAssistantService.chat(request, principal));
    }
}