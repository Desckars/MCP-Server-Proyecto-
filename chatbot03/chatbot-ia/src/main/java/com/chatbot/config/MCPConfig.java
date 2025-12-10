package com.chatbot.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MCPConfig {
    private static MCPConfig instance;
    private boolean enabled;
    private String jarPath;
    private String workingDirectory;
    
    private MCPConfig() {
        loadConfiguration();
    }
    
    public static MCPConfig getInstance() {
        if (instance == null) {
            instance = new MCPConfig();
        }
        return instance;
    }
    //Cargar configuración desde el archivo properties
    private void loadConfiguration() {
        Properties props = new Properties();
        
        try {
            InputStream input = getClass().getClassLoader()
                .getResourceAsStream("config.properties");
            
            if (input == null) {
                System.err.println("[ERROR] config.properties no encontrado para MCP");
                setDefaults();
                return;
            }
            
            props.load(input);
            this.enabled = Boolean.parseBoolean(
                props.getProperty("mcp.o3.enabled", "true")
            );
            this.jarPath = props.getProperty("mcp.o3.jar-path");
            this.workingDirectory = props.getProperty("mcp.o3.working-directory");
            
            input.close();
            
            System.out.println("Configuración MCP O3 cargada");
            System.out.println("  Habilitado: " + this.enabled);
            System.out.println("  JAR Path: " + this.jarPath);
            System.out.println("  Working Dir: " + this.workingDirectory);
            
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            setDefaults();
        }
    }
    
    private void setDefaults() {
        this.enabled = true;
        this.jarPath = "mcp/mcp_o3-0.0.4-SNAPSHOT.jar";
        this.workingDirectory = ".";
    }
    
    //Getters
    public boolean isEnabled() { return enabled; }
    public String getJarPath() { return jarPath; }
    public String getWorkingDirectory() { return workingDirectory; }
}
