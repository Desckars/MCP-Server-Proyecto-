package com.chatbot.service;

import com.chatbot.config.MCPConfig;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MCPService {

    private static final Logger log = LoggerFactory.getLogger(MCPService.class);
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY = 1000; // 1 segundo

    private MCPConfig config;
    private Process mcpProcess;
    private BufferedWriter mcpWriter;
    private BufferedReader mcpReader;
    private Gson gson;
    private int requestId = 0;

    private String sendRequest(JsonObject request) throws IOException {
    int attempts = 0;
    while (attempts < MAX_RETRIES) {
        try {
            String requestStr = gson.toJson(request);
            log.debug("→ Enviando: {}", requestStr);
            
            synchronized (mcpWriter) {
                mcpWriter.write(requestStr);
                mcpWriter.newLine();
                mcpWriter.flush();
                
                // Leer hasta encontrar respuesta JSON válida
                String line;
                while ((line = mcpReader.readLine()) != null) {
                    if (line.trim().startsWith("{")) {
                        log.debug("← Recibido: {}", 
                            line.substring(0, Math.min(200, line.length())) + "...");
                        return line;
                    } else {
                        log.debug("Ignorando línea no-JSON: {}", line);
                    }
                }
            }
            
            attempts++;
            Thread.sleep(RETRY_DELAY);
            
        } catch (IOException e) {
            log.error("Error en intento #{}: {}", attempts + 1, e.getMessage());
            if (attempts >= MAX_RETRIES - 1) {
                throw e;
            }
            reinitializeConnection();
            attempts++;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Operación interrumpida", e);
        }
    }
    throw new IOException("No se pudo obtener respuesta JSON válida después de " + MAX_RETRIES + " intentos");
}
    
    private boolean isWriterValid() {
        try {
            mcpWriter.write("");
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    private boolean verifyConnection() {
    try {
        JsonObject pingRequest = new JsonObject();
        pingRequest.addProperty("jsonrpc", "2.0");
        pingRequest.addProperty("id", ++requestId);
        pingRequest.addProperty("method", "ping");
        
        String response = sendRequest(pingRequest);
        return response != null && response.contains("\"result\"");
    } catch (Exception e) {
        log.error("Error verificando conexión: {}", e.getMessage());
        return false;
    }
}

private void initializeConnections() throws IOException {
    if (mcpProcess == null || !mcpProcess.isAlive()) {
        log.warn("Proceso MCP no está activo, reiniciando...");
        start();
    }
    
    mcpWriter = new BufferedWriter(new OutputStreamWriter(mcpProcess.getOutputStream()));
    mcpReader = new BufferedReader(new InputStreamReader(mcpProcess.getInputStream()));
}
    private void reinitializeConnection() {
    int retries = 0;
    while (retries < MAX_RETRIES) {
        try {
            log.info("Intento de reconexión #{}", retries + 1);
            closeConnections();
            initializeConnections();
            if (verifyConnection()) {
                log.info("Reconexión exitosa");
                return;
            }
            retries++;
            Thread.sleep(RETRY_DELAY);
        } catch (Exception e) {
            log.error("Error en intento de reconexión #{}: {}", retries + 1, e.getMessage());
            retries++;
        }
    }
    throw new RuntimeException("No se pudo restablecer la conexión después de " + MAX_RETRIES + " intentos");
}
    
    private void closeConnections() {
        try {
            if (mcpWriter != null) mcpWriter.close();
            if (mcpReader != null) mcpReader.close();
        } catch (IOException e) {
            log.error("Error cerrando conexiones: " + e.getMessage());
        }
    }
    public MCPService() {
        this.config = MCPConfig.getInstance();
        this.gson = new Gson();
    }
    
    public boolean start() {
        if (!config.isEnabled()) {
            System.out.println("⚠ MCP O3 está deshabilitado en la configuración");
            return false;
        }
        
        System.out.println("\n📡 ========================================");
        System.out.println("   INICIANDO MCP O3 SERVER");
        System.out.println("========================================");
        
        try {
            File jarFile = new File(config.getJarPath());
            if (!jarFile.exists()) {
                System.err.println("❌ JAR del MCP no encontrado: " + config.getJarPath());
                return false;
            }
            
            ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-jar",
                jarFile.getAbsolutePath()
            );
            
            File workingDir = new File(config.getWorkingDirectory());
            if (workingDir.exists() && workingDir.isDirectory()) {
                pb.directory(workingDir);
            }
            
            pb.redirectErrorStream(false);
            
            System.out.println("🚀 Iniciando proceso MCP...");
            System.out.println("   JAR: " + jarFile.getAbsolutePath());
            System.out.println("   Working Dir: " + workingDir.getAbsolutePath());
            
            mcpProcess = pb.start();
            
            mcpWriter = new BufferedWriter(new OutputStreamWriter(mcpProcess.getOutputStream()));
            mcpReader = new BufferedReader(new InputStreamReader(mcpProcess.getInputStream()));
            
            // Inicializar la conexión MCP
            if (initializeMCP()) {
                System.out.println("✅ MCP O3 Server iniciado correctamente");
                System.out.println("========================================\n");
                return true;
            } else {
                System.err.println("❌ Error inicializando MCP");
                stop();
                return false;
            }
            
        } catch (IOException e) {
            System.err.println("❌ Error iniciando MCP: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    //-------------------------------
    private boolean initializeMCP() {
        try {
            // Enviar initialize request
            JsonObject initRequest = new JsonObject();
            initRequest.addProperty("jsonrpc", "2.0");
            initRequest.addProperty("id", ++requestId);
            initRequest.addProperty("method", "initialize");
            
            JsonObject params = new JsonObject();
            params.addProperty("protocolVersion", "2024-11-05");
            
            JsonObject clientInfo = new JsonObject();
            clientInfo.addProperty("name", "chatbot-ia");
            clientInfo.addProperty("version", "2.0");
            params.add("clientInfo", clientInfo);
            
            initRequest.add("params", params);
            
            String response = sendRequest(initRequest);
            
            if (response != null && response.contains("\"result\"")) {
                // Enviar initialized notification
                JsonObject notification = new JsonObject();
                notification.addProperty("jsonrpc", "2.0");
                notification.addProperty("method", "notifications/initialized");
                sendNotification(notification);
                
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            System.err.println("Error en initializeMCP: " + e.getMessage());
            return false;
        }
    }
    
    public String executeQuery(String mdxQuery) {
        System.out.println("\n📊 Ejecutando consulta MDX via MCP...");
        System.out.println("   Query: " + mdxQuery);
        
        try {
            // Llamar al tool executeCustomMdxQuery del MCP
            JsonObject toolCall = new JsonObject();
            toolCall.addProperty("jsonrpc", "2.0");
            toolCall.addProperty("id", ++requestId);
            toolCall.addProperty("method", "tools/call");
            
            JsonObject params = new JsonObject();
            params.addProperty("name", "executeCustomMdxQuery");
            
            JsonObject arguments = new JsonObject();
            arguments.addProperty("mdxQuery", mdxQuery);
            params.add("arguments", arguments);
            
            toolCall.add("params", params);
            
            String response = sendRequest(toolCall);
            
            if (response != null) {
                return parseToolResponse(response);
            }
            
            return "Error: No se obtuvo respuesta del MCP";
            
        } catch (Exception e) {
            System.err.println("❌ Error ejecutando query: " + e.getMessage());
            e.printStackTrace();
            return "Error ejecutando consulta: " + e.getMessage();
        }
    }
    
    public String listTools() {
        System.out.println("\n🔧 Obteniendo lista de tools del MCP...");
        
        try {
            JsonObject request = new JsonObject();
            request.addProperty("jsonrpc", "2.0");
            request.addProperty("id", ++requestId);
            request.addProperty("method", "tools/list");
            request.add("params", new JsonObject());
            
            String response = sendRequest(request);
            
            if (response != null) {
                JsonObject root = gson.fromJson(response, JsonObject.class);
                if (root.has("result")) {
                    JsonObject result = root.getAsJsonObject("result");
                    if (result.has("tools")) {
                        JsonArray tools = result.getAsJsonArray("tools");
                        StringBuilder sb = new StringBuilder("Tools disponibles en MCP O3:\n\n");
                        
                        for (int i = 0; i < tools.size(); i++) {
                            JsonObject tool = tools.get(i).getAsJsonObject();
                            String name = tool.get("name").getAsString();
                            String description = tool.has("description") 
                                ? tool.get("description").getAsString() 
                                : "Sin descripción";
                            
                            sb.append((i + 1)).append(". ").append(name).append("\n");
                            sb.append("   ").append(description).append("\n\n");
                        }
                        
                        return sb.toString();
                    }
                }
            }
            
            return "No se pudieron obtener los tools";
            
        } catch (Exception e) {
            System.err.println("❌ Error listando tools: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }
    
    
    private void sendNotification(JsonObject notification) throws IOException {
        String notificationStr = gson.toJson(notification);
        mcpWriter.write(notificationStr);
        mcpWriter.newLine();
        mcpWriter.flush();
    }
    
    private String parseToolResponse(String response) {
        try {
        // Ignorar líneas de log
        if (response.contains("INFO") || response.contains("DEBUG") || response.contains("WARN")) {
            log.warn("Respuesta contiene logs, intentando leer siguiente línea...");
            String nextLine = mcpReader.readLine();
            if (nextLine != null) {
                response = nextLine;
            }
        }

        // Validar que la respuesta sea JSON
        if (!response.trim().startsWith("{")) {
            log.error("Respuesta no válida: {}", response);
            return "Error: Respuesta no válida del MCP";
        }

        JsonObject root = gson.fromJson(response, JsonObject.class);
            if (root.has("result")) {
                JsonObject result = root.getAsJsonObject("result");
                if (result.has("content")) {
                    JsonArray content = result.getAsJsonArray("content");
                    if (content.size() > 0) {
                        JsonObject firstContent = content.get(0).getAsJsonObject();
                        if (firstContent.has("text")) {
                            return firstContent.get("text").getAsString();
                        }
                    }
                }
            }
            
            if (root.has("error")) {
                JsonObject error = root.getAsJsonObject("error");
                String errorMsg = error.has("message") 
                    ? error.get("message").getAsString() 
                    : "Error desconocido";
                return "Error del MCP: " + errorMsg;
            }
            
            return "Respuesta inesperada del MCP";
            
            } catch (JsonSyntaxException e) {
            log.error("Error parseando JSON: {}", e.getMessage());
            log.debug("Respuesta raw: {}", response);
            return "Error: Formato de respuesta inválido";
        } catch (Exception e) {
            log.error("Error procesando respuesta: {}", e.getMessage());
            return "Error interno procesando respuesta";
        }
    }    
    public boolean isRunning() {
        return mcpProcess != null && mcpProcess.isAlive();
    }

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

public void startHealthCheck() {
    scheduler.scheduleAtFixedRate(() -> {
        try {
            if (!verifyConnection()) {
                log.warn("Healthcheck falló, intentando reconexión...");
                reinitializeConnection();
            }
        } catch (Exception e) {
            log.error("Error en healthcheck: {}", e.getMessage());
        }
    }, 30, 30, TimeUnit.SECONDS);
}

public void stop() {
    log.info("🛑 Deteniendo MCP O3 Server...");
    try {
        scheduler.shutdown();
        // ...existing stop code...
    } catch (Exception e) {
        log.error("Error deteniendo MCP: {}", e.getMessage());
    }
}
}
