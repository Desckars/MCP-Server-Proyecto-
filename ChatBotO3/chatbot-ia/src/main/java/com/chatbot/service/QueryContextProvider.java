package com.chatbot.service;

import com.chatbot.model.Query;

/**
 * Servicio para integrar queries exitosas en el contexto de Claude.
 * Actúa como intermediario entre QueryPersistenceService y ClaudeService.
 */
public class QueryContextProvider {
    private static QueryContextProvider instance;
    private QueryPersistenceService persistenceService;
    
    private QueryContextProvider() {
        this.persistenceService = QueryPersistenceService.getInstance();
    }
    
    public static synchronized QueryContextProvider getInstance() {
        if (instance == null) {
            instance = new QueryContextProvider();
        }
        return instance;
    }
    
    /**
     * Obtiene el texto de instrucciones dinámicas para incluir en el system prompt
     */
    public String getSuccessfulQueriesContext() {
        return persistenceService.generateInstructionsFromSuccessfulQueries();
    }
    
    /**
     * Registra una query como exitosa basada en feedback del usuario
     */
    public void registerSuccessfulQuery(String userPrompt, String claudeResponse) {
        Query query = new Query(userPrompt, claudeResponse, claudeResponse);
        query.markAsSuccessful();
        query.setNotes("Auto-registered from user validation");
        persistenceService.saveQuery(query);
    }
    
    /**
     * Registra una query como fallida basada en feedback del usuario
     */
    public void registerFailedQuery(String userPrompt, String claudeResponse) {
        Query query = new Query(userPrompt, claudeResponse, claudeResponse);
        query.markAsFailed();
        query.setNotes("Auto-registered from user validation");
        persistenceService.saveQuery(query);
    }
    
    /**
     * Obtiene estadísticas de las queries
     */
    public String getStatistics() {
        var stats = persistenceService.getStatistics();
        return String.format("Total: %d | Exitosas: %d | Fallidas: %d | Sin calificar: %d",
            stats.get("total_queries"),
            stats.get("successful_queries"),
            stats.get("failed_queries"),
            stats.get("unrated_queries"));
    }
}
