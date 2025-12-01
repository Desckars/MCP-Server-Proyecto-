package com.chatbot.controller;

import com.chatbot.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para funcionalidades de MCP y IA avanzadas
 */
@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AIController {

    @Autowired
    private AIService aiService;

    /**
     * Lista las herramientas disponibles en MCP
     */
    @GetMapping("/tools")
    public ResponseEntity<Map<String, Object>> listTools() {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("tools", aiService.listMCPTools());
            result.put("mcpEnabled", aiService.isMCPEnabled());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Ejecuta una consulta MDX directa
     */
    @PostMapping("/execute-mdx")
    public ResponseEntity<Map<String, Object>> executeMDX(@RequestBody Map<String, String> request) {
        try {
            String mdxQuery = request.get("query");
            
            if (mdxQuery == null || mdxQuery.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "La consulta MDX no puede estar vacía");
                return ResponseEntity.badRequest().body(error);
            }

            String result = aiService.executeDirectMDX(mdxQuery);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("result", result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Obtiene información sobre el estado actual
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("ai_enabled", aiService.isUsingAI());
            status.put("mcp_enabled", aiService.isMCPEnabled());
            status.put("context_size", aiService.getContextSize());
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("status", status);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Limpia el contexto de Claude
     */
    @PostMapping("/clear-context")
    public ResponseEntity<Map<String, String>> clearContext() {
        try {
            aiService.clearContext();
            
            Map<String, String> result = new HashMap<>();
            result.put("success", "true");
            result.put("message", "Contexto de Claude limpiado");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("success", "false");
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
