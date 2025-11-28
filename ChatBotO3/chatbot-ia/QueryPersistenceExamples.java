// ========================================
// QueryPersistenceService - API Reference
// ========================================

// EJEMPLOS DE USO

import com.chatbot.service.QueryPersistenceService;
import com.chatbot.model.Query;
import java.util.List;
import java.util.Map;

public class QueryPersistenceExamples {

    /**
     * EJEMPLO 1: Obtener la instancia del servicio
     */
    public static void example1_GetInstance() {
        // El servicio es Singleton - acceso único y global
        QueryPersistenceService service = QueryPersistenceService.getInstance();
        
        // Automáticamente:
        // - Crea el directorio data/queries_data si no existe
        // - Carga queries previas del JSON
        // - Inicializa GSON para serialización
    }

    /**
     * EJEMPLO 2: Guardar una query exitosa
     */
    public static void example2_SaveSuccessfulQuery() {
        QueryPersistenceService service = QueryPersistenceService.getInstance();
        
        // Crear una query
        Query query = new Query(
            "Muestra ventas por región",                    // userPrompt
            "SELECT {[Measures].[Sales]} ON COLUMNS, " +
            "{[Region].Members} ON ROWS FROM [Demo]",      // mdxQuery
            "Resultado exitoso con 5 regiones"             // queryResult
        );
        
        // Marcar como exitosa
        query.markAsSuccessful();
        query.setNotes("Usuario validó como correcta");
        
        // Guardar
        service.saveQuery(query);
        // Resultado: Se guarda en data/queries_data/successful_queries.json
    }

    /**
     * EJEMPLO 3: Guardar una query fallida
     */
    public static void example3_SaveFailedQuery() {
        QueryPersistenceService service = QueryPersistenceService.getInstance();
        
        Query query = new Query(
            "Muestra vendedores por producto",
            "SELECT {[Salesmen].Members} ON COLUMNS, " +
            "{[Products].Members} ON ROWS",  // Falta FROM y medidas
            "Error: Cube not found"
        );
        
        // Marcar como fallida
        query.markAsFailed();
        query.setNotes("Syntax error - falta cube");
        
        // Guardar para análisis
        service.saveQuery(query);
    }

    /**
     * EJEMPLO 4: Obtener todas las queries exitosas
     */
    public static void example4_GetSuccessfulQueries() {
        QueryPersistenceService service = QueryPersistenceService.getInstance();
        
        List<Query> successful = service.getSuccessfulQueries();
        
        System.out.println("Total queries exitosas: " + successful.size());
        for (Query q : successful) {
            System.out.println("  - " + q.getUserPrompt());
            System.out.println("    MDX: " + q.getMdxQuery());
            System.out.println("    Guardada: " + q.getTimestampAsString());
            System.out.println();
        }
    }

    /**
     * EJEMPLO 5: Obtener instrucciones para el LLM
     */
    public static void example5_GetInstructionsForLLM() {
        QueryPersistenceService service = QueryPersistenceService.getInstance();
        
        // Esto genera un string formateado para incluir en el system prompt
        String instructions = service.generateInstructionsFromSuccessfulQueries();
        
        // Usar en ClaudeService.buildSystemPrompt():
        // String systemPrompt = basePrompt + instructions;
        
        System.out.println(instructions);
        /*
        Output:
        
        === SUCCESSFUL QUERY EXAMPLES (Reference) ===
        Use these examples as reference for generating MDX queries:

        1. User Intent: Muestra ventas por región
        Valid MDX Query: SELECT {[Measures].[Sales]} ON COLUMNS, {[Region].Members} ON ROWS FROM [Demo]
        Notes: Usuario validó como correcta

        2. User Intent: Compara medidas por cliente
        Valid MDX Query: SELECT {[Measures].[Sales], [Measures].[Cost]} ON COLUMNS, {[Customers].Members} ON ROWS FROM [Demo]
        
        === END OF EXAMPLES ===
        */
    }

    /**
     * EJEMPLO 6: Obtener estadísticas
     */
    public static void example6_GetStatistics() {
        QueryPersistenceService service = QueryPersistenceService.getInstance();
        
        Map<String, Object> stats = service.getStatistics();
        
        System.out.println("=== STATISTICS ===");
        System.out.println("Total queries: " + stats.get("total_queries"));
        System.out.println("Exitosas: " + stats.get("successful_queries"));
        System.out.println("Fallidas: " + stats.get("failed_queries"));
        System.out.println("Sin calificar: " + stats.get("unrated_queries"));
    }

    /**
     * EJEMPLO 7: Actualizar rating de una query
     */
    public static void example7_UpdateRating() {
        QueryPersistenceService service = QueryPersistenceService.getInstance();
        
        // Si el usuario clickea en Like/Dislike después
        String queryId = "1732747421929";
        
        // Marcar como exitosa
        service.updateQueryRating(queryId, 1);  // 1 = Like
        
        // O marcar como fallida
        // service.updateQueryRating(queryId, -1);  // -1 = Dislike
    }

    /**
     * EJEMPLO 8: Obtener una query específica
     */
    public static void example8_GetSpecificQuery() {
        QueryPersistenceService service = QueryPersistenceService.getInstance();
        
        java.util.Optional<Query> q = service.getQueryById("1732747421929");
        
        if (q.isPresent()) {
            System.out.println("Query encontrada:");
            System.out.println("  Prompt: " + q.get().getUserPrompt());
            System.out.println("  MDX: " + q.get().getMdxQuery());
            System.out.println("  Rating: " + q.get().getSuccessRating());
        } else {
            System.out.println("Query no encontrada");
        }
    }

    /**
     * EJEMPLO 9: Eliminar una query
     */
    public static void example9_DeleteQuery() {
        QueryPersistenceService service = QueryPersistenceService.getInstance();
        
        service.deleteQuery("1732747421929");
        // Se elimina de memoria y se persiste inmediatamente
    }

    /**
     * EJEMPLO 10: Exportar queries a archivo
     */
    public static void example10_ExportQueries() {
        QueryPersistenceService service = QueryPersistenceService.getInstance();
        
        // Generar backup o compartir con otro sistema
        service.exportToFile("backup_queries_2025-11-27.json");
    }

    /**
     * EJEMPLO 11: Importar queries desde archivo
     */
    public static void example11_ImportQueries() {
        QueryPersistenceService service = QueryPersistenceService.getInstance();
        
        // Importar queries de otro sistema o backup
        service.importFromFile("backup_queries_2025-11-27.json");
        
        // Las queries importadas se agregan a las existentes
    }

