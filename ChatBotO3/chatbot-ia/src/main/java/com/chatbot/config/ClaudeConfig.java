package com.chatbot.config;

import com.chatbot.security.EncryptionUtil;
import java.io.*;
import java.util.Properties;

public class ClaudeConfig {
    private static ClaudeConfig instance;
    private String apiKey;
    private String model;
    private int maxTokens;
    private boolean configured = false;
    
    // Clave maestra única por instalación
    private static final String MASTER_KEY = generateMasterKey();
    
    private ClaudeConfig() {
        loadConfiguration();
    }
    
    public static ClaudeConfig getInstance() {
        if (instance == null) {
            instance = new ClaudeConfig();
        }
        return instance;
    }
    
    /**
     * Forzar recarga de configuración (útil después de guardar cambios)
     */
    public static void reload() {
        instance = null;
        getInstance();
    }
    
    /**
     * Genera clave maestra única por instalación
     */
    private static String generateMasterKey() {
        String userName = System.getProperty("user.name", "default");
        String osName = System.getProperty("os.name", "unknown");
        String javaVersion = System.getProperty("java.version", "unknown");
        String salt = "ChatBot-IA-O3-MCP-2025";
        
        return salt + "-" + userName + "-" + osName.hashCode() + "-" + javaVersion.hashCode();
    }
    
    private void loadConfiguration() {
        System.out.println("[Claude Config] Cargando configuración...");
        
        File configFile = getConfigFile();
        Properties props = new Properties();
        
        try {
            // Intentar cargar desde archivo
            if (configFile.exists()) {
                try (FileInputStream fis = new FileInputStream(configFile)) {
                    props.load(fis);
                }
            } else {
                System.err.println("⚠️  Archivo config.properties no encontrado");
                System.err.println("   Ruta esperada: " + configFile.getAbsolutePath());
                setDefaults();
                return;
            }
            
            // PRIORIDAD 1: Variable de entorno (para CI/CD)
            this.apiKey = System.getenv("CLAUDE_API_KEY");
            
            // PRIORIDAD 2: API Key encriptado
            if (this.apiKey == null || this.apiKey.isEmpty()) {
                String encryptedKey = props.getProperty("anthropic.api-key.encrypted");
                
                if (encryptedKey != null && !encryptedKey.isEmpty() && 
                    EncryptionUtil.isEncrypted(encryptedKey)) {
                    
                    System.out.println("🔐 Desencriptando API Key...");
                    
                    try {
                        this.apiKey = EncryptionUtil.decrypt(encryptedKey, MASTER_KEY);
                        System.out.println("✅ API Key cargado correctamente");
                    } catch (Exception e) {
                        System.err.println("❌ Error desencriptando API Key: " + e.getMessage());
                        System.err.println("   La configuración podría estar corrupta");
                        this.apiKey = null;
                    }
                }
            }
            
            // PRIORIDAD 3: API Key en texto plano (primera vez)
            if (this.apiKey == null || this.apiKey.isEmpty()) {
                String plainKey = props.getProperty("anthropic.api-key");
                
                if (plainKey != null && !plainKey.isEmpty() && plainKey.startsWith("sk-ant-")) {
                    System.out.println("🔒 API Key en texto plano detectado");
                    System.out.println("   Encriptando automáticamente...");
                    
                    try {
                        // Encriptar
                        String encrypted = EncryptionUtil.encrypt(plainKey, MASTER_KEY);
                        
                        // Actualizar properties
                        props.setProperty("anthropic.api-key.encrypted", encrypted);
                        props.remove("anthropic.api-key");
                        
                        // Guardar
                        savePropertiesWithComments(configFile, props);
                        
                        this.apiKey = plainKey;
                        System.out.println("✅ API Key encriptado y guardado");
                        System.out.println("   config.properties actualizado");
                        
                    } catch (Exception e) {
                        System.err.println("⚠️  Error encriptando: " + e.getMessage());
                        this.apiKey = plainKey; // Usar sin encriptar esta vez
                    }
                }
            }
            
            // Cargar resto de configuración
            this.model = props.getProperty("anthropic.model", "claude-sonnet-4-20250514").trim();
            this.maxTokens = Integer.parseInt(
                props.getProperty("anthropic.max-tokens", "4096").trim()
            );
            
            this.configured = (this.apiKey != null && !this.apiKey.isEmpty());
            
            if (this.configured) {
                System.out.println("✓ Configuración Claude cargada");
                System.out.println("  API Key: " + maskApiKey(this.apiKey));
                System.out.println("  Modelo: " + this.model);
                System.out.println("  Max Tokens: " + this.maxTokens);
            } else {
                System.err.println("❌ API Key no configurado");
            }
            
        } catch (IOException e) {
            System.err.println("Error cargando configuración: " + e.getMessage());
            setDefaults();
        }
    }
    
