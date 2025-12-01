package com.chatbot.controller;

import com.chatbot.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para el ChatBot
 * API para enviar mensajes y gestionar conversaciones
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    @Autowired
    private ChatService chatService;

    /**
     * Envía un mensaje y obtiene respuesta del ChatBot
     */
    @PostMapping("/message")
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody Map<String, String> request) {
        try {
            String userMessage = request.get("message");
            
            if (userMessage == null || userMessage.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "El mensaje no puede estar vacío");
                return ResponseEntity.badRequest().body(error);
            }

            String response = chatService.sendMessage(userMessage);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("response", response);
            result.put("messageCount", chatService.getMessageCount());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Obtiene el historial de conversación
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getHistory() {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("history", chatService.getConversationHistory());
            result.put("messageCount", chatService.getMessageCount());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Limpia el historial de conversación
     */
    @PostMapping("/clear")
    public ResponseEntity<Map<String, String>> clearHistory() {
        try {
            chatService.clearHistory();
            
            Map<String, String> result = new HashMap<>();
            result.put("success", "true");
            result.put("message", "Historial limpiado");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("success", "false");
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Obtiene estadísticas de la conversación
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("stats", chatService.getConversationStats());
            result.put("messageCount", chatService.getMessageCount());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