    /**
     * EJEMPLO 12: Integración en MessagePanel (UI)
     */
    public static void example12_UIIntegration() {
        // En MessagePanel.java - cuando el usuario clickea Like:
        
        QueryPersistenceService service = QueryPersistenceService.getInstance();
        
        // 1. Crear Query con info del mensaje
        Query query = new Query(userPrompt, claudeResponse, claudeResponse);
        query.markAsSuccessful();
        
        // 2. Guardar
        service.saveQuery(query);
        
        // 3. Deshabilitar botones
        disableActionButtons();
        
        // 4. Mostrar confirmación
        JOptionPane.showMessageDialog(this, 
            "✓ Query guardada como exitosa.\n" +
            "Será usada como referencia para futuras consultas.",
            "Query Guardada", 
            JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * EJEMPLO 13: Integración en ClaudeService
     */
    public static void example13_ClaudeIntegration() {
        // En ClaudeService.buildSystemPrompt():
        
        String basePrompt = """
            Eres un experto en MDX...
            """;
        
        // Agregar ejemplos exitosos dinámicamente
        String successExamples = QueryPersistenceService
            .getInstance()
            .generateInstructionsFromSuccessfulQueries();
        
        String finalSystemPrompt = basePrompt + successExamples;
        
        // Usar finalSystemPrompt en la request a Claude API
        return finalSystemPrompt;
    }

    /**
     * EJEMPLO 14: Flujo completo de una consulta
     */
    public static void example14_CompleteFlow() {
        QueryPersistenceService service = QueryPersistenceService.getInstance();
        
        // 1. Usuario escribe: "Muestra clientes principales"
        String userPrompt = "Muestra clientes principales";
        
        // 2. Claude genera query (con ayuda de ejemplos previos)
        String claudeResponse = "SELECT {[Customers].[Major Accounts]} ON ROWS...";
        
        // 3. MCP O3 ejecuta y retorna datos
        String result = "✓ Datos obtenidos exitosamente";
        
        // 4. UI muestra con botones Like/Dislike
        // Usuario clickea Like
        
        // 5. Guardar como exitosa
        Query query = new Query(userPrompt, claudeResponse, result);
        query.markAsSuccessful();
        service.saveQuery(query);
        
        // 6. Próxima consulta Claude recibe:
        String instructions = service.generateInstructionsFromSuccessfulQueries();
        // Incluye: "User Intent: Muestra clientes principales"
        //          "Valid MDX: SELECT {[Customers].[Major Accounts]} ON ROWS..."
        
        // 7. Claude usa esto para próximas queries similares
    }

    /**
     * EJEMPLO 15: Monitoreo y análisis
     */
    public static void example15_MonitoringAndAnalysis() {
        QueryPersistenceService service = QueryPersistenceService.getInstance();
        
        Map<String, Object> stats = service.getStatistics();
        
        long total = (long) stats.get("total_queries");
        long successful = (long) stats.get("successful_queries");
        
        double successRate = (total > 0) ? (successful * 100.0 / total) : 0;
        
        System.out.println("Success Rate: " + String.format("%.1f%%", successRate));
        
        // Si < 70%, revisar las queries fallidas
        if (successRate < 70) {
            System.out.println("⚠️  Baja tasa de éxito. Revisar ejemplos negativos.");
        }
    }
}

// ========================================
// NOTAS DE IMPLEMENTACIÓN
// ========================================

/*
 * INTEGRACIÓN EN DIFERENTES COMPONENTES:
 * 
 * 1. MessagePanel.java
 *    - Detecta click en botón Like/Dislike
 *    - Llama a saveQueryAsSuccessful() o saveQueryAsFailed()
 *    - Deshabilita botones después de calificar
 * 
 * 2. ChatUI.java
 *    - Rastrea lastUserPrompt (variable de instancia)
 *    - Pasa al MessagePanel para asociar con respuesta
 * 
 * 3. ClaudeService.java
 *    - buildSystemPrompt() incluye generateInstructionsFromSuccessfulQueries()
 *    - Las instrucciones se agregan al system prompt dinámicamente
 *    - Cada consulta beneficia de ejemplos previos
 * 
 * 4. AIService.java (sin cambios necesarios)
 *    - Usa ClaudeService internamente
 *    - El aprendizaje es transparente
 * 
 * UBICACIÓN DEL ARCHIVO JSON:
 * 
 * data/queries_data/successful_queries.json
 * 
 * Estructura:
 * [
 *   {
 *     "id": "1732747421929",
 *     "userPrompt": "Muestra ventas por región",
 *     "mdxQuery": "SELECT {[Measures].[Sales]} ON COLUMNS...",
 *     "queryResult": "Datos obtenidos",
 *     "timestamp": "2025-11-27T23:27:01.929",
 *     "successRating": 1,
 *     "notes": "Usuario validó como correcta"
 *   },
 *   ...
 * ]
 * 
 * THREAD SAFETY:
 * 
 * - QueryPersistenceService usa synchronized en métodos críticos
 * - Singleton garantiza una sola instancia en toda la JVM
 * - Operaciones de archivo son síncronas
 * 
 * PERFORMANCE:
 * 
 * - Queries se cargan una sola vez al iniciar
 * - Se cachean en memoria (List<Query>)
 * - Búsquedas son O(n) pero típicamente n < 100
 * - Escritura a disco es O(1) por query (actualiza todo el archivo)
 * 
 * EXTENSIONES FUTURAS:
 * 
 * 1. Base de datos en lugar de JSON (SQLite)
 * 2. Scoring automático basado en timestamps
 * 3. Versionado de queries
 * 4. Sincronización multi-usuario
 * 5. API REST para compartir queries
 */
