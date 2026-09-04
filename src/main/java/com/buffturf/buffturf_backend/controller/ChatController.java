package com.buffturf.buffturf_backend.controller;

import com.buffturf.buffturf_backend.dto.ChatRequest;
import com.buffturf.buffturf_backend.dto.ChatResponse;
import com.buffturf.buffturf_backend.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @RequestBody ChatRequest chatRequest,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        String userEmail = userDetails != null ? userDetails.getUsername() : null;
        ChatResponse response = chatService.handleChat(chatRequest, userEmail);
        return ResponseEntity.ok(response);
    }
}
