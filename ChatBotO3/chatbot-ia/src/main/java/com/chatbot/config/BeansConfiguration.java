package com.chatbot.config;

import com.chatbot.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Beans de Spring
 * Define los servicios y dependencias como beans administrados por Spring
 */
@Configuration
public class BeansConfiguration {

    /**
     * Bean para ClaudeConfig (Singleton)
     */
    @Bean
    public ClaudeConfig claudeConfig() {
        return ClaudeConfig.getInstance();
    }

    /**
     * Bean para MCPConfig
     */
    @Bean
    public MCPConfig mcpConfig(MCPProperties mcpProperties) {
        // MCPConfig usa singleton y carga su propia configuración desde config.properties
        return MCPConfig.getInstance();
    }

    /**
     * Bean para MCPService
     */
    @Bean
    public MCPService mcpService(MCPConfig mcpConfig) {
        return new MCPService();
    }

    /**
     * Bean para ClaudeService
     */
    @Bean
    public ClaudeService claudeService(MCPService mcpService) {
        ClaudeService service = new ClaudeService();
        service.setMCPService(mcpService);
        return service;
    }

    /**
     * Bean para AIService
     */
    @Bean
    public AIService aiService(ClaudeService claudeService, MCPService mcpService) {
        AIService service = new AIService();
        // Los servicios se inyectarán automáticamente en el constructor
        return service;
    }

    /**
     * Bean para ChatService
     */
    @Bean
    public ChatService chatService(AIService aiService) {
        return new ChatService();
    }

    /**
     * Bean para ConversationLogger
     */
    @Bean
    public ConversationLogger conversationLogger() {
        try {
            return ConversationLogger.getInstance();
        } catch (Exception e) {
            System.err.println("Error inicializando ConversationLogger: " + e.getMessage());
            return null;
        }
    }
}
