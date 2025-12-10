package com.chatbot.service;

import com.chatbot.model.Message;
import java.util.ArrayList;
import java.util.List;

public class ChatService {
    private AIService aiService;
    private List<Message> conversationHistory;
    private ConversationLogger logger;
    //Constructor
    public ChatService() {
        this.aiService = new AIService();
        this.conversationHistory = new ArrayList<>();
        this.logger = ConversationLogger.getInstance();
    }
    
    // Envía un mensaje y obtiene respuesta
    // NOTA: El contexto ahora lo maneja ClaudeService internamente
    public String sendMessage(String userMessage) {
        // Guardar mensaje del usuario en historial local
        Message userMsg = new Message("USER", userMessage);
        conversationHistory.add(userMsg);
        // Registrar en el log
        try { logger.logUser(userMessage); } catch (Exception ignored) {}
        
        // Enviar a AIService (que usa ClaudeService con contexto)
        String aiResponse = aiService.generateResponse(userMessage);
        
        // Guardar respuesta en historial local
        Message aiMsg = new Message("CLAUDE", aiResponse);
        conversationHistory.add(aiMsg);
        try { logger.logAssistant("CLAUDE", aiResponse); } catch (Exception ignored) {}
        
        return aiResponse;
    }
    
    // Retorna el historial completo de conversación 
    public List<Message> getConversationHistory() {
        return new ArrayList<>(conversationHistory);
    }
    
    // Limpia el historial local Y el contexto de Claude
    public void clearHistory() {
        conversationHistory.clear();
        // También limpiar el contexto interno de Claude
        aiService.clearContext();
        System.out.println(" Historial y contexto limpiados");
    }
    
    // Obtiene el número total de mensajes
    public int getMessageCount() {
        return conversationHistory.size();
    }
    
    // Obtiene estadísticas de la conversación (Esto sirve solamente para el modo consola y para que el usuario vea datos de la conversacion)
    // Se podria quitar
    public String getConversationStats() {
        int userMessages = 0;
        int claudeMessages = 0;
        
        for (Message msg : conversationHistory) {
            if ("USER".equals(msg.getSender())) {
                userMessages++;
            } else {
                claudeMessages++;
            }
        }
        
        return String.format("""
            Estadísticas de Conversación:
            • Total de mensajes: %d
            • Mensajes del usuario: %d
            • Respuestas de Claude: %d
            • Contexto de Claude: %d mensajes
            """, 
            conversationHistory.size(),
            userMessages,
            claudeMessages,
            aiService.getContextSize()
        );
    }
    
    // Verifica si hay conversación activa
    public boolean hasActiveConversation() {
        return !conversationHistory.isEmpty();
    }
    
    public AIService getAIService() {
        return aiService;
    }
    
    public void shutdown() {
        aiService.shutdown();
    }
}