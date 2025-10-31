package com.chatbot;
import com.chatbot.ui.ChatUI;
import com.chatbot.ui.ConsoleUI;
import javax.swing.SwingUtilities;
public class Main {
    public static void main(String[] args) {
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