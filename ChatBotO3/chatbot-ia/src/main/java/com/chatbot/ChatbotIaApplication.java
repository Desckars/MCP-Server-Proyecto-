package com.chatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

import com.chatbot.Main;

/**
 * Aplicación principal de Spring Boot
 * ChatBot IA con Claude + MCP O3
 */
@SpringBootApplication
@EnableAsync
@ComponentScan(basePackages = {"com.chatbot"})
public class ChatbotIaApplication {

    public static void main(String[] args) {
        // Si se pasa el argumento --gui, lanzar la antigua interfaz Swing (com.chatbot.Main)
        for (String a : args) {
            if ("--gui".equalsIgnoreCase(a) || "--mode=gui".equalsIgnoreCase(a)) {
                // Delegar al Main antiguo que abre la UI Swing y maneja la configuración
                Main.main(args);
                return;
            }
        }

        SpringApplication.run(ChatbotIaApplication.class, args);
    }
}
