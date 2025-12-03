package com.chatbot.service;

import com.chatbot.config.ClaudeConfig;
import com.chatbot.model.Message;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class ClaudeService {
    private ClaudeConfig config;
    private OkHttpClient client;
    private Gson gson;
    private MCPService mcpService;
    
    // Mantener contexto de conversación
    // Probar de pasar directamente toda la conversación en vez de solo los ultimos 10 mensajes <---
    private List<Message> conversationContext;
    private static final int MAX_CONTEXT_MESSAGES = 10; // Últimos 10 mensajes(Cambiar a futuro)
    
    // Constructor
    public ClaudeService() {
        this.config = ClaudeConfig.getInstance();
        this.client = new OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .callTimeout(180, TimeUnit.SECONDS)
            .build();
        this.gson = new Gson();
        this.conversationContext = new ArrayList<>();
        
        System.out.println("========================================");
        System.out.println("INICIALIZANDO CLAUDE SERVICE");
        System.out.println("========================================");
        System.out.println("API Key configurada: " + config.isConfigured());
        System.out.println("Modelo: " + config.getModel());
        System.out.println("Contexto conversacional: ACTIVADO");
        System.out.println("========================================\n");
    }
    // Setters
    public void setMCPService(MCPService mcpService) {
        this.mcpService = mcpService;
    }
    
    // Agrega contexto de conversación     
    public String generateResponseWithTools(String userMessage) {
        if (!config.isConfigured()) {
            return "ERROR: API Key de Anthropic no configurada";
        }
        
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║  CLAUDE CON CONTEXTO Y TOOLS           ║");
        System.out.println("╚════════════════════════════════════════╝");
        System.out.println(" Mensaje: " + userMessage);
        System.out.println(" Contexto: " + conversationContext.size() + " mensajes previos");
        
        try {
            // Agregar mensaje del usuario al contexto
            conversationContext.add(new Message("USER", userMessage));
            
            // Construir request con contexto
            JsonObject requestBody = buildToolCallRequestWithContext();
            String response = sendRequest(requestBody);
            
            // Procesar respuesta
            String claudeResponse = processToolCallResponse(response, userMessage);
            
            // Agregar respuesta de Claude al contexto
            conversationContext.add(new Message("CLAUDE", claudeResponse));
            
            // Mantener solo los últimos N mensajes
            trimContext();
            
            return claudeResponse;
            
        } catch (Exception e) {
            System.err.println(" Error: " + e.getMessage());
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
    
    // Construye request con historial de conversación
    private JsonObject buildToolCallRequestWithContext() {
        JsonObject request = new JsonObject();
        request.addProperty("model", config.getModel());
        request.addProperty("max_tokens", config.getMaxTokens());
        
        // Agregar system prompt mejorado
        String systemPrompt = buildSystemPrompt();
        request.addProperty("system", systemPrompt);
        
        // Construir historial de mensajes
        JsonArray messages = new JsonArray();
        
        for (Message msg : conversationContext) {
            JsonObject message = new JsonObject();
            message.addProperty("role", msg.getSender().equals("USER") ? "user" : "assistant");
            message.addProperty("content", msg.getContent());
            messages.add(message);
        }
        
        request.add("messages", messages);
        
        // Agregar tools
        JsonArray tools = new JsonArray();
        tools.add(buildExecuteMDXTool());
        request.add("tools", tools);
        
        return request;
    }
    
    // Construye System prompt mejorado con instrucciones de manejo de errores     
    private String buildSystemPrompt() {
        // Obtener instrucciones de queries exitosas
        QueryPersistenceService queryService = QueryPersistenceService.getInstance();
        String successfulExamples = queryService.generateInstructionsFromSuccessfulQueries();
        
        return """
            You are an expert assistant in data analysis with O3 and MDX queries.

            INTELLIGENT MDX ERROR HANDLING:
                        
            1. ANALIZE THE SPECIFIC ERROR:
               - Read the full error message carefully
               - Identify the exact reason of failure (cube, dimension, measure, sintax)
               - DO NOT assume generic solutions
            
            2. STRATEGIES BASED ON ERROR TYPE:
            
               A) "Cubo no encontrado / Cube does not exist":
                  - The specified cube does not exist on the server
                  - Ask the user what cubes are available
                  - Or suggest using exploratory queries to list cubes
                  - DO NOT switch to Demo cube automatically without user confirmation
            
               B) "Dimension not found / Member not found":
                  - The dimension or specified member does not exist un that cube
                  - Try similar names or query the cube structure
                  - Use exploratory queries like: SELECT {[DimensionName].Members}
            
               C) "Measure not found":
                  - The measure does not exist in that cube
                  - Query available measures: SELECT {Measures.Members}
                  - Then use the actual measures found
            
               D) "Syntax error":
                  - Review the MDX sintax
                  - Check parentheses, braces, commas
                  - Simplify the query if it is too complex
            
            3. RETRY PROCESS:
               - Only try if you can CORRECT the specific error
               - Explain which error you found and how you resolve it
               - If you can't fix it, ask the user for more information
               - DO NOT make assumptions about which cube or dimension to use
            
            4. EXPLORATORY QUERIES:
               When you don't know the structure:
               - List measures: SELECT {Measures.Members} ON COLUMNS FROM [CubeName]
               - List dimenisons: SELECT {Dimensions} ON COLUMNS FROM [CubeName] 
               - List Cube : SELECT {Cubes} ON COLUMNS FROM SYSCATALOG
            
            5. CONVERSATION CONTEXT:
               - Remember previous successful queries from the same cube
               - If the user mentions a specific cube , use THAT cube
               - DO NOT switch to a diferent cube without the user's explicit imput/request 
            
            6. COMUNICATION:
               - Explain clearly what you tried and what failed
               - If you can't resolve the problem/error, ask/say "necesito más información sobre..."
               - Ofer generic options, not specific ones
               - Keep a professional and honest tone/demeanor
            
            IMPORTANT: 
            - DO NOT assume all errors are solved by using the "Demo" cube
            - Each error has a specific cause that you must identify
            - It's better to ask for clarification than to make wrong assumptions
            """;        
    }
    
    //  Procesa respuesta con reintentos automáticos     
    private String processToolCallResponse(String responseBody, String originalMessage) {
        try {
            JsonObject root = gson.fromJson(responseBody, JsonObject.class);
            
            if (root.has("error")) {
                return "Error: " + parseErrorMessage(responseBody);
            }
            
            String stopReason = root.get("stop_reason").getAsString();
            JsonArray content = root.getAsJsonArray("content");
            
            if ("tool_use".equals(stopReason)) {
                return handleToolUseWithRetry(content, root, originalMessage, 0);
            }
            
            return extractTextFromContent(content);
            
        } catch (Exception e) {
            System.err.println(" Error procesando respuesta: " + e.getMessage());
            e.printStackTrace();
            return "Error procesando respuesta: " + e.getMessage();
        }
    }
    
    //Maneja tool use con reintentos automáticos     
    private String handleToolUseWithRetry(JsonArray content, JsonObject originalResponse, 
                                            String originalMessage, int attemptNumber) {
        final int MAX_ATTEMPTS = 3;
        
        try {
            JsonObject toolUse = null;
            String thinkingText = "";
            
            // Extraer tool_use y texto de "pensamiento"
            for (int i = 0; i < content.size(); i++) {
                JsonObject item = content.get(i).getAsJsonObject();
                String type = item.get("type").getAsString();
                
                if ("text".equals(type)) {
                    thinkingText = item.get("text").getAsString();
                } else if ("tool_use".equals(type)) {
                    toolUse = item;
                }
            }
            
            if (toolUse == null) {
                return "Error: No se encontró tool_use en la respuesta";
            }
            
            String toolName = toolUse.get("name").getAsString();
            String toolId = toolUse.get("id").getAsString();
            JsonObject input = toolUse.getAsJsonObject("input");
            
            System.out.println(" Intento #" + (attemptNumber + 1) + " - Claude quiere usar: " + toolName);
            if (!thinkingText.isEmpty()) {
                System.out.println(" Claude piensa: " + thinkingText.substring(0, Math.min(100, thinkingText.length())));
            }
            System.out.println(" Parámetros: " + input);
            
            // Ejecutar el tool
            String toolResult = executeTool(toolName, input);
            System.out.println(" Resultado obtenido (" + toolResult.length() + " caracteres)");
            
            // Verificar si el resultado es un error
            boolean isError = toolResult.contains("Error") || 
                                toolResult.contains("ERROR") ||
                                toolResult.contains("Exception");
            
            // Continuar con el resultado
            String finalResponse = continueWithToolResult(
                originalResponse, toolId, toolResult, originalMessage, 
                isError, attemptNumber
            );
            
            // Si es un error y no hemos superado max intentos, Claude podría reintentar
            if (isError && attemptNumber < MAX_ATTEMPTS - 1) {
                System.out.println(" Resultado contiene error, Claude decidirá si reintenta...");
            }
            
            return finalResponse;
            
        } catch (Exception e) {
            System.err.println(" Error en handleToolUseWithRetry: " + e.getMessage());
            e.printStackTrace();
            
            if (attemptNumber < MAX_ATTEMPTS - 1) {
                System.out.println(" Reintentando... (intento " + (attemptNumber + 2) + ")");
                return "Error en intento " + (attemptNumber + 1) + ": " + e.getMessage();
            }
            
            return "Error ejecutando tool después de " + MAX_ATTEMPTS + " intentos: " + e.getMessage();
        }
    }
    // Ejecutar el tool
    private String executeTool(String toolName, JsonObject input) {
        if ("executeCustomMdxQuery".equals(toolName)) {
            String mdxQuery = input.get("mdxQuery").getAsString();
            System.out.println(" Ejecutando MDX: " + mdxQuery);
            try { ConversationLogger.getInstance().logInfo("Claude solicita ejecutar tool: executeCustomMdxQuery"); } catch (Exception ignored) {}
            try { ConversationLogger.getInstance().logMCPQuery(mdxQuery); } catch (Exception ignored) {}
            
            try {
                String result = mcpService.executeQuery(mdxQuery);
                try { ConversationLogger.getInstance().logMCPResponse(result); } catch (Exception ignored) {}
                
                // Si el resultado es muy largo, truncarlo pero indicar que hay más
                if (result.length() > 4000) {
                    return result.substring(0, 4000) + 
                            "\n\n... (Resultado truncado. Total: " + result.length() + " caracteres)";
                }
                
                return result;
                
            } catch (Exception e) {
                return "Error ejecutando MDX: " + e.getMessage() + 
                        "\nConsulta: " + mdxQuery;
            }
        }
        return "Error: Tool desconocido: " + toolName;
    }
    
    // Continua con resultado del tool, permitiendo que Claude reintente SOLO si tiene sentido     
    private String continueWithToolResult(JsonObject originalResponse, String toolId, 
                                        String toolResult, String originalMessage,
                                        boolean wasError, int attemptNumber) {
        try {
            JsonObject request = new JsonObject();
            request.addProperty("model", config.getModel());
            request.addProperty("max_tokens", config.getMaxTokens());
            
            // Construir el system prompt completo
            String systemPrompt = buildSystemPrompt();
            
            if (wasError) {
                String errorContext;
                    errorContext = """
                        WARNING: the previous MDX query has failed.
                        
                        BEFORE RETRYING, ANALYZE THE ERROR CAREFULLY:
                        
                        1. Which type of error is it?
                        - Cube doesn't exist → DO NOT change cube without user input
                        - Dimension doesn t exist → Check which dimensions are available in that cube
                        - Measure doesn t exist → Check which measures are available in that cube
                        - Sintax → Fix the sintax (parentheses, braces, commas)
                        - Connection → IT ISN'T a query issue, inform the user
                        
                        2. Do you have enough information to retry?
                        YES → Try an exploratory query or a corrected query
                        NO → Explain the error to the user and ask for more info
                        
                        3. Is the cube you are using correct?
                        - If the user specified a cube, use THAT cube
                        - DO NOT switch to "Demo" or another default cube
                        - If the cube doesn't exist, ask the user which cubes are available
                        
                        4. Need information about the structure?
                        - Use exploratory queries in the SAME CUBE:
                            * SELECT {Measures.Members} → See measure
                            * SELECT {Dimensions} ON COLUMNS FROM [CubeName]  → See dimension
                        
                        REMEMBER:
                        - Analyze the full error message
                        - Explain what you tried and why it failed
                        - Only try again if you have a clear plan
                        - It's better to ask for clarification than to guess
                        
                        """.formatted(attemptNumber + 1);
                // Agregar el contexto de error al system prompt
                systemPrompt = systemPrompt + "\n\n" + errorContext;
            }
            
            // Agregar system prompt mejorado
            request.addProperty("system", systemPrompt);
            
            JsonArray messages = new JsonArray();
            
            // Mensaje original del usuario
            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", originalMessage);
            messages.add(userMsg);
            
            // Respuesta de Claude con el tool_use
            JsonObject assistantMsg = new JsonObject();
            assistantMsg.addProperty("role", "assistant");
            assistantMsg.add("content", originalResponse.getAsJsonArray("content"));
            messages.add(assistantMsg);
            
            // Resultado del tool
            JsonObject toolResultMsg = new JsonObject();
            toolResultMsg.addProperty("role", "user");
            
            JsonArray toolResultContent = new JsonArray();
            JsonObject toolResultBlock = new JsonObject();
            toolResultBlock.addProperty("type", "tool_result");
            toolResultBlock.addProperty("tool_use_id", toolId);
            
            // Incluir información adicional si fue error
            if (wasError) {
                // Agregar contexto adicional para ayudar a Claude
                String enhancedResult = toolResult + 
                    "\n\n[ANÁLISIS REQUERIDO: Lee el error cuidadosamente. " +
                    "¿Es un problema que puedes resolver con otra consulta? " +
                    "¿O necesitas preguntar al usuario? " +
                    "Intento: " + (attemptNumber + 1) + " de 3]";
                toolResultBlock.addProperty("content", enhancedResult);
                toolResultBlock.addProperty("is_error", true);
            } else {
                toolResultBlock.addProperty("content", toolResult);
            }
            
            toolResultContent.add(toolResultBlock);
            toolResultMsg.add("content", toolResultContent);
            messages.add(toolResultMsg);
            
            request.add("messages", messages);
            
            // Incluir tools para permitir reintentos (pero solo si no agotamos intentos)
            if (attemptNumber < 2) { // Máximo 3 intentos (0, 1, 2)
                JsonArray tools = new JsonArray();
                tools.add(buildExecuteMDXTool());
                request.add("tools", tools);
                System.out.println("   (Claude puede decidir reintentar - quedan " + (2 - attemptNumber) + " intentos)");
            } else {
                System.out.println("   (Último intento alcanzado - sin opción de tool_use)");
            }
            
            System.out.println(" Enviando resultado a Claude para análisis...");
            if (wasError) {
                System.out.println(" Claude analizará si puede corregir el error o necesita preguntar");
            }
            
            String response = sendRequest(request);
            JsonObject root = gson.fromJson(response, JsonObject.class);
            
            // Verificar si Claude quiere hacer otro tool_use (reintento inteligente)
            if (root.has("stop_reason") && "tool_use".equals(root.get("stop_reason").getAsString())) {
                System.out.println(" Claude identificó cómo corregir el error y reintenta...");
                return handleToolUseWithRetry(
                    root.getAsJsonArray("content"), 
                    root, 
                    originalMessage, 
                    attemptNumber + 1
                );
            }
            
            // Extraer respuesta final (Claude decidió no reintentar o explicar el error)
            if (root.has("content")) {
                return extractTextFromContent(root.getAsJsonArray("content"));
            }
            
            return "Error: No se pudo obtener interpretación final";
            
        } catch (Exception e) {
            System.err.println(" Error en continueWithToolResult: " + e.getMessage());
            e.printStackTrace();
            return "Resultados: " + toolResult + "\n\n(No se pudo obtener interpretación de Claude)";
        }
    }
    // Contiene descripcion y esquema de input para el tool executeCustomMdxQuery
    private JsonObject buildExecuteMDXTool() {
        JsonObject tool = new JsonObject();
        tool.addProperty("name", "executeCustomMdxQuery");
        tool.addProperty("description", """
            Execute a custom MDX query against the O3 cube server and return the results.
            Use the following guidelines to construct your MDX queries:

            COOMON MDX QUERY PATTERNS:
            1. Simple query by measure: SELECT {Measures.[MeasureName]} ON COLUMNS FROM [CubeName]
            2. By dimension: SELECT {Measures.[MeasureName]} ON COLUMNS, {[Dimension].children} ON ROWS FROM [CubeName]
            3. Multiple measures: SELECT {Measures.[MeasureName1], Measures.[MeasureName2]} ON COLUMNS FROM [CubeName]
            4. With WHERE filter: SELECT {Measures.[MeasureName]} ON COLUMNS FROM [CubeName] WHERE Measures.[MeasureFilter]
            5. NON EMPTY to omit empty/void variables/values: SELECT NON EMPTY {[Dimension].children} ON ROWS FROM [CubeName]
            6. CROSSJOIN to cross dimensions: CROSSJOIN({[Dimension1].children}, {[Dimension2].[SpecificMember]})
            7. Cube info: SELECT {CubeInfo.LastModifiedDate} ON COLUMNS FROM [CubeName]

            Interpretation examples (Use these only as a reference for constructing your own queries, do not copy them directly. Use only the cube the user specifies):
            1 - 'show units sold by location' → SELECT {Measures.[Units Sold]} ON COLUMNS, NON EMPTY {Location.children} ON ROWS FROM [CubeName]
            2 - 'costs and units sold by major accounts' → SELECT {Measures.[Cost], Measures.[Units Sold]} ON COLUMNS, {Customers.[Major Accounts]} ON ROWS FROM [CubeName]
            3 - 'earnings by product' → SELECT {Measures.[Revenue]} ON COLUMNS, NON EMPTY {Products.children} ON ROWS FROM [CubeName]
            4 - 'Units sold in France for client types Major Accounts and Minor Accounts'
            SELECT {Customers.[Major Accounts], Customers.[Minor Accounts]} ON COLUMNS, {Location.[France]} ON ROWS FROM [CubeName] WHERE (Measures.[Units Sold])
            5 - 'global vision of each salesman with units sold and commissions earned'
            SELECT {Measures.[Units Sold], Measures.[Commissions]} ON COLUMNS, {Salesmen.Seller.members} ON ROWS FROM [CubeName]
            6 - 'revenue for mountain bikes professional in US for years 2002 and 2003'
            SELECT {Date.Date.[2002], Date.Date.[2003]} ON COLUMNS, {Location.[US]} ON ROWS FROM [CubeName] WHERE (Products.[Mountain Bikes].[Professional], Measures.[Revenue])
            7 - 'show all major accounts in France with units sold'
            SELECT {Customers.[Major Accounts].children} ON COLUMNS, {Location.[France].children} ON ROWS FROM [CubeName] WHERE (Measures.[Units Sold])
            8 - 'children members of salesmen with total number of children'
            SELECT {Measures.children} ON COLUMNS, {Salesmen.Seller.members} ON ROWS FROM [CubeName]
            9 - 'query involves 3 dimensions: products, locations and dates. 3 axis visualization is complex so usually we show a bi-dimensional matrix combining dimensions'
            SELECT {Date.[2001], Date.[2002]} ON COLUMNS, {
                (Location.[Brazil], Products.[Mountain Bikes].[Professional]),
                (Location.[Brazil], Products.[Mountain Bikes].[Recreational]),
                (Location.[Spain], Products.[Mountain Bikes].[Professional]),
                (Location.[Spain], Products.[Mountain Bikes].[Recreational])
            } ON ROWS FROM [CubeName] WHERE (Measures.[Units Sold])
            10 - 'MDX CrossJoin returns the cartesian product of two sets'
            SELECT {Date.[2001], Date.[2002]} ON COLUMNS, CrossJoin({Location.children}, {Products.[Mountain Bikes].children}) ON ROWS FROM [CubeName] WHERE (Measures.[Units Sold])
            11 - 'evolution of cost per unit sold across years except 2002'
            SELECT except(Date.Year.Members, {Date.[2002]}) ON COLUMNS, {Products.Line.Members} ON ROWS FROM [CubeName] WHERE (Measures.[Cost])
            12 - 'Get all cities in France'
            SELECT {Location.[France].children} ON COLUMNS, {} ON ROWS FROM [CubeName]
            13 - 'Get all cities across every location'
            SELECT {Measures.[Units Sold]} ON COLUMNS, Descendants(Location, Location.City) ON ROWS FROM [CubeName]
            14 - 'Return elements in first set not in second'
            SELECT Except(Date.Year.Members, {Date.[2002]}) ON COLUMNS, {Products.Line.Members} ON ROWS FROM [CubeName] WHERE (Measures.[Cost])
            15 - 'List available cubes on the server'
            SELECT {Cubes} ON COLUMNS FROM SYSCATALOG
            16 - 'List dimensions of a cube'
            SELECT {Dimensions} ON COLUMNS FROM [CubeName]
            17 - 'List measures of a cube'
            SELECT {Measures.Members} ON COLUMNS FROM [CubeName]
            """);        
        
        JsonObject inputSchema = new JsonObject();
        inputSchema.addProperty("type", "object");
        
        JsonObject properties = new JsonObject();
        JsonObject mdxQueryProp = new JsonObject();
        mdxQueryProp.addProperty("type", "string");
        mdxQueryProp.addProperty("description", 
            "Consulta MDX a ejecutar. Si el resultado es un error, analiza el mensaje " +
            "específico antes de decidir si reintentar o pedir más información al usuario.");
        properties.add("mdxQuery", mdxQueryProp);
        
        inputSchema.add("properties", properties);
        
        JsonArray required = new JsonArray();
        required.add("mdxQuery");
        inputSchema.add("required", required);
        
        tool.add("input_schema", inputSchema);
        
        return tool;
    }
    // Extrae texto de la respuesta de Claude
    private String extractTextFromContent(JsonArray content) {
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < content.size(); i++) {
            JsonObject item = content.get(i).getAsJsonObject();
            if ("text".equals(item.get("type").getAsString())) {
                result.append(item.get("text").getAsString());
            }
        }
        
        return result.toString();
    }
    // Envía la solicitud HTTP a la API de Claude
    private String sendRequest(JsonObject requestBody) throws IOException {
        String jsonBody = gson.toJson(requestBody);
        
        RequestBody body = RequestBody.create(
            jsonBody,
            MediaType.parse("application/json; charset=utf-8")
        );
        
        Request request = new Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .post(body)
            .addHeader("Content-Type", "application/json")
            .addHeader("x-api-key", config.getApiKey())
            .addHeader("anthropic-version", "2023-06-01")
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            
            if (!response.isSuccessful()) {
                System.err.println(" ERROR HTTP " + response.code());
                System.err.println(responseBody);
                throw new IOException("HTTP " + response.code() + ": " + parseErrorMessage(responseBody));
            }
            
            return responseBody;
        }
    }
    
    // Limpia el contexto manteniendo solo los últimos N mensajes
    private void trimContext() {
        if (conversationContext.size() > MAX_CONTEXT_MESSAGES) {
            int toRemove = conversationContext.size() - MAX_CONTEXT_MESSAGES;
            conversationContext = new ArrayList<>(
                conversationContext.subList(toRemove, conversationContext.size())
            );
            System.out.println(" Contexto trimmed: manteniendo últimos " + MAX_CONTEXT_MESSAGES + " mensajes");
        }
    }
    
    // Limpia el contexto completamente
    public void clearContext() {
        conversationContext.clear();
        System.out.println(" Contexto de conversación limpiado");
    }
    // Obtiene el tamaño del contexto actual
    public int getContextSize() {
        return conversationContext.size();
    }    
    // Analiza el mensaje de error de Claude    
    private String parseErrorMessage(String responseBody) {
        try {
            JsonObject root = gson.fromJson(responseBody, JsonObject.class);
            if (root.has("error")) {
                JsonObject error = root.getAsJsonObject("error");
                if (error.has("message")) {
                    return error.get("message").getAsString();
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return responseBody;
    }
}