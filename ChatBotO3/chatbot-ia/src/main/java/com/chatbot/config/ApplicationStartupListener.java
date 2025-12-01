package com.chatbot.config;

import com.chatbot.service.ConversationLogger;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listener para eventos de ciclo de vida de la aplicación
 * Maneja la inicialización y cierre adecuado de recursos
 */
@Component
public class ApplicationStartupListener {

    /**
     * Se ejecuta cuando la aplicación ha iniciado completamente
     */
    @EventListener(ApplicationStartedEvent.class)
    public void onApplicationStarted() {
        System.out.println("\n========================================");
        System.out.println("  CHATBOT IA - SPRING BOOT");
        System.out.println("  v2.0");
        System.out.println("========================================");
        System.out.println("✅ Aplicación iniciada correctamente");
        System.out.println("🌐 Accede a: http://localhost:8080");
        System.out.println("📡 API REST: http://localhost:8080/api/chat");
        System.out.println("========================================\n");

        // Inicializar logger de conversaciones
        try {
            ConversationLogger.getInstance();
            System.out.println("✓ Sistema de logging de conversaciones activo");
        } catch (Exception e) {
            System.err.println("⚠️  Error inicializando logger: " + e.getMessage());
        }

        // Registrar shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n========================================");
            System.out.println("  Cerrando aplicación...");
            System.out.println("========================================");
            try {
                ConversationLogger.getInstance().close();
                System.out.println("✓ Recursos liberados correctamente");
            } catch (Exception e) {
                System.err.println("⚠️  Error cerrando recursos: " + e.getMessage());
            }
        }));
    }
}
