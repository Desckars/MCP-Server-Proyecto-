package com.example.mcp_server.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.Map;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class LLMService {
    private final String ollamaApiUrl;
    private final String ollamaModel;
    private final RestTemplate restTemplate = new RestTemplate();

    //GEMEINI
     private final String geminiApiKey;
    private final String geminiModel;
    private final String geminiApiUrl;

    public LLMService(@Value("${ollama.api.url}") String ollamaApiUrl,
                      @Value("${ollama.model}") String ollamaModel,
                      @Value("${google.gemini.api-key}") String geminiApiKey,                    
                      @Value("${google.gemini.model}") String geminiModel) {
        this.ollamaApiUrl = ollamaApiUrl;
        this.ollamaModel = ollamaModel;
        this.geminiApiKey = geminiApiKey;
        this.geminiModel = geminiModel;
        this.geminiApiUrl = String.format("https://generativelanguage.googleapis.com/v1/models/%s:generateContent?key=%s", geminiModel, geminiApiKey);
    }

    public String processQuery(String query) {
        // 1. Enviar prompt a Ollama
        return callOllamaApi(query);
    }

    public String askGemini(String question) {
        return callGeminiApi(question);
    }

    private String callOllamaApi(String query) {
        // Request JSON para Ollama
        Map<String, Object> request = Map.of(
                "model", ollamaModel,
                "prompt", query,
                "stream", false
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(ollamaApiUrl, entity, Map.class);

        if (response.getBody() != null && response.getBody().get("response") != null) {
            return response.getBody().get("response").toString().trim();
        }

        return "(sin respuesta de Ollama)";
    }
    private String callGeminiApi(String question) {
        try {
            // Request JSON for Google Gemini
            String requestBody = String.format("{\"contents\":[{\"role\":\"user\",\"parts\":[{\"text\":\"%s\"}]}]}", question);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(geminiApiUrl, HttpMethod.POST, entity, Map.class);
            
            if (response.getBody() != null) {
                // Parse the nested JSON response from Gemini
                ObjectMapper mapper = new ObjectMapper();
                String responseJson = mapper.writeValueAsString(response.getBody());
                
                // You'll need to parse this JSON to get the actual text response
                // A simpler, but less robust, way is to use a Map and navigate the structure
                Map<String, Object> parsedResponse = response.getBody();
                if (parsedResponse.containsKey("candidates")) {
                    java.util.List<Map> candidates = (java.util.List<Map>) parsedResponse.get("candidates");
                    if (!candidates.isEmpty()) {
                        Map<String, Object> candidate = candidates.get(0);
                        Map<String, Object> content = (Map<String, Object>) candidate.get("content");
                        java.util.List<Map> parts = (java.util.List<Map>) content.get("parts");
                        if (!parts.isEmpty()) {
                            return parts.get(0).get("text").toString();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "An error occurred while contacting the Google Gemini API.";
        }
        return "(no response from Gemini)";
    }
}