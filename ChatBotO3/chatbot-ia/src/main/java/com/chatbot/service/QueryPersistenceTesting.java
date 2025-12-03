package com.chatbot.service;

import com.chatbot.model.Query;

/**
 * Clase de utilidad para testing del sistema de persistencia de queries.
 * Proporciona métodos para crear queries de prueba y validar el sistema.
 */
public class QueryPersistenceTesting {
    
    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("TESTING: Query Persistence System");
        System.out.println("=".repeat(60));
        
        QueryPersistenceService service = QueryPersistenceService.getInstance();
        
        // Test 1: Crear queries exitosas
        System.out.println("\n✓ TEST 1: Crear queries exitosas");
        createTestQueries(service);
        
        // Test 2: Obtener estadísticas
        System.out.println("\n✓ TEST 2: Estadísticas del sistema");
        printStatistics(service);
        
        // Test 3: Generar instrucciones para Claude
        System.out.println("\n✓ TEST 3: Generar contexto para Claude");
        String instructions = service.generateInstructionsFromSuccessfulQueries();
        if (instructions.isEmpty()) {
            System.out.println("⚠️  No hay queries exitosas aún.");
        } else {
            System.out.println("Instrucciones generadas (" + instructions.length() + " caracteres):");
            System.out.println(instructions);
        }
        
        // Test 4: Listar queries
        System.out.println("\n✓ TEST 4: Listar todas las queries");
        for (Query q : service.getAllQueries()) {
            System.out.println("- [" + (q.isSuccessful() ? "✓" : "✗") + "] " + 
                             q.getUserPrompt().substring(0, Math.min(50, q.getUserPrompt().length())));
        }
        
        // Test 5: Exportar (opcional)
        System.out.println("\n✓ TEST 5: Sistema listo para usar");
        System.out.println("Archivo de persistencia: data/queries_data/successful_queries.json");
        System.out.println("Estadísticas: " + service.getStatistics());
    }
    
    private static void createTestQueries(QueryPersistenceService service) {
        // Query 1: Exitosa
        Query q1 = new Query(
            "Muéstrame todas las unidades vendidas por cliente mayor",
            "SELECT {Measures.[Units Sold]} ON COLUMNS, {Customers.Customers.[Major Accounts]} ON ROWS FROM Demo",
            "Resultados: Cliente A: 1000 units, Cliente B: 800 units..."
        );
        q1.markAsSuccessful();
        q1.setNotes("Consulta básica con clientes mayores");
        service.saveQuery(q1);
        System.out.println("  ✓ Query 1 guardada (exitosa)");
        
        // Query 2: Exitosa
        Query q2 = new Query(
            "Obtén el costo total de productos por localidad",
            "SELECT NON EMPTY {Location.children} ON ROWS, {Measures.[Cost]} ON COLUMNS FROM Demo WHERE Measures.[Units Sold]",
            "Resultados: NY: $50000, LA: $35000..."
        );
        q2.markAsSuccessful();
        q2.setNotes("Query con filtros por medidas");
        service.saveQuery(q2);
        System.out.println("  ✓ Query 2 guardada (exitosa)");
        
        // Query 3: Fallida
        Query q3 = new Query(
            "Dame medidas inexistentes",
            "SELECT {Measures.[Nonexistent]} ON COLUMNS FROM Demo",
            "Error: Measure not found"
        );
        q3.markAsFailed();
        q3.setNotes("Error: medida no existe");
        service.saveQuery(q3);
        System.out.println("  ✓ Query 3 guardada (fallida - para análisis)");
    }
    
    private static void printStatistics(QueryPersistenceService service) {
        var stats = service.getStatistics();
        System.out.println("  Total de queries: " + stats.get("total_queries"));
        System.out.println("  Exitosas: " + stats.get("successful_queries"));
        System.out.println("  Fallidas: " + stats.get("failed_queries"));
        System.out.println("  Sin calificar: " + stats.get("unrated_queries"));
    }
}