    /**
     * Obtiene la ruta del archivo config.properties
     */
    private File getConfigFile() {
        // Intentar primero desde resources en el classpath
        try {
            String path = getClass().getClassLoader()
                .getResource("config.properties")
                .getPath();
            File file = new File(path);
            if (file.exists()) {
                return file;
            }
        } catch (Exception e) {
            // Ignorar, intentar ruta alternativa
        }
        
        // Ruta alternativa (desarrollo)
        return new File("src/main/resources/config.properties");
    }
    
    /**
     * Guarda properties manteniendo comentarios
     */
    private void savePropertiesWithComments(File file, Properties props) throws IOException {
        StringBuilder content = new StringBuilder();
        
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Actualizar líneas específicas
                    if (line.trim().startsWith("anthropic.api-key=") && 
                        !line.trim().startsWith("anthropic.api-key.encrypted=")) {
                        content.append("# ").append(line).append(" # AUTO-ENCRIPTADO\n");
                        continue;
                    } else if (line.trim().startsWith("anthropic.api-key.encrypted=")) {
                        content.append("anthropic.api-key.encrypted=")
                               .append(props.getProperty("anthropic.api-key.encrypted"))
                               .append("\n");
                        continue;
                    } else if (line.trim().startsWith("anthropic.model=")) {
                        content.append("anthropic.model=")
                               .append(props.getProperty("anthropic.model"))
                               .append("\n");
                        continue;
                    } else if (line.trim().startsWith("anthropic.max-tokens=")) {
                        content.append("anthropic.max-tokens=")
                               .append(props.getProperty("anthropic.max-tokens"))
                               .append("\n");
                        continue;
                    }
                    
                    content.append(line).append("\n");
                }
            }
        }
        
        // Escribir de vuelta
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(content.toString());
        }
    }
    
    /**
     * Enmascara el API Key para logging seguro
     */
    private String maskApiKey(String key) {
        if (key == null || key.length() < 12) {
            return "***";
        }
        return key.substring(0, 11) + "...";
    }
    
    private void setDefaults() {
        this.apiKey = "";
        this.model = "claude-sonnet-4-20250514";
        this.maxTokens = 4096;
        this.configured = false;
    }
    
    // Getters
    public String getApiKey() { return apiKey; }
    public String getModel() { return model; }
    public int getMaxTokens() { return maxTokens; }
    public boolean isConfigured() { return configured; }

    /**
     * Guarda y encripta la API Key en el archivo de configuración.
     * Este método actualiza la propiedad `anthropic.api-key.encrypted`.
     */
    public synchronized void saveApiKey(String plainKey) throws Exception {
        if (plainKey == null || plainKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API Key no puede estar vacía");
        }

        File configFile = getConfigFile();
        Properties props = new Properties();

        // Cargar existentes si existen
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
            }
        }

        // Encriptar usando clave maestra
        String encrypted = EncryptionUtil.encrypt(plainKey, MASTER_KEY);

        props.setProperty("anthropic.api-key.encrypted", encrypted);
        props.remove("anthropic.api-key");

        // Asegurar que existan las propiedades de modelo y tokens
        props.setProperty("anthropic.model", this.model != null ? this.model : "claude-sonnet-4-20250514");
        props.setProperty("anthropic.max-tokens", String.valueOf(this.maxTokens > 0 ? this.maxTokens : 4096));

        // Guardar de forma segura (manteniendo comentarios donde sea posible)
        savePropertiesWithComments(configFile, props);

        // Actualizar instancia en memoria
        this.apiKey = plainKey;
        this.configured = true;

        System.out.println("✅ API Key guardado y encriptado en: " + configFile.getAbsolutePath());
    }
}