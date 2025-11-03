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
    private boolean eng = true;
    
    // NUEVO: Mantener contexto de conversación
    private List<Message> conversationContext;
    private static final int MAX_CONTEXT_MESSAGES = 10; // Últimos 10 mensajes
    
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
    
    public void setMCPService(MCPService mcpService) {
        this.mcpService = mcpService;
    }
    
    /**
     * MEJORADO: Agrega contexto de conversación
     */
    public String generateResponseWithTools(String userMessage) {
        if (!config.isConfigured()) {
            return "ERROR: API Key de Anthropic no configurada";
        }
        
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║  CLAUDE CON CONTEXTO Y TOOLS           ║");
        System.out.println("╚════════════════════════════════════════╝");
        System.out.println("📝 Mensaje: " + userMessage);
        System.out.println("📚 Contexto: " + conversationContext.size() + " mensajes previos");
        
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
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
    
    /**
     * NUEVO: Construye request con historial de conversación
     */
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
    
    /**
     * NUEVO: System prompt mejorado con instrucciones de manejo de errores
     */
    private String buildSystemPrompt() {
        if(eng){
        return """
            You are an expert assistant in data analysis with Oracle Essbase/O3 and MDX queries.

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
               - List dimenisons: SELECT {[DimensionName].Members} ON COLUMNS FROM [CubeName]
               - See basic structure: SELECT {} ON COLUMNS FROM [CubeName]
            
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
        return """
            Eres un asistente experto en análisis de datos con Oracle Essbase/O3 y consultas MDX.
            
            MANEJO INTELIGENTE DE ERRORES MDX:
            
            1. ANALIZAR EL ERROR ESPECÍFICO:
               - Lee cuidadosamente el mensaje de error completo
               - Identifica QUÉ falló exactamente (cubo, dimensión, medida, sintaxis)
               - NO asumas soluciones genéricas
            
            2. ESTRATEGIAS SEGÚN EL TIPO DE ERROR:
            
               A) "Cubo no encontrado / Cube does not exist":
                  - El cubo especificado NO existe en el servidor
                  - Pregunta al usuario qué cubos tiene disponibles
                  - O sugiere usar consultas exploratorias para listar cubos
                  - NO cambies al cubo Demo automáticamente
            
               B) "Dimension not found / Member not found":
                  - La dimensión o miembro específico no existe en ESE cubo
                  - Intenta con nombres similares o consulta la estructura del cubo
                  - Usa consultas exploratorias: SELECT {[DimensionName].Members}
            
               C) "Measure not found":
                  - La medida no existe en ese cubo
                  - Consulta medidas disponibles: SELECT {Measures.Members}
                  - Luego usa las medidas reales encontradas
            
               D) "Syntax error":
                  - Revisa la sintaxis MDX
                  - Verifica paréntesis, llaves, comas
                  - Simplifica la consulta si es muy compleja
            
            3. PROCESO DE REINTENTO:
               - Solo reintenta si puedes CORREGIR el error específico
               - Explica qué error encontraste y cómo lo corriges
               - Si no puedes corregirlo, pide más información al usuario
               - NO hagas suposiciones sobre qué cubo o dimensión usar
            
            4. CONSULTAS EXPLORATORIAS:
               Cuando no conozcas la estructura:
               - Listar medidas: SELECT {Measures.Members} ON COLUMNS FROM [CubeName]
               - Listar dimensión: SELECT {[DimensionName].Members} ON COLUMNS FROM [CubeName]
               - Ver estructura básica: SELECT {} ON COLUMNS FROM [CubeName]
            
            5. CONTEXTO DE CONVERSACIÓN:
               - Recuerda consultas exitosas previas del MISMO cubo
               - Si el usuario mencionó un cubo, usa ESE cubo
               - No cambies de cubo sin que el usuario lo pida
            
            6. COMUNICACIÓN:
               - Explica claramente qué intentaste y qué falló
               - Si no puedes resolver el error, di "necesito más información sobre..."
               - Ofrece opciones específicas, no genéricas
               - Mantén un tono profesional y honesto
            
            IMPORTANTE: 
            - NO asumas que todos los errores se resuelven usando el cubo "Demo"
            - Cada error tiene una causa específica que debes identificar
            - Es mejor pedir aclaración que hacer suposiciones incorrectas
            """;
    }
    
    /**
     * MEJORADO: Procesa respuesta con reintentos automáticos
     */
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
            System.err.println("❌ Error procesando respuesta: " + e.getMessage());
            e.printStackTrace();
            return "Error procesando respuesta: " + e.getMessage();
        }
    }
    
    /**
     * NUEVO: Maneja tool use con reintentos automáticos
     */
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
            
            System.out.println("🔧 Intento #" + (attemptNumber + 1) + " - Claude quiere usar: " + toolName);
            if (!thinkingText.isEmpty()) {
                System.out.println("💭 Claude piensa: " + thinkingText.substring(0, Math.min(100, thinkingText.length())));
            }
            System.out.println("📝 Parámetros: " + input);
            
            // Ejecutar el tool
            String toolResult = executeTool(toolName, input);
            System.out.println("✅ Resultado obtenido (" + toolResult.length() + " caracteres)");
            
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
                System.out.println("⚠️  Resultado contiene error, Claude decidirá si reintenta...");
            }
            
            return finalResponse;
            
        } catch (Exception e) {
            System.err.println("❌ Error en handleToolUseWithRetry: " + e.getMessage());
            e.printStackTrace();
            
            if (attemptNumber < MAX_ATTEMPTS - 1) {
                System.out.println("🔄 Reintentando... (intento " + (attemptNumber + 2) + ")");
                return "Error en intento " + (attemptNumber + 1) + ": " + e.getMessage();
            }
            
            return "Error ejecutando tool después de " + MAX_ATTEMPTS + " intentos: " + e.getMessage();
        }
    }
    
    private String executeTool(String toolName, JsonObject input) {
        if ("executeCustomMdxQuery".equals(toolName)) {
            String mdxQuery = input.get("mdxQuery").getAsString();
            System.out.println("📊 Ejecutando MDX: " + mdxQuery);
            
            try {
                String result = mcpService.executeQuery(mdxQuery);
                
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
    
    /**
     * MEJORADO: Continua con resultado del tool, permitiendo que Claude reintente SOLO si tiene sentido
     */
    private String continueWithToolResult(JsonObject originalResponse, String toolId, 
                                         String toolResult, String originalMessage,
                                         boolean wasError, int attemptNumber) {
        try {
            JsonObject request = new JsonObject();
            request.addProperty("model", config.getModel());
            request.addProperty("max_tokens", config.getMaxTokens());
            
            // Agregar system prompt con contexto de error si aplica
            if (wasError) {
                if(eng){
                String errorContext = """
                    ⚠️ WARNING: the previous MDX query has failed.
                    
                    BEFORE RETRYING, ANALYZE THE ERROR CAREFULLY:
                    
                    1. Which type of error is it?
                       - Cube doesn't exist → DO NOT change cube without user input
                       - Dimension doesn´t exist → Check which dimensions are available in that cube
                       - Measure doesn´t exist → Check which measures are available in that cube
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
                         * SELECT {[DimName].Members} → See dimension
                    
                    REMEMBER:
                    - Analyze the full error message
                    - Explain what you tried and why it failed
                    - Only try again if you have a clear plan
                    - It's better to ask for clarification than to guess
                    
                    Attemps done: %d de 3
                """.formatted(attemptNumber + 1);
                }else{
                String errorContext = """
                ⚠ ATENCIÓN: La consulta MDX anterior falló.
                    
                    ANTES DE REINTENTAR, ANALIZA:
                    
                    1. ¿Qué tipo de error fue?
                       - Cubo no existe → NO cambies a otro cubo sin confirmar
                       - Dimensión no existe → Consulta qué dimensiones tiene ese cubo
                       - Medida no existe → Consulta qué medidas tiene ese cubo
                       - Sintaxis → Corrige la sintaxis
                       - Conexión → NO es problema de tu consulta, informa al usuario
                    
                    2. ¿Tienes información suficiente para corregir?
                       SÍ → Intenta una consulta exploratoria o corregida
                       NO → Explica al usuario el error y pide más información
                    
                    3. ¿Es el cubo correcto?
                       - Si el usuario pidió un cubo específico, usa ESE cubo
                       - NO cambies a "Demo" u otro cubo por defecto
                       - Si el cubo no existe, pregunta qué cubos tiene disponibles
                    
                    4. ¿Necesitas información sobre la estructura?
                       - Usa consultas exploratorias en el MISMO cubo:
                         * SELECT {Measures.Members} → Ver medidas
                         * SELECT {[DimName].Members} → Ver dimensión
                    
                    RECUERDA:
                    - Analiza el mensaje de error completo
                    - Explica qué intentaste y por qué falló
                    - Solo reintenta si tienes un plan claro
                    - Es mejor pedir aclaración que adivinar
                    
                    Intentos usados: %d de 3
                """.formatted(attemptNumber + 1);
                }
                request.addProperty("system", buildSystemPrompt() + "\n\n" + errorContext);
            } else {
                request.addProperty("system", buildSystemPrompt());
            }
            
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
            
            System.out.println("🔄 Enviando resultado a Claude para análisis...");
            if (wasError) {
                System.out.println("   ⚠️  Claude analizará si puede corregir el error o necesita preguntar");
            }
            
            String response = sendRequest(request);
            JsonObject root = gson.fromJson(response, JsonObject.class);
            
            // Verificar si Claude quiere hacer otro tool_use (reintento inteligente)
            if (root.has("stop_reason") && "tool_use".equals(root.get("stop_reason").getAsString())) {
                System.out.println("🔄 Claude identificó cómo corregir el error y reintenta...");
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
            System.err.println("❌ Error en continueWithToolResult: " + e.getMessage());
            e.printStackTrace();
            return "Resultados: " + toolResult + "\n\n(No se pudo obtener interpretación de Claude)";
        }
    }
    
    private JsonObject buildExecuteMDXTool() {
        JsonObject tool = new JsonObject();
        tool.addProperty("name", "executeCustomMdxQuery");
        if(eng){
            tool.addProperty("description", """
            Executes a specific MDX query against Oracle O3/Essbase OLAP cubes and returns formatted results.
            
            ⚠️ INTELLIGENT ERROR HANDLING
            
            If you get an ERROR, analize the specific error message:
            
            1) "Cube 'X' not found" / "Cubo no existe":
               → The specified cube does not exist on the server
               → Ask the user what cubes are available
               → Or suggest using exploratory queries to list cubes
               → DO NOT switch to Demo cube automatically without user confirmation
            
            2) "Dimension 'X' not found" / "Member 'X' not found":
               → That dimension/member does not exist in THAT specific cube
               → First ask what dimensions are available: 
                 SELECT {Dimensions.Members} ON COLUMNS FROM [CubeName]
               → Then use the actual dimensions/members you found
            
            3) "Measure 'X' not found":
               → That measure does not exist in that cube
               → Query available measures:
                 SELECT {Measures.Members} ON COLUMNS FROM [CubeName]
               → Use the actual measures if that cube
            
            4) "Syntax error" / Error de sintaxis:
               → Review the MDX sintax (braces, parentheses, FROM, WHERE)
               → Simplify the query
               → Verify the correct format

            5) "Connection refused" / "Server not available":
               → The server O3/Essbase isn't accesible
               → Inform the user that the server is down
               → DO NOT attempt to retry, it's not a query issue
            
            📋 EXPLORATORY QUERIES (When you don't know the structure):
            
            - See all measures in a cube:
              SELECT {Measures.Members} ON COLUMNS FROM [CubeName]
            
            - See all members of a dimension:
              SELECT {[DimensionName].Members} ON COLUMNS FROM [CubeName]
            
            - See basic structure:
              SELECT {} ON COLUMNS FROM [CubeName]
            
            - See hierarchy of a dimension:
              SELECT {[DimensionName].Levels(0).Members} ON COLUMNS FROM [CubeName]
            
            📚 CUBE EXAMPLES AND KNOWN STRUCTURES:
            
            CUBE "Demo" (if the user mentions it):
            - Dimensions: Customers, Location, Products, Date, Salesmen
            - Measures: Units Sold, Cost, Revenue, Commissions, Discount
            
            BUT REMEMBER: The user can use other cubes with different a structure.
            DO NOT ASSUME that each and every one is like Demo or similar. 
            
            🎯 RETRY STATEGIES:
            
            ONLY retry if:
            ✅ You've identified the specific prioblem (badly written dimension name, etc.)
            ✅ You have enough info to fix it
            ✅ You can use an exploratory query to get info
            
            NO reintentes si:
            ❌ The error is a connection issue or server related
            ❌ You don't know what went wrong
            ❌ You need more info from the user
            
            In those cases, Explain the error to the user and aks for their help.
            
            💡 GOLDEL RULE:
            It's far better to ask the user for clarification, than to assume wrongly and make it worse.
            """);
        }else{
            tool.addProperty("description", """
            Ejecuta una consulta MDX específica contra cubos OLAP de Oracle O3/Essbase y retorna los resultados formateados.
            
            ⚠️ MANEJO INTELIGENTE DE ERRORES:
            
            Si recibes un ERROR, analiza el mensaje específico:
            
            1) "Cube 'X' not found" / "Cubo no existe":
               → El cubo especificado no existe en el servidor
               → NO cambies automáticamente a otro cubo
               → Pregunta al usuario qué cubos tiene disponibles
               → O usa una consulta exploratoria si el usuario no sabe
            
            2) "Dimension 'X' not found" / "Member 'X' not found":
               → Esa dimensión/miembro no existe en ESE cubo específico
               → Primero consulta qué dimensiones tiene: 
                 SELECT {Dimensions.Members} ON COLUMNS FROM [CubeName]
               → Luego usa las dimensiones reales que encontraste
            
            3) "Measure 'X' not found":
               → Esa medida no existe en ese cubo
               → Consulta medidas disponibles:
                 SELECT {Measures.Members} ON COLUMNS FROM [CubeName]
               → Usa las medidas reales del cubo
            
            4) "Syntax error" / Error de sintaxis:
               → Revisa la sintaxis MDX (llaves, paréntesis, FROM, WHERE)
               → Simplifica la consulta
               → Verifica el formato correcto
            
            5) "Connection refused" / "Server not available":
               → El servidor O3/Essbase no está accesible
               → Informa al usuario que el servidor está caído
               → NO intentes reintentar, no es un problema de la consulta
            
            📋 CONSULTAS EXPLORATORIAS (cuando NO conozcas la estructura):
            
            - Ver todas las medidas de un cubo:
              SELECT {Measures.Members} ON COLUMNS FROM [CubeName]
            
            - Ver miembros de una dimensión:
              SELECT {[DimensionName].Members} ON COLUMNS FROM [CubeName]
            
            - Ver estructura básica:
              SELECT {} ON COLUMNS FROM [CubeName]
            
            - Ver jerarquía de una dimensión:
              SELECT {[DimensionName].Levels(0).Members} ON COLUMNS FROM [CubeName]
            
            📚 EJEMPLOS DE CUBOS Y ESTRUCTURAS CONOCIDAS:
            
            CUBO "Demo" (si el usuario lo menciona):
            - Dimensiones: Customers, Location, Products, Date, Salesmen
            - Medidas: Units Sold, Cost, Revenue, Commissions, Discount
            
            Pero RECUERDA: El usuario puede tener OTROS cubos con OTRAS estructuras.
            NO asumas que todos usan Demo.
            
            🎯 ESTRATEGIA DE REINTENTOS:
            
            SOLO reintenta si:
            ✅ Identificaste el problema específico (dimensión mal escrita, etc.)
            ✅ Tienes información para corregirlo
            ✅ Puedes usar una consulta exploratoria para obtener info
            
            NO reintentes si:
            ❌ El error es de conexión al servidor
            ❌ No entiendes qué salió mal
            ❌ Necesitas que el usuario te dé más información
            
            En esos casos, explica el error y pide ayuda al usuario.
            
            💡 REGLA DE ORO:
            Es mejor pedir aclaración al usuario que hacer suposiciones incorrectas.
            """);
        }
        
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
                System.err.println("❌ ERROR HTTP " + response.code());
                System.err.println(responseBody);
                throw new IOException("HTTP " + response.code() + ": " + parseErrorMessage(responseBody));
            }
            
            return responseBody;
        }
    }
    
    /**
     * NUEVO: Limpia el contexto manteniendo solo los últimos N mensajes
     */
    private void trimContext() {
        if (conversationContext.size() > MAX_CONTEXT_MESSAGES) {
            int toRemove = conversationContext.size() - MAX_CONTEXT_MESSAGES;
            conversationContext = new ArrayList<>(
                conversationContext.subList(toRemove, conversationContext.size())
            );
            System.out.println("🗑️  Contexto trimmed: manteniendo últimos " + MAX_CONTEXT_MESSAGES + " mensajes");
        }
    }
    
    /**
     * NUEVO: Limpia el contexto completamente
     */
    public void clearContext() {
        conversationContext.clear();
        System.out.println("🗑️  Contexto de conversación limpiado");
    }
    
    /**
     * NUEVO: Obtiene el tamaño del contexto actual
     */
    public int getContextSize() {
        return conversationContext.size();
    }
    
    // Métodos legacy para compatibilidad
    public String generateResponse(String userMessage) {
        return generateResponse(userMessage, null);
    }
    
    public String generateResponse(String userMessage, String systemPrompt) {
        if (!config.isConfigured()) {
            return "ERROR: API Key no configurada";
        }
        
        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", config.getModel());
            requestBody.addProperty("max_tokens", config.getMaxTokens());
            
            if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
                requestBody.addProperty("system", systemPrompt);
            }
            
            JsonArray messages = new JsonArray();
            JsonObject message = new JsonObject();
            message.addProperty("role", "user");
            message.addProperty("content", userMessage);
            messages.add(message);
            requestBody.add("messages", messages);
            
            String response = sendRequest(requestBody);
            return parseClaudeResponse(response);
            
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
    
    private String parseClaudeResponse(String responseBody) {
        try {
            JsonObject root = gson.fromJson(responseBody, JsonObject.class);
            
            if (root.has("error")) {
                JsonObject error = root.getAsJsonObject("error");
                return "Error: " + error.get("message").getAsString();
            }
            
            if (root.has("content")) {
                return extractTextFromContent(root.getAsJsonArray("content"));
            }
            
            return "(no response)";
            
        } catch (Exception e) {
            return "Error parseando: " + e.getMessage();
        }
    }
    
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