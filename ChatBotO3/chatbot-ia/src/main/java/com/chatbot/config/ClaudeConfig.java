package com.chatbot.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ClaudeConfig {
    private static ClaudeConfig instance;
    private String apiKey;
    private String model;
    private int maxTokens;
    
    private ClaudeConfig() {
        loadConfiguration();
    }
    
    public static ClaudeConfig getInstance() {
        if (instance == null) {
            instance = new ClaudeConfig();
        }
        return instance;
    }
    
    private void loadConfiguration() {
        Properties props = new Properties();
        
        System.out.println("[Claude Config] Cargando configuración...");
        
        try {
            InputStream input = getClass().getClassLoader()
                .getResourceAsStream("config.properties");
            
            if (input == null) {
                System.err.println("[ERROR] config.properties no encontrado");
                setDefaults();
                return;
            }
            
            props.load(input);
            this.apiKey = props.getProperty("anthropic.api-key");
            this.model = props.getProperty("anthropic.model", "claude-sonnet-4-20250514");
            this.maxTokens = Integer.parseInt(
                props.getProperty("anthropic.max-tokens", "4096")
            );
            
            input.close();
            
            System.out.println("✓ Configuración Claude cargada");
            System.out.println("  API Key: " + (this.apiKey != null && !this.apiKey.equals("TU_API_KEY_AQUI") 
                ? this.apiKey.substring(0, 20) + "..." 
                : "NO CONFIGURADA"));
            System.out.println("  Modelo: " + this.model);
            System.out.println("  Max Tokens: " + this.maxTokens);
            
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            setDefaults();
        }
    }
    
    private void setDefaults() {
        this.apiKey = null;
        this.model = "claude-sonnet-4-20250514";
        this.maxTokens = 4096;
    }
    
    public String getApiKey() { return apiKey; }
    public String getModel() { return model; }
    public int getMaxTokens() { return maxTokens; }
    
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.equals("TU_API_KEY_AQUI");
    }
}
