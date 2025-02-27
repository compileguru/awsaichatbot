package com.gkumar.ai.chatbot.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkumar.ai.chatbot.ChatRequest;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.retry.support.RetryTemplate;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

public class ChatbotLambdaHandlerApiGateway implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final OpenAiChatModel chatModel;
    private final ObjectMapper objectMapper = new ObjectMapper();


    public ChatbotLambdaHandlerApiGateway() {
        String apiKey = fetchApiKeyFromSSM();
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

    private String fetchApiKeyFromSSM() {
        String region = System.getenv("AWS_REGION"); // Auto-set by AWS Lambda
        if (region == null || region.isEmpty()) {
            throw new IllegalArgumentException("AWS_REGION environment variable is not set.");
        }

        try (SsmClient ssmClient = SsmClient.builder().region(Region.of(region)).build()) {
            GetParameterRequest request = GetParameterRequest.builder()
                    .name("/chatbot/openai_api_key") // Ensure this parameter exists in AWS SSM
                    .withDecryption(false)  // If stored as a SecureString
                    .build();

            GetParameterResponse result = ssmClient.getParameter(request);
            return result.parameter().value();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch OpenAI API key from SSM: " + e.getMessage(), e);
        }
    }


    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        try {
            System.out.println("Received Event: " + event);

            // Extract body from request
            String body = event.getBody();
            if (body == null || body.isEmpty()) {
                return response
                        .withStatusCode(400)
                        .withBody("{\"error\": \"Request body is missing\"}");
            }
            ChatRequest chatRequest = objectMapper.readValue(body, ChatRequest.class);
            if (chatRequest.getMessage() == null || chatRequest.getMessage().trim().isEmpty()) {
                return response
                        .withStatusCode(400)
                        .withBody("{\"error\": \"Message field is missing\"}");
            }
            String reply = chatModel.call(chatRequest.getMessage());
            return response
                    .withStatusCode(200)
                    .withBody("{\"response\": \"" + reply + "\"}");

        } catch (Exception e) {
            e.printStackTrace();
            return response
                    .withStatusCode(500)
                    .withBody("{\"error\": \"Internal Server Error\"}");
        }
    }
}
