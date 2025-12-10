package com.chatbot.service;

import com.chatbot.config.ClaudeConfig;


public class AIService {
    private ClaudeService claudeService;
    private MCPService mcpService;
    private boolean useAI;
    private boolean mcpEnabled;

    //Constructor
    public AIService() {
        this.claudeService = new ClaudeService();
        this.mcpService = new MCPService();
        this.useAI = checkAIAvailability();
        this.mcpEnabled = false;
        
        claudeService.setMCPService(mcpService);
        
        if (useAI) {
            System.out.println(" Claude AI activado");
            System.out.println(" Contexto conversacional activado");
            System.out.println(" Multi-query con reintentos automáticos");
            System.out.println(" Tool Calling habilitado");
        } else {
            System.out.println(" Modo simulado (verifica tu API key de Claude)");
        }
    }
    // Chequeo de disponibilidad de AI
    private boolean checkAIAvailability() {
        return ClaudeConfig.getInstance().isConfigured();
    }
    //Chequea si MCP está iniciado
    private void ensureMCPStarted() {
        if (!mcpEnabled && mcpService.start()) {
            mcpEnabled = true;
        }
    }    
    //Genera respuesta usando Claude con Tool Calling y contexto
    public String generateResponse(String userMessage) {
        if (!useAI) {
            return  "ERROR: AI no disponible. Verifica tu configuración de Claude.";
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
    // Limpia el contexto de Claude
    public void clearContext() {
        if (claudeService != null) {
            claudeService.clearContext();
        }
    }
    
    // Obtiene el tamaño del contexto actual    
    public int getContextSize() {
        return claudeService != null ? claudeService.getContextSize() : 0;
    }
    //Getters
    public boolean isUsingAI() {
        return useAI;
    }    
    public boolean isMCPEnabled() {
        return mcpEnabled;
    }
    //Lista todas las herramientas (tools) disponibles en el servidor MCP
    public String listMCPTools() {
        ensureMCPStarted();
        if (!mcpEnabled) {
            return "MCP no está disponible";
        }
        return mcpService.listTools();
    }
    // Ejecuta una consulta MDX directamente sin pasar por Claude
    public String executeDirectMDX(String mdxQuery) {
        ensureMCPStarted();
        if (!mcpEnabled) {
            return "MCP no está disponible";
        }
        return mcpService.executeQuery(mdxQuery);
    }
    // Apaga servicios al cerrar la aplicación
    public void shutdown() {
        if (mcpEnabled) {
            mcpService.stop();
        }
    }
}