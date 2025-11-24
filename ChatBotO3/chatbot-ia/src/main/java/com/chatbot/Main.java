package com.chatbot;

import com.chatbot.service.ConversationLogger;
import com.chatbot.ui.ChatUI;
import com.chatbot.ui.ConsoleUI;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // Inicializar logger de conversaciones (crea carpeta logs y archivo por sesión)
        try {
            ConversationLogger.getInstance();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                ConversationLogger.getInstance().close();
            }));
        } catch (Exception e) {
            System.err.println("No se pudo inicializar ConversationLogger: " + e.getMessage());
        }

        if (args.length > 0 && args[0].equals("--console")) {
            // Modo consola
            ConsoleUI console = new ConsoleUI();
            console.start();
        } else {
            // Modo interfaz gráfica (por defecto)
            SwingUtilities.invokeLater(() -> {
                ChatUI app = new ChatUI();
                app.setVisible(true);
            });
        }
    }
}