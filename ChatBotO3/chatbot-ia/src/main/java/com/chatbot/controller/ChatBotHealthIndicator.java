package com.chatbot.controller;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import com.chatbot.config.ClaudeConfig;

/**
 * Indicador de salud personalizado para verificar la configuración de Claude
 */
@Component
public class ChatBotHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        try {
            ClaudeConfig config = ClaudeConfig.getInstance();
            
            if (config.isConfigured()) {
                return Health.up()
                    .withDetail("claude_configured", true)
                    .withDetail("model", config.getModel())
                    .withDetail("max_tokens", config.getMaxTokens())
                    .build();
            } else {
                return Health.outOfService()
                    .withDetail("claude_configured", false)
                    .withDetail("message", "API Key no configurado")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
