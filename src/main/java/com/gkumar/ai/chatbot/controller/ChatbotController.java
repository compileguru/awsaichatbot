package com.gkumar.ai.chatbot.controller;

import com.gkumar.ai.chatbot.ChatRequest;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
public class ChatbotController {

    private final OpenAiChatModel chatModel;

    @Autowired
    public ChatbotController(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @PostMapping
    public String chat(@RequestBody ChatRequest request) {
        return chatModel.call(request.getMessage());
    }
}

