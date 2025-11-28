package com.chatbot.ui;

import com.chatbot.service.QueryPersistenceService;
import com.chatbot.model.Query;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Panel personalizado para mostrar mensajes de Claude con botones de Like/Dislike
 * Solo se muestra para respuestas de Claude que podrían contener queries MDX
 */
public class MessagePanel extends JPanel {
    private String sender;
    private String content;
    private boolean isClaudeResponse;
    private String userPrompt;
    private JPanel actionPanel;
    
    public MessagePanel(String sender, String content, String userPrompt) {
        this.sender = sender;
        this.content = content;
        this.userPrompt = userPrompt;
        this.isClaudeResponse = sender.equals("Claude");
        
        initializeUI();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        
        if (sender.equals("SISTEMA")) {
            setBackground(new Color(255, 240, 245));
        } else if (isClaudeResponse) {
            setBackground(new Color(230, 245, 255));
        } else {
            setBackground(new Color(245, 250, 255));
        }
        
        setOpaque(true);
        
        // Panel de contenido
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(this.getBackground());
        
        // Encabezado con timestamp y sender
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        headerPanel.setBackground(this.getBackground());
        
        String timestamp = java.time.LocalTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
        );
        
        JLabel headerLabel = new JLabel("[" + timestamp + "] " + sender + ":");
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        if (isClaudeResponse) {
            headerLabel.setForeground(new Color(0, 102, 204));
        } else if (sender.equals("Tú")) {
            headerLabel.setForeground(new Color(0, 150, 0));
        }
        headerPanel.add(headerLabel);
        
        contentPanel.add(headerPanel);
        
        // Área de texto con contenido
        JTextArea textArea = new JTextArea(content);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        textArea.setMargin(new Insets(5, 5, 5, 5));
        textArea.setBackground(this.getBackground());
        textArea.setBorder(BorderFactory.createEmptyBorder());
        
        contentPanel.add(textArea);
        
        add(contentPanel, BorderLayout.CENTER);
        
        // Panel de acciones (Like/Dislike) - solo para respuestas de Claude
        if (isClaudeResponse) {
            actionPanel = createActionPanel();
            add(actionPanel, BorderLayout.SOUTH);
        }
    }
    
    private JPanel createActionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        panel.setBackground(this.getBackground());
        
        JButton likeButton = new JButton("👍 Útil");
        likeButton.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        likeButton.setFocusPainted(false);
        likeButton.setBackground(new Color(76, 175, 80));
        likeButton.setForeground(Color.WHITE);
        
        JButton dislikeButton = new JButton("👎 Inútil");
        dislikeButton.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        dislikeButton.setFocusPainted(false);
        dislikeButton.setBackground(new Color(244, 67, 54));
        dislikeButton.setForeground(Color.WHITE);
        
        JButton detailsButton = new JButton("📋 Ver Detalles");
        detailsButton.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        detailsButton.setFocusPainted(false);
        detailsButton.setBackground(new Color(33, 150, 243));
        detailsButton.setForeground(Color.WHITE);
        
        // Acción para Like
        likeButton.addActionListener(e -> {
            saveQueryAsSuccessful();
            disableActionButtons(panel);
            JOptionPane.showMessageDialog(this, 
                "✓ Query guardada como exitosa.\n" +
                "Será usada como referencia para futuras consultas.",
                "Query Guardada", 
                JOptionPane.INFORMATION_MESSAGE);
        });
        
        // Acción para Dislike
        dislikeButton.addActionListener(e -> {
            saveQueryAsFailed();
            disableActionButtons(panel);
            JOptionPane.showMessageDialog(this, 
                "✗ Query marcada como fallida.\n" +
                "Se registrará para análisis.",
                "Query Marcada", 
                JOptionPane.INFORMATION_MESSAGE);
        });
        
        // Acción para Ver Detalles
        detailsButton.addActionListener(e -> {
            showQueryDetails();
        });
        
        panel.add(likeButton);
        panel.add(dislikeButton);
        panel.add(detailsButton);
        
        return panel;
    }
    
    private void saveQueryAsSuccessful() {
        QueryPersistenceService service = QueryPersistenceService.getInstance();
        Query query = new Query(userPrompt, content, content);
        query.markAsSuccessful();
        query.setNotes("Validated as successful by user");
        service.saveQuery(query);
    }
    
    private void saveQueryAsFailed() {
        QueryPersistenceService service = QueryPersistenceService.getInstance();
        Query query = new Query(userPrompt, content, content);
        query.markAsFailed();
        query.setNotes("Marked as failed by user");
        service.saveQuery(query);
    }
    
    private void showQueryDetails() {
        QueryPersistenceService service = QueryPersistenceService.getInstance();
        StringBuilder details = new StringBuilder();
        details.append("Prompt del Usuario:\n").append(userPrompt).append("\n\n");
        details.append("Respuesta de Claude:\n").append(content).append("\n\n");
        details.append("Estadísticas:\n");
        var stats = service.getStatistics();
        details.append("- Total de queries guardadas: ").append(stats.get("total_queries")).append("\n");
        details.append("- Exitosas: ").append(stats.get("successful_queries")).append("\n");
        details.append("- Fallidas: ").append(stats.get("failed_queries")).append("\n");
        
        JTextArea textArea = new JTextArea(details.toString());
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 300));
        
        JOptionPane.showMessageDialog(this, scrollPane, 
            "Detalles de la Query", 
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void disableActionButtons(JPanel panel) {
        for (Component comp : panel.getComponents()) {
            if (comp instanceof JButton) {
                comp.setEnabled(false);
            }
        }
    }
    
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(super.getPreferredSize().width, 
                           Math.max(80, super.getPreferredSize().height));
    }
}
