package com.chatbot.ui;

import com.chatbot.service.ChatService;
import com.chatbot.model.Message;
import java.util.Scanner;

public class ConsoleUI {
    private ChatService chatService;
    private Scanner scanner;
    private boolean running;
    
    public ConsoleUI() {
        this.chatService = new ChatService();
        this.scanner = new Scanner(System.in);
        this.running = true;
    }
    
    public void start() {
        printWelcome();
        
        while (running) {
            System.out.print("\n Tú: ");
            String userInput = scanner.nextLine().trim();
            
            if (userInput.isEmpty()) {
                continue;
            }
            
            // Comandos especiales
            if (handleCommand(userInput)) {
                continue;
            }
            
            // Enviar mensaje y obtener respuesta
            System.out.println("\n Claude está pensando...\n");
            String response = chatService.sendMessage(userInput);
            System.out.println(" Claude: " + response);
        }
        
        printGoodbye();
    }
    
    private boolean handleCommand(String input) {
        String lower = input.toLowerCase();
        
        switch (lower) {
            case "/salir":
            case "/exit":
            case "/quit":
                running = false;
                return true;
                
            case "/limpiar":
            case "/clear":
                chatService.clearHistory();
                System.out.println("\n [Historial y contexto limpiados]");
                return true;
                
            case "/historial":
            case "/history":
                printHistory();
                return true;
                
            case "/ayuda":
            case "/help":
                printHelp();
                return true;
                
            case "/status":
            case "/estado":
                printStatus();
                return true;
                
            case "/stats":
            case "/estadisticas":
                printStats();
                return true;
                
            case "/tools":
                printTools();
                return true;
                
            case "/contexto":
            case "/context":
                printContextInfo();
                return true;
                
            default:
                return false;
        }
    }
    
    private void printWelcome() {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║     CHATBOT IA - Claude Sonnet 4 + Oracle O3            ║");
        System.out.println("║     Versión 2.0 - Con Contexto y Multi-Query            ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println("\n Nuevas Características:");
        System.out.println("   Contexto conversacional (recuerda tu conversación)");
        System.out.println("   Reintentos automáticos en consultas MDX");
        System.out.println("   Claude decide cuándo usar herramientas");
        System.out.println("\n Escribe '/ayuda' para ver comandos disponibles");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
    
    private void printGoodbye() {
        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║          ¡Gracias por usar ChatBot IA!                  ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println(chatService.getConversationStats());
        chatService.shutdown();
    }
    
    private void printHelp() {
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println(" COMANDOS DISPONIBLES:");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("  /ayuda, /help          - Muestra esta ayuda");
        System.out.println("  /historial, /history   - Muestra el historial completo");
        System.out.println("  /limpiar, /clear       - Limpia historial y contexto");
        System.out.println("  /status, /estado       - Estado de conexiones");
        System.out.println("  /stats, /estadisticas  - Estadísticas de conversación");
        System.out.println("  /contexto, /context    - Info sobre contexto actual");
        System.out.println("  /tools                 - Lista herramientas MCP");
        System.out.println("  /salir, /exit          - Sale de la aplicación");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("\n EJEMPLOS DE USO:");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("  Conversación normal:");
        System.out.println("  → Hola, ¿cómo funciona MDX?");
        System.out.println("");
        System.out.println("  Consultas con contexto:");
        System.out.println("  → Dame ventas por ubicación");
        System.out.println("  → Ahora muéstrame solo Francia");
        System.out.println("  → ¿Y cuánto vendió España en comparación?");
        System.out.println("");
        System.out.println("  Claude reintenta automáticamente:");
        System.out.println("  → Muestra datos del cubo Wines");
        System.out.println("  Claude: (intenta, falla, reintenta con consulta corregida)");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
    }
    
    private void printHistory() {
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println(" HISTORIAL DE CONVERSACIÓN:");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        if (!chatService.hasActiveConversation()) {
            System.out.println("  (Vacío - aún no hay mensajes)");
        } else {
            int count = 1;
            for (Message msg : chatService.getConversationHistory()) {
                String sender = "USER".equals(msg.getSender()) ? " Tú" : " Claude";
                String content = msg.getContent();
                
                // Truncar si es muy largo
                if (content.length() > 200) {
                    content = content.substring(0, 200) + "...";
                }
                
                System.out.printf("\n[%d] %s [%s]:\n%s\n", 
                    count++, sender, msg.getFormattedTimestamp(), content);
            }
        }
        
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
    
    private void printStatus() {
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("ESTADO DE CONEXIONES:");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        boolean aiActive = chatService.getAIService().isUsingAI();
        boolean mcpActive = chatService.getAIService().isMCPEnabled();
        
        System.out.println("  Claude AI:       " + (aiActive ? " Activo" : " Inactivo"));
        System.out.println("  MCP O3:          " + (mcpActive ? " Conectado" : " Esperando uso"));
        System.out.println("  Contexto:         Activado");
        System.out.println("  Multi-Query:      Activado");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
    }
    
    private void printStats() {
        System.out.println("\n" + chatService.getConversationStats());
    }
    
    private void printContextInfo() {
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println(" INFORMACIÓN DEL CONTEXTO:");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("  Mensajes en contexto de Claude: " + 
                          chatService.getAIService().getContextSize());
        System.out.println("  Historial local: " + chatService.getMessageCount());
        System.out.println("\n ¿Qué es el contexto?");
        System.out.println("  Claude recuerda los últimos 10 mensajes de la conversación,");
        System.out.println("  lo que le permite:");
        System.out.println("  • Entender referencias como 'lo anterior' o 'ese dato'");
        System.out.println("  • Recordar consultas previas exitosas");
        System.out.println("  • Aprender de errores anteriores");
        System.out.println("  • Dar respuestas más coherentes");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
    }
    
    private void printTools() {
        System.out.println("\n Obteniendo herramientas disponibles...\n");
        String tools = chatService.getAIService().listMCPTools();
        System.out.println(tools);
    }
    
    public static void main(String[] args) {
        ConsoleUI ui = new ConsoleUI();
        ui.start();
    }
}