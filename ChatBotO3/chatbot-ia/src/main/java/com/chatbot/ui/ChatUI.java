package com.chatbot.ui;

import com.chatbot.service.ChatService;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import com.chatbot.config.ClaudeConfig;

public class ChatUI extends JFrame {
    private ChatService chatService;
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private JButton clearButton;
    private JButton configButton;
    private JLabel statusLabel;
    
    public ChatUI() {
        chatService = new ChatService();
        initializeUI();
        
        // Agregar shutdown hook
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                chatService.shutdown();
            }
        });
    }
    
    private void initializeUI() {
        setTitle("ChatBot IA - Claude Sonnet 4 + MCP O3");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Panel principal
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(new Color(240, 240, 245));
        
        // ========================================
        // PANEL SUPERIOR CON ESTADO Y CONFIG
        // ========================================
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(240, 240, 245));
        
        JLabel titleLabel = new JLabel("🤖 Claude AI + O3");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        topPanel.add(titleLabel, BorderLayout.WEST);
        
        // Panel derecho con estado y botón de configuración
        JPanel topRightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        topRightPanel.setBackground(new Color(240, 240, 245));
        
        statusLabel = new JLabel();
        updateStatusLabel();
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        topRightPanel.add(statusLabel);
        
        // Botón de configuración
        configButton = new JButton("⚙️");
        configButton.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        configButton.setPreferredSize(new Dimension(45, 30));
        configButton.setFocusPainted(false);
        configButton.setBorderPainted(false);
        configButton.setContentAreaFilled(false);
        configButton.setToolTipText("Configuración");
        configButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        configButton.addActionListener(e -> openConfigDialog());
        
        // Efecto hover
        configButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                configButton.setContentAreaFilled(true);
                configButton.setBackground(new Color(220, 220, 225));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                configButton.setContentAreaFilled(false);
            }
        });
        
        topRightPanel.add(configButton);
        topPanel.add(topRightPanel, BorderLayout.EAST);
        
        // ========================================
        // ÁREA DE CHAT
        // ========================================
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chatArea.setMargin(new Insets(10, 10, 10, 10));
        chatArea.setBackground(Color.WHITE);
        
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        
        // ========================================
        // PANEL INFERIOR
        // ========================================
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBackground(new Color(240, 240, 245));
        
        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        inputField.addActionListener(e -> sendMessage());
        
        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setBackground(new Color(240, 240, 245));
        
        clearButton = new JButton("Limpiar");
        clearButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        clearButton.setFocusPainted(false);
        clearButton.addActionListener(e -> clearChat());
        
        sendButton = new JButton("Enviar");
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sendButton.setBackground(new Color(120, 70, 255));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setBorderPainted(false);
        sendButton.setPreferredSize(new Dimension(90, 35));
        sendButton.addActionListener(e -> sendMessage());
        
        buttonPanel.add(clearButton);
        buttonPanel.add(sendButton);
        
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);
        
        // ========================================
        // AGREGAR COMPONENTES
        // ========================================
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        
        // Mensaje de bienvenida
        appendMessage("Claude", "¡Hola! Soy Claude Sonnet 4 de Anthropic. Puedo ayudarte con consultas generales " +
                "y también ejecutar consultas MDX sobre cubos O3. ¿En qué puedo ayudarte hoy?");
    }
    
    /**
     * Actualiza el label de estado con la info de AI y MCP
     */
    private void updateStatusLabel() {
        boolean aiActive = chatService.getAIService().isUsingAI();
        boolean mcpActive = chatService.getAIService().isMCPEnabled();
        
        StringBuilder status = new StringBuilder();
        
        if (aiActive) {
            status.append("🟢 Claude AI");
        } else {
            status.append("🔴 Sin API Key");
        }
        
        if (mcpActive) {
            status.append(" | 🟢 MCP O3");
        } else {
            status.append(" | ⚪ MCP O3");
        }
        
        statusLabel.setText(status.toString());
        
        // Cambiar color según estado
        if (aiActive) {
            statusLabel.setForeground(new Color(0, 150, 0));
        } else {
            statusLabel.setForeground(new Color(200, 0, 0));
        }
    }
    
    /**
     * Abre el diálogo de configuración
     */
    private void openConfigDialog() {
        ConfigSetupUI configDialog = new ConfigSetupUI(this);
        configDialog.setVisible(true);
        
        if (configDialog.isConfigSaved()) {
            // Recargar configuración
            ClaudeConfig.reload();
            
            // Reiniciar el servicio de chat con la nueva configuración
            chatService.shutdown();
            chatService = new ChatService();
            
            // Actualizar el estado visual
            updateStatusLabel();
            
            // Notificar al usuario
            appendMessage("SISTEMA", "✅ Configuración actualizada correctamente. " +
                "Los cambios se han aplicado. Puedes continuar usando el chatbot.");
            
            // Limpiar historial para evitar confusiones
            chatService.clearHistory();
        }
    }
    
    /**
     * Envía un mensaje al chatbot
     */
    private void sendMessage() {
        String message = inputField.getText().trim();
        
        if (message.isEmpty()) {
            return;
        }
        
        // Verificar si hay API Key configurado
        if (!chatService.getAIService().isUsingAI()) {
            appendMessage("SISTEMA", "⚠️ No hay API Key configurado. Haz clic en ⚙️ para configurarlo.");
            inputField.setText("");
            return;
        }
        
        appendMessage("Tú", message);
        inputField.setText("");
        
        inputField.setEnabled(false);
        sendButton.setEnabled(false);
        sendButton.setText("Enviando...");
        
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return chatService.sendMessage(message);
            }
            
            @Override
            protected void done() {
                try {
                    String response = get();
                    appendMessage("Claude", response);
                    updateStatusLabel(); // Actualizar estado por si MCP se activó
                } catch (Exception e) {
                    appendMessage("SISTEMA", "❌ Error: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    inputField.setEnabled(true);
                    sendButton.setEnabled(true);
                    sendButton.setText("Enviar");
                    inputField.requestFocus();
                }
            }
        };
        
        worker.execute();
    }
    
    /**
     * Agrega un mensaje al área de chat
     */
    private void appendMessage(String sender, String content) {
        String timestamp = java.time.LocalTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
        );
        
        // Formato especial para mensajes del sistema
        if (sender.equals("SISTEMA")) {
            chatArea.append(String.format("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"));
            chatArea.append(String.format("[%s] %s\n", timestamp, content));
            chatArea.append(String.format("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n"));
        } else {
            chatArea.append(String.format("[%s] %s:\n%s\n\n", timestamp, sender, content));
        }
        
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }
    
    /**
     * Limpia el chat
     */
    private void clearChat() {
        int option = JOptionPane.showConfirmDialog(
            this,
            "¿Estás seguro de que deseas limpiar el chat?\n" +
            "Se borrará todo el historial de conversación.",
            "Confirmar Limpieza",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (option == JOptionPane.YES_OPTION) {
            chatArea.setText("");
            chatService.clearHistory();
            appendMessage("Claude", "Conversación reiniciada. ¿En qué puedo ayudarte?");
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            ChatUI app = new ChatUI();
            app.setVisible(true);
        });
    }
}