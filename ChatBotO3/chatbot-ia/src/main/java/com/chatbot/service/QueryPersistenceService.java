package com.chatbot.service;

import com.chatbot.model.Query;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de persistencia para queries exitosas.
 * Guarda y carga queries en formato JSON para reutilización como instrucciones del LLM.
 */
public class QueryPersistenceService {
    private static QueryPersistenceService instance;
    private static final String QUERIES_DIR = "data";
    private static final String QUERIES_FILE = "queries_data/successful_queries.json";
    private final Gson gson;
    private List<Query> queries;
    
    private QueryPersistenceService() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter())
                .create();
        this.queries = new ArrayList<>();
        ensureDirectoryExists();
        loadQueries();
    }
    
    public static synchronized QueryPersistenceService getInstance() {
        if (instance == null) {
            instance = new QueryPersistenceService();
        }
        return instance;
    }
    
    /**
     * Guarda una query exitosa en el archivo de persistencia
     */
    public synchronized void saveQuery(Query query) {
        if (query == null) return;
        
        // Si existe una query con el mismo ID, la reemplazamos
        queries.removeIf(q -> q.getId().equals(query.getId()));
        queries.add(query);
        
        persistToFile();
        System.out.println("✓ Query guardada: " + query.getId());
    }
    
    /**
     * Obtiene todas las queries exitosas (rating = 1)
     */
    public List<Query> getSuccessfulQueries() {
        return queries.stream()
                .filter(Query::isSuccessful)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtiene todas las queries (incluyendo las no calificadas)
     */
    public List<Query> getAllQueries() {
        return new ArrayList<>(queries);
    }
    
    /**
     * Obtiene una query por ID
     */
    public Optional<Query> getQueryById(String id) {
        return queries.stream()
                .filter(q -> q.getId().equals(id))
                .findFirst();
    }
    
    /**
     * Elimina una query por ID
     */
    public synchronized void deleteQuery(String id) {
        if (queries.removeIf(q -> q.getId().equals(id))) {
            persistToFile();
            System.out.println("✓ Query eliminada: " + id);
        }
    }
    
    /**
     * Actualiza el rating de una query existente
     */
    public synchronized void updateQueryRating(String queryId, int rating) {
        Optional<Query> optQuery = getQueryById(queryId);
        if (optQuery.isPresent()) {
            Query query = optQuery.get();
            query.setSuccessRating(rating);
            persistToFile();
            System.out.println("✓ Query actualizada - Rating: " + (rating == 1 ? "✓ Like" : rating == -1 ? "✗ Dislike" : "Sin calificar"));
        }
    }
    
    /**
     * Genera un texto con instrucciones basadas en queries exitosas
     * Para usar en el system prompt de Claude
     */
    public String generateInstructionsFromSuccessfulQueries() {
        List<Query> successful = getSuccessfulQueries();
        
        if (successful.isEmpty()) {
            return "";
        }
        
        StringBuilder instructions = new StringBuilder();
        instructions.append("\n\n=== SUCCESSFUL QUERY EXAMPLES (Reference) ===\n");
        instructions.append("Use these examples as reference for generating MDX queries:\n\n");
        
        int count = 1;
        for (Query q : successful) {
            instructions.append(count++).append(". ");
            instructions.append(q.toInstructionString());
            instructions.append("\n");
        }
        
        instructions.append("=== END OF EXAMPLES ===\n");
        return instructions.toString();
    }
    
    /**
     * Retorna estadísticas de las queries almacenadas
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total_queries", queries.size());
        stats.put("successful_queries", getSuccessfulQueries().size());
        stats.put("failed_queries", queries.stream().filter(q -> q.getSuccessRating() == -1).count());
        stats.put("unrated_queries", queries.stream().filter(q -> q.getSuccessRating() == 0).count());
        return stats;
    }
    
    /**
     * Exporta todas las queries a un archivo JSON
     */
    public void exportToFile(String filePath) {
        try {
            String json = gson.toJson(queries);
            Files.write(Paths.get(filePath), json.getBytes());
            System.out.println("✓ Queries exportadas a: " + filePath);
        } catch (IOException e) {
            System.err.println("Error exportando queries: " + e.getMessage());
        }
    }
    
    /**
     * Importa queries desde un archivo JSON
     */
    public void importFromFile(String filePath) {
        try {
            String json = Files.readString(Paths.get(filePath));
            List<Query> imported = gson.fromJson(json, new TypeToken<List<Query>>(){}.getType());
            if (imported != null) {
                queries.addAll(imported);
                persistToFile();
                System.out.println("✓ Queries importadas: " + imported.size());
            }
        } catch (IOException e) {
            System.err.println("Error importando queries: " + e.getMessage());
        }
    }
    
    // ========================================
    // MÉTODOS PRIVADOS
    // ========================================
    
    private void ensureDirectoryExists() {
        try {
            Path dir = Paths.get(QUERIES_DIR).resolve("queries_data");
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
        } catch (IOException e) {
            System.err.println("Error creando directorios: " + e.getMessage());
        }
    }
    
    private void loadQueries() {
        try {
            Path filePath = Paths.get(QUERIES_FILE);
            if (Files.exists(filePath)) {
                String json = Files.readString(filePath);
                List<Query> loaded = gson.fromJson(json, new TypeToken<List<Query>>(){}.getType());
                if (loaded != null) {
                    queries = loaded;
                    System.out.println("✓ Queries cargadas: " + queries.size());
                }
            }
        } catch (IOException e) {
            System.err.println("Error cargando queries: " + e.getMessage());
        }
    }
    
    private synchronized void persistToFile() {
        try {
            ensureDirectoryExists();
            Path filePath = Paths.get(QUERIES_FILE);
            String json = gson.toJson(queries);
            Files.write(filePath, json.getBytes());
        } catch (IOException e) {
            System.err.println("Error guardando queries: " + e.getMessage());
        }
    }
    
    /**
     * TypeAdapter personalizado para LocalDateTime
     */
    private static class LocalDateTimeTypeAdapter 
            extends com.google.gson.TypeAdapter<LocalDateTime> {
        
        @Override
        public void write(com.google.gson.stream.JsonWriter out, LocalDateTime value) 
                throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.toString());
            }
        }
        
        @Override
        public LocalDateTime read(com.google.gson.stream.JsonReader in) throws IOException {
            String value = in.nextString();
            return value == null ? null : LocalDateTime.parse(value);
        }
    }
}
