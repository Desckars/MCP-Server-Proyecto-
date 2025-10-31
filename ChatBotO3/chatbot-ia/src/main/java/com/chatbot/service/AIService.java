package com.chatbot.service;

import com.chatbot.config.ClaudeConfig;
import com.chatbot.config.MCPConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class AIService {
    private ClaudeService claudeService;
    private MCPService mcpService;
    private boolean useAI;
    private boolean mcpEnabled;
    private Random random;
    private Map<String, String> responses;
    
    public AIService() {
        this.claudeService = new ClaudeService();
        this.mcpService = new MCPService();
        this.random = new Random();
        this.responses = new HashMap<>();
        initializeResponses();
        this.useAI = checkAIAvailability();
        this.mcpEnabled = false;
        
        claudeService.setMCPService(mcpService);
        
        if (useAI) {
            System.out.println("✓ Claude AI activado");
            System.out.println("✓ Contexto conversacional activado");
            System.out.println("✓ Multi-query con reintentos automáticos");
            System.out.println("✓ Tool Calling habilitado");
        } else {
            System.out.println("⚠ Modo simulado (verifica tu API key de Claude)");
        }
    }
    
    private boolean checkAIAvailability() {
        return ClaudeConfig.getInstance().isConfigured();
    }
    
    private void initializeResponses() {
        responses.put("hola", "¡Hola! Soy Claude. ¿En qué puedo ayudarte hoy?");
        responses.put("adios", "¡Hasta luego! Fue un placer ayudarte.");
    }
    
    private void ensureMCPStarted() {
        if (!mcpEnabled && mcpService.start()) {
            mcpEnabled = true;
        }
    }
    
    /**
     * Genera respuesta usando Claude con Tool Calling y contexto
     */
    public String generateResponse(String userMessage) {
        if (!useAI) {
            return generateSimulatedResponse(userMessage);
        }
        
        try {
            ensureMCPStarted();
            
            // Claude maneja TODO: contexto, tool calling, reintentos
            String response = claudeService.generateResponseWithTools(userMessage);
            
            if (!response.startsWith("ERROR")) {
                return response;
            }
            
            System.err.println("Error de Claude: " + response);
            return response;
            
        } catch (Exception e) {
            System.err.println("Excepción: " + e.getMessage());
            e.printStackTrace();
            return "Error procesando tu mensaje: " + e.getMessage();
        }
    }
    
    private String generateSimulatedResponse(String userMessage) {
        String normalized = userMessage.toLowerCase().trim();
        
        for (Map.Entry<String, String> entry : responses.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        String[] genericResponses = {
            "Interesante. Cuéntame más.",
            "Entiendo. ¿Qué más te gustaría saber?",
            "He recibido tu mensaje. ¿En qué más puedo ayudarte?"
        };
        
        return genericResponses[random.nextInt(genericResponses.length)];
    }
    
    /**
     * NUEVO: Limpia el contexto de Claude
     */
    public void clearContext() {
        if (claudeService != null) {
            claudeService.clearContext();
        }
    }
    
    /**
     * NUEVO: Obtiene el tamaño del contexto actual
     */
    public int getContextSize() {
        return claudeService != null ? claudeService.getContextSize() : 0;
    }
    
    public boolean isUsingAI() {
        return useAI;
    }
    
    public boolean isMCPEnabled() {
        return mcpEnabled;
    }
    
    public String listMCPTools() {
        ensureMCPStarted();
        if (!mcpEnabled) {
            return "MCP no está disponible";
        }
        return mcpService.listTools();
    }
    
    public String executeDirectMDX(String mdxQuery) {
        ensureMCPStarted();
        if (!mcpEnabled) {
            return "MCP no está disponible";
        }
        return mcpService.executeQuery(mdxQuery);
    }
    
    public void shutdown() {
        if (mcpEnabled) {
            mcpService.stop();
        }
    }
}