package com.gkumar.ai.chatbot.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkumar.ai.chatbot.ChatRequest;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.retry.support.RetryTemplate;

public class ChatbotLambdaHandlerSimple implements RequestHandler<ChatRequest, String> {

    private final OpenAiChatModel chatModel;
    private final ObjectMapper objectMapper = new ObjectMapper();


    public ChatbotLambdaHandlerSimple() {
        String apiKey = System.getenv("OPENAI_API_KEY"); // Load API key from environment

        OpenAiApi openAiApi = OpenAiApi.builder().apiKey(apiKey).build();
        OpenAiChatOptions defaultOptions = OpenAiChatOptions.builder()
                .model("gpt-4-turbo")  // or "gpt-3.5-turbo"
                .build();
        this.chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(defaultOptions)
                .retryTemplate(RetryTemplate.defaultInstance())
                .observationRegistry(ObservationRegistry.create())
                .build();
    }

    @Override
    public String handleRequest(ChatRequest request, Context context) {
        System.out.println("Received Request: " + request);
        System.out.println("Received message: " + request.getMessage());
        if (request == null || request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return "{\"error\": \"Content must not be null for SYSTEM or USER messages\"}";
        }
        return chatModel.call(request.getMessage());
    }
}
