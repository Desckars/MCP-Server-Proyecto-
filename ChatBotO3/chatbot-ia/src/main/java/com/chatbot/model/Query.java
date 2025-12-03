package com.chatbot.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Modelo para representar una query de O3 exitosa
 * Almacena el prompt, respuesta y metadata para crear instrucciones del LLM
 */
public class Query {
    private String id;
    private String userPrompt;      // Prompt original del usuario
    private String mdxQuery;        // Query MDX generada
    private String queryResult;     // Resultado de ejecutar la query
    private LocalDateTime timestamp;
    private int successRating;      // 1 = Like, -1 = Dislike, 0 = no calificada
    private String notes;           // Notas del usuario (opcional)
    
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public Query() {
        this.timestamp = LocalDateTime.now();
        this.successRating = 0;
        this.notes = "";
    }
    
    public Query(String userPrompt, String mdxQuery, String queryResult) {
        this();
        this.id = System.nanoTime() + "";
        this.userPrompt = userPrompt;
        this.mdxQuery = mdxQuery;
        this.queryResult = queryResult;
    }
    
    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUserPrompt() { return userPrompt; }
    public void setUserPrompt(String userPrompt) { this.userPrompt = userPrompt; }
    
    public String getMdxQuery() { return mdxQuery; }
    public void setMdxQuery(String mdxQuery) { this.mdxQuery = mdxQuery; }
    
    public String getQueryResult() { return queryResult; }
    public void setQueryResult(String queryResult) { this.queryResult = queryResult; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getTimestampAsString() { 
        return timestamp != null ? timestamp.format(formatter) : "N/A"; 
    }
    
    public int getSuccessRating() { return successRating; }
    public void setSuccessRating(int successRating) { this.successRating = successRating; }
    
    public boolean isSuccessful() { return successRating == 1; }
    public void markAsSuccessful() { this.successRating = 1; }
    public void markAsFailed() { this.successRating = -1; }
    public void resetRating() { this.successRating = 0; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    /**
     * Retorna una representación legible de la instrucción para el LLM
     */
    public String toInstructionString() {
        StringBuilder sb = new StringBuilder();
        sb.append("User Intent: ").append(userPrompt).append("\n");
        sb.append("Valid MDX Query: ").append(mdxQuery).append("\n");
        if (notes != null && !notes.isEmpty()) {
            sb.append("Notes: ").append(notes).append("\n");
        }
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return "Query{" +
                "id='" + id + '\'' +
                ", userPrompt='" + userPrompt + '\'' +
                ", mdxQuery='" + mdxQuery + '\'' +
                ", timestamp=" + getTimestampAsString() +
                ", successRating=" + successRating +
                '}';
    }
}
