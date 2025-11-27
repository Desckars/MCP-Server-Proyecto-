package com.chatbot;

import com.chatbot.config.ClaudeConfig;
import com.chatbot.service.ConversationLogger;
import com.chatbot.ui.ChatUI;
import com.chatbot.ui.ConfigSetupUI;
import com.chatbot.ui.ConsoleUI;
import javax.swing.JOptionPane;
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
            startConsoleMode();
        } else {
            // Modo interfaz gráfica (por defecto)
            startGUIMode();
        }
    }
    
    private static void startGUIMode() {
        SwingUtilities.invokeLater(() -> {
            // ========================================
            // VERIFICAR CONFIGURACIÓN
            // ========================================
            ClaudeConfig config = ClaudeConfig.getInstance();
            
            if (!config.isConfigured()) {
                System.out.println("⚙️ Primera configuración necesaria");
                System.out.println("   Abriendo ventana de configuración...\n");
                
                // Mostrar ventana de configuración
                ConfigSetupUI configDialog = new ConfigSetupUI(null);
                configDialog.setVisible(true);
                
                // Si el usuario canceló, salir
                if (!configDialog.isConfigSaved()) {
                    System.out.println("❌ Configuración cancelada. Saliendo...");
                    JOptionPane.showMessageDialog(
                        null,
                        "Configuración cancelada.\nNo se puede iniciar el chatbot sin API Key.",
                        "Configuración Requerida",
                        JOptionPane.WARNING_MESSAGE
                    );
                    System.exit(0);
                    return;
                }
                
                // Recargar configuración después de guardar
                System.out.println("✅ Configuración guardada. Recargando...\n");
                // Forzar nueva instancia para recargar desde archivo
                config = ClaudeConfig.getInstance();
            }
            
            // Verificar que ahora sí esté configurado
            if (!config.isConfigured()) {
                System.err.println("❌ Error: Configuración inválida después de guardar");
                JOptionPane.showMessageDialog(
                    null,
                    "Error en la configuración guardada.\n\nPor favor, verifica:\n" +
                    "1. Que el API Key sea válido (sk-ant-api03-...)\n" +
                    "2. Que el archivo config.properties tenga permisos de escritura\n\n" +
                    "Ruta: src/main/resources/config.properties",
                    "Error de Configuración",
                    JOptionPane.ERROR_MESSAGE
                );
                System.exit(1);
                return;
            }
            
            // ========================================
            // INICIAR CHATBOT
            // ========================================
            System.out.println("✅ Configuración válida. Iniciando ChatBot IA...\n");
            
            ChatUI app = new ChatUI();
            app.setVisible(true);
        });
    }
    
    private static void startConsoleMode() {
        System.out.println("========================================");
        System.out.println("  CHATBOT IA - MODO CONSOLA");
        System.out.println("========================================\n");
        
        // Verificar configuración
        ClaudeConfig config = ClaudeConfig.getInstance();
        
        if (!config.isConfigured()) {
            System.err.println("❌ ERROR: API Key no configurado\n");
            System.out.println("Para configurar tu API Key:");
            System.out.println();
            System.out.println("OPCIÓN 1 (Recomendada - Interfaz Gráfica):");
            System.out.println("  1. Ejecuta el chatbot sin el parámetro --console");
            System.out.println("  2. Se abrirá una ventana de configuración");
            System.out.println("  3. Ingresa tu API Key y guarda");
            System.out.println("  4. Luego podrás usar el modo consola");
            System.out.println();
            System.out.println("OPCIÓN 2 (Manual):");
            System.out.println("  1. Edita: src/main/resources/config.properties");
            System.out.println("  2. Busca la línea: anthropic.api-key=");
            System.out.println("  3. Pega tu API Key: anthropic.api-key=sk-ant-api03-...");
            System.out.println("  4. Guarda el archivo");
            System.out.println("  5. Al ejecutar de nuevo, se encriptará automáticamente");
            System.out.println();
            System.out.println("========================================\n");
            System.exit(1);
            return;
        }
        
        System.out.println("✅ Configuración válida");
        System.out.println("   Modelo: " + config.getModel());
        System.out.println("   Max Tokens: " + config.getMaxTokens());
        System.out.println();
        
        // Iniciar modo consola
        ConsoleUI console = new ConsoleUI();
        console.start();
    }
}