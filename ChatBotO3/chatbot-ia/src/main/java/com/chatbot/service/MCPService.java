package com.chatbot.service;

import com.chatbot.config.MCPConfig;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MCPService {

    private final Object writeLock = new Object();
    private static final Logger log = LoggerFactory.getLogger(MCPService.class);

    private static final int MAX_RETRIES = 3;  // Número máximo de reintentos
    private static final long RETRY_DELAY = 1000; // Espera entre reintentos(Ver de quitarlo)

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
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
                // Validar streams
                if (mcpWriter == null || mcpReader == null) {
                    log.warn("mcpWriter/mcpReader aún no inicializados, intentando reinicializar...");
                    reinitializeConnection();
                }

                String requestStr = gson.toJson(request);
                log.debug("→ Enviando: {}", requestStr);

                synchronized (writeLock) {
                    if (mcpWriter == null) { // doble check
                        throw new IOException("mcpWriter no disponible tras reintento");
                    }
                    mcpWriter.write(requestStr);
                    mcpWriter.newLine();
                    mcpWriter.flush();
                }

                // Leer hasta encontrar respuesta JSON válida
                String line;
                while ((line = mcpReader.readLine()) != null) {
                    if (line.trim().startsWith("{")) {
                        log.debug("← Recibido: {}", line.substring(0, Math.min(200, line.length())) + "...");
                        return line;
                    } else {
                        log.debug("Ignorando línea no-JSON: {}", line);
                    }
                }

                // Si llegamos acá, no leímos línea válida: reintentar
                attempts++;
                Thread.sleep(RETRY_DELAY);

            } catch (IOException e) {
                log.error("Error en intento #{}: {}", attempts + 1, e.getMessage());
                attempts++;
                if (attempts >= MAX_RETRIES) {
                    throw e;
                }
                try {
                    reinitializeConnection();
                } catch (RuntimeException re) {
                    log.error("No se pudo reestablecer conexión: {}", re.getMessage());
                    throw new IOException("No se pudo reestablecer conexión", re);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Operación interrumpida", e);
            }
        }
        throw new IOException("No se pudo obtener respuesta JSON válida después de " + MAX_RETRIES + " intentos");
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
            if (mcpWriter != null) {
                try { mcpWriter.close(); } catch (IOException ignored) {}
                mcpWriter = null;
            }
            if (mcpReader != null) {
                try { mcpReader.close(); } catch (IOException ignored) {}
                mcpReader = null;
            }
        } catch (Exception e) {
            log.error("Error cerrando conexiones: " + e.getMessage());
        }
    }

    public MCPService() {
        this.config = MCPConfig.getInstance();
        this.gson = new Gson();
    }
    
    public boolean start() {
        if (!config.isEnabled()) {
            System.out.println("MCP O3 está deshabilitado en la configuración");
            return false;
        }
        
        System.out.println("\n========================================");
        System.out.println("   INICIANDO MCP O3 SERVER");
        System.out.println("========================================");
        
        try {
            File jarFile;
            String jarPath = config.getJarPath(); // "mcp/mcp_o3-0.0.4-SNAPSHOT.jar"
            
            // Intentar cargar desde resources
            System.out.println("Extrayendo JAR desde resources...");
            System.out.println("   Buscando: " + jarPath);
            
            InputStream jarStream = getClass().getClassLoader()
                .getResourceAsStream(jarPath);// Se copia a: /tmp/mcp_o3_12345.jar (temporal)
            
            if (jarStream == null) {
                System.err.println(" JAR no encontrado en resources: " + jarPath);
                System.err.println(" Ubicación esperada: src/main/resources/" + jarPath);
                return false;
            }
            
            // Crear archivo temporal
            jarFile = File.createTempFile("mcp_o3_", ".jar");
            jarFile.deleteOnExit();
            
            // Copiar desde resources al archivo temporal
            try (FileOutputStream out = new FileOutputStream(jarFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = jarStream.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            jarStream.close();
            
            System.out.println(" JAR extraído a: " + jarFile.getAbsolutePath());
            
            // Working directory - CRÍTICO para que el MCP encuentre su application.properties
            String workingDirPath = config.getWorkingDirectory();
            File workingDir = new File(workingDirPath);
            
            if (!workingDir.exists() || !workingDir.isDirectory()) {
                System.err.println(" Working directory no existe: " + workingDir.getAbsolutePath());
                System.err.println(" El MCP necesita este directorio para encontrar su configuración");
                return false;
            }
            
            System.out.println(" Working Dir: " + workingDir.getAbsolutePath());
            
            // Verificar que exista application.properties en el working dir
            File mcpConfig = new File(workingDir, "application.properties");
            if (!mcpConfig.exists()) {
                System.err.println(" Advertencia: application.properties no encontrado en " + workingDir.getAbsolutePath());
                System.err.println("   El MCP podría no funcionar correctamente sin su configuración");
            } else {
                System.out.println(" Config del MCP encontrado: " + mcpConfig.getAbsolutePath());
            }
            
            ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-Dspring.ai.mcp.server.stdio=true",
                "-Dspring.main.banner-mode=off",

                "-Do3.server.url=jdbc:o3:mdx://localhost:7777",
                "-Do3.server.username=user",
                "-Do3.server.password=user",
                "-Do3.server.columnsType=DIMENSION_LABEL",
                "-Do3.server.memberByLabel=true",

                "-jar",
                jarFile.getAbsolutePath()
            );
            
            // CRÍTICO: Establecer el working directory donde está la config del MCP
            pb.directory(workingDir);
            pb.redirectErrorStream(false);
            
            System.out.println("Iniciando proceso MCP...");
            System.out.println("   JAR: " + jarFile.getAbsolutePath());
            System.out.println("   Working Dir: " + workingDir.getAbsolutePath());
            
            mcpProcess = pb.start();
            
            // Inicializar streams INMEDIATAMENTE después de iniciar el proceso
            mcpWriter = new BufferedWriter(new OutputStreamWriter(mcpProcess.getOutputStream()));
            mcpReader = new BufferedReader(new InputStreamReader(mcpProcess.getInputStream()));
            
            // Verificar que el proceso inició correctamente
            if (!mcpProcess.isAlive()) {
                System.err.println("El proceso MCP no pudo iniciar");
                System.err.println("Verifica que Java esté instalado y en el PATH");
                return false;
            }
            
            System.out.println("Proceso MCP iniciado (PID: " + mcpProcess.pid() + ")");
            System.out.println("Esperando 3 segundos a que el MCP inicie completamente...");
            
            Thread.sleep(3000);

            // Inicializar la conexión MCP
            if (initializeMCP()) {
                System.out.println("MCP O3 Server iniciado correctamente");
                System.out.println("========================================\n");
                return true;
            } else {
                System.err.println("Error inicializando protocolo MCP");
                System.err.println("El proceso está corriendo pero no responde al protocolo");
                stop();
                return false;
            }
            
        } catch (IOException e) {
            System.err.println("Error iniciando MCP: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Proceso interrumpido");
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
            
            System.out.println(" Esperando respuesta del MCP (timeout: 2s)...");
            
            // AGREGAR TIMEOUT
            long startTime = System.currentTimeMillis();
            long timeout = 2000; // 2 segundos
            
            String response = null;
            while (response == null && (System.currentTimeMillis() - startTime) < timeout) {
                try {
                    response = sendRequest(initRequest);
                } catch (IOException e) {
                    System.err.println("Esperando conexión MCP...");
                    Thread.sleep(1000);
                }
            }
            
            if (response == null) {
                System.err.println("Timeout esperando respuesta del MCP");
                return false;
            }
            
            if (response.contains("\"result\"")) {
                System.out.println("MCP respondió correctamente");
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
            e.printStackTrace();
            return false;
        }
    }
    
    public String executeQuery(String mdxQuery) {
        System.out.println("\nEjecutando consulta MDX via MCP...");
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
            System.err.println("Error ejecutando query: " + e.getMessage());
            e.printStackTrace();
            return "Error ejecutando consulta: " + e.getMessage();
        }
    }
    
    public String listTools() {
        System.out.println("\nObteniendo lista de tools del MCP...");
        
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
            System.err.println("Error listando tools: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    private void sendNotification(JsonObject notification) throws IOException {
        if (mcpWriter == null) {
            throw new IOException("mcpWriter no inicializado para enviar notificación");
        }
        String notificationStr = gson.toJson(notification);
        synchronized (writeLock) {
            mcpWriter.write(notificationStr);
            mcpWriter.newLine();
            mcpWriter.flush();
        }
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

    public void stop() {
        log.info(" Deteniendo MCP O3 Server...");
        try {
            scheduler.shutdown();
        } catch (Exception e) {
            log.error("Error deteniendo MCP: {}", e.getMessage());
        }
    }
}
