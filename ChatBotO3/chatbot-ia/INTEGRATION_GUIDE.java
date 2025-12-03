import com.chatbot.service.QueryContextProvider;

/*
 * GUÍA DE INTEGRACIÓN EN ClaudeService.java
 * 
 * Para integrar el sistema de persistencia de queries en ClaudeService,
 * sigue estos pasos:
 */

// ========================================
// PASO 1: Agregar import al inicio del archivo
// ========================================
// Copiar esta línea al principio de ClaudeService.java:
// import com.chatbot.service.QueryContextProvider;


// ========================================
// PASO 2: Modificar el método buildSystemPrompt()
// ========================================
// Buscar el método buildSystemPrompt() y reemplazarlo o modificarlo así:

/*
    private String buildSystemPrompt() {
        // Obtener ejemplos de queries exitosas del historial
        String successfulExamples = QueryContextProvider.getInstance()
            .getSuccessfulQueriesContext();
        
        // Tu sistema prompt actual (inglés o español)
        String basePrompt = ... // [tu código actual]
        
        // Retornar combinado (agregar los ejemplos al final del prompt)
        return basePrompt + successfulExamples;
    }
*/


// ========================================
// PASO 3: Verificar que el método compile
// ========================================
// La integración es mínima y solo requiere:
// - Una línea de import
// - Dos líneas en buildSystemPrompt() para obtener el contexto
// - Concatenar el contexto al final del system prompt


// ========================================
// STEP 1: Add import at beginning of file
// ========================================
// Copy this line to the beginning of ClaudeService.java:
// import com.chatbot.service.QueryContextProvider;


// ========================================
// STEP 2: Modify buildSystemPrompt() method
// ========================================
// Find the buildSystemPrompt() method and modify it like this:

/*
    private String buildSystemPrompt() {
        // Get successful query examples from history
        String successfulExamples = QueryContextProvider.getInstance()
            .getSuccessfulQueriesContext();
        
        // Your current system prompt (English or Spanish)
        String basePrompt = ... // [your current code]
        
        // Return combined (add examples at the end of the prompt)
        return basePrompt + successfulExamples;
    }
*/


// ========================================
// STEP 3: Verify the method compiles
// ========================================
// The integration is minimal and only requires:
// - One import line
// - Two lines in buildSystemPrompt() to get the context
// - Concatenate the context to the end of the system prompt
