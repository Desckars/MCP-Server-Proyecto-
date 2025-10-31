package com.chatbot.ui;

import com.chatbot.service.ChatService;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

public class ChatUI extends JFrame {
    private ChatService chatService;
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private JButton clearButton;
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
        
        // Panel superior con estado
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(240, 240, 245));
        
        JLabel titleLabel = new JLabel("🤖 Claude AI + Oracle O3");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        topPanel.add(titleLabel, BorderLayout.WEST);
        
        statusLabel = new JLabel();
        updateStatusLabel();
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        topPanel.add(statusLabel, BorderLayout.EAST);
        
        // Área de chat
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
        
        // Panel inferior
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
        
        // Agregar componentes
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        
        // Mensaje de bienvenida
        appendMessage("Claude", "¡Hola! Soy Claude Sonnet 4 de Anthropic. Puedo ayudarte con consultas generales " +
                "y también ejecutar consultas MDX sobre cubos Oracle O3. ¿En qué puedo ayudarte hoy?");
    }
    
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
        statusLabel.setForeground(aiActive ? new Color(0, 128, 0) : new Color(200, 0, 0));
    }
    
    private void sendMessage() {
        String message = inputField.getText().trim();
        
        if (message.isEmpty()) {
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
                    appendMessage("SISTEMA", "Error: " + e.getMessage());
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
    
    private void appendMessage(String sender, String content) {
        String timestamp = java.time.LocalTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
        );
        chatArea.append(String.format("[%s] %s:\n%s\n\n", timestamp, sender, content));
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }
    
    private void clearChat() {
        int option = JOptionPane.showConfirmDialog(
            this,
            "¿Estás seguro de que deseas limpiar el chat?",
            "Confirmar",
            JOptionPane.YES_NO_OPTION
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
