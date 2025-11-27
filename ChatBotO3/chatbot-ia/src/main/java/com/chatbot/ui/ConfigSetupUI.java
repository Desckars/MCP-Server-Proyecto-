package com.chatbot.ui;

import com.chatbot.config.ClaudeConfig;
import com.chatbot.security.EncryptionUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.util.Properties;

/**
 * Ventana de configuración inicial para gestionar el API Key
 * Diseño moderno y profesional
 */
public class ConfigSetupUI extends JDialog {
    
    private JPasswordField apiKeyField;
    private JComboBox<String> modelCombo;
    private JSpinner maxTokensSpinner;
    private JButton saveButton;
    private JButton cancelButton;
    private JButton showHideButton;
    private JLabel statusLabel;
    private JLabel infoLabel;
    
    private boolean configSaved = false;
    private boolean isFirstTime;
    
    // Colores modernos
    private static final Color PRIMARY_COLOR = new Color(120, 70, 255);
    private static final Color PRIMARY_HOVER = new Color(100, 50, 235);
    private static final Color BACKGROUND = new Color(250, 251, 252);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color TEXT_PRIMARY = new Color(30, 30, 30);
    private static final Color TEXT_SECONDARY = new Color(100, 100, 100);
    private static final Color BORDER_COLOR = new Color(230, 232, 236);
    private static final Color SUCCESS_COLOR = new Color(34, 197, 94);
    private static final Color ERROR_COLOR = new Color(239, 68, 68);
    
    public ConfigSetupUI(Frame parent) {
        super(parent, "Configuración de Claude AI", true);
        
        // Verificar si es primera vez
        this.isFirstTime = !hasExistingConfig();
        
        initComponents();
        loadCurrentConfig();
        
        // Si es primera vez, no permitir cancelar
        if (isFirstTime) {
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            
            addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    int option = JOptionPane.showConfirmDialog(
                        ConfigSetupUI.this,
                        "Necesitas configurar el API Key para usar el chatbot.\n\n¿Deseas salir de la aplicación?",
                        "Configuración Requerida",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                    );
                    if (option == JOptionPane.YES_OPTION) {
                        System.exit(0);
                    }
                }
            });
        }
    }
    
    private boolean hasExistingConfig() {
        try {
            File configFile = new File("src/main/resources/config.properties");
            if (!configFile.exists()) return false;
            
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
            }
            
            String encrypted = props.getProperty("anthropic.api-key.encrypted", "");
            return !encrypted.isEmpty();
            
        } catch (Exception e) {
            return false;
        }
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        setSize(700, isFirstTime ? 600 : 550);
        setLocationRelativeTo(getParent());
        setResizable(false);
        getContentPane().setBackground(BACKGROUND);
        
        // Panel principal con scroll
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(BACKGROUND);
        mainPanel.setBorder(new EmptyBorder(40, 50, 40, 50));
        
        // ========================================
        // ENCABEZADO CON ICONO
        // ========================================
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(BACKGROUND);
        headerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Icono grande
        JLabel iconLabel = new JLabel("🔐");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 64));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(iconLabel);
        headerPanel.add(Box.createVerticalStrut(20));
        
        // Título
        JLabel titleLabel = new JLabel(isFirstTime ? 
            "Configuración Inicial" : "Actualizar Configuración");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(10));
        
        // Subtítulo
        String infoText = isFirstTime ? 
            "Tu API Key será encriptado automáticamente y guardado de forma segura." :
            "Actualiza tu configuración. Los cambios se aplicarán inmediatamente.";
        
        infoLabel = new JLabel("<html><center>" + infoText + "</center></html>");
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        infoLabel.setForeground(TEXT_SECONDARY);
        infoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(infoLabel);
        
        mainPanel.add(headerPanel);
        mainPanel.add(Box.createVerticalStrut(40));
        
        // ========================================
        // TARJETA DE API KEY
        // ========================================
        JPanel apiKeyCard = createModernCard();
        
        JLabel apiKeyTitleLabel = new JLabel("🔑 API Key de Anthropic");
        apiKeyTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        apiKeyTitleLabel.setForeground(TEXT_PRIMARY);
        apiKeyCard.add(apiKeyTitleLabel);
        apiKeyCard.add(Box.createVerticalStrut(12));
        
        // Panel para el campo y el botón de mostrar/ocultar
        JPanel apiKeyInputPanel = new JPanel(new BorderLayout(10, 0));
        apiKeyInputPanel.setBackground(CARD_BG);
        
        apiKeyField = new JPasswordField();
        apiKeyField.setFont(new Font("Consolas", Font.PLAIN, 13));
        apiKeyField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        apiKeyField.setEchoChar('•');
        
        // Botón mostrar/ocultar con diseño moderno
        showHideButton = new JButton("👁");
        showHideButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        showHideButton.setPreferredSize(new Dimension(50, 44));
        showHideButton.setFocusPainted(false);
        showHideButton.setBorderPainted(false);
        showHideButton.setBackground(BACKGROUND);
        showHideButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        showHideButton.setToolTipText("Mostrar/Ocultar API Key");
        showHideButton.addActionListener(e -> toggleApiKeyVisibility());
        
        apiKeyInputPanel.add(apiKeyField, BorderLayout.CENTER);
        apiKeyInputPanel.add(showHideButton, BorderLayout.EAST);
        
        apiKeyCard.add(apiKeyInputPanel);
        apiKeyCard.add(Box.createVerticalStrut(8));
        
        JLabel hintLabel = new JLabel("Formato: sk-ant-api03-...");
        hintLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        hintLabel.setForeground(TEXT_SECONDARY);
        apiKeyCard.add(hintLabel);
        
        mainPanel.add(apiKeyCard);
        mainPanel.add(Box.createVerticalStrut(20));
        
        // ========================================
        // TARJETA DE MODELO
        // ========================================
        JPanel modelCard = createModernCard();
        
        JLabel modelTitleLabel = new JLabel("🤖 Modelo de Claude");
        modelTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        modelTitleLabel.setForeground(TEXT_PRIMARY);
        modelCard.add(modelTitleLabel);
        modelCard.add(Box.createVerticalStrut(12));
        
        String[] models = {
            "claude-sonnet-4-20250514",
            "claude-opus-4-20250514",
            "claude-haiku-4-20250514"
        };
        modelCombo = new JComboBox<>(models);
        modelCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        modelCombo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        modelCombo.setBackground(Color.WHITE);
        modelCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        
        modelCard.add(modelCombo);
        
        mainPanel.add(modelCard);
        mainPanel.add(Box.createVerticalStrut(20));
        
        // ========================================
        // TARJETA DE MAX TOKENS
        // ========================================
        JPanel tokensCard = createModernCard();
        
        JLabel tokensTitleLabel = new JLabel("📊 Max Tokens por Respuesta");
        tokensTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tokensTitleLabel.setForeground(TEXT_PRIMARY);
        tokensCard.add(tokensTitleLabel);
        tokensCard.add(Box.createVerticalStrut(12));
        
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(4096, 1024, 200000, 512);
        maxTokensSpinner = new JSpinner(spinnerModel);
        maxTokensSpinner.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        ((JSpinner.DefaultEditor) maxTokensSpinner.getEditor()).getTextField().setHorizontalAlignment(JTextField.LEFT);
        maxTokensSpinner.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        maxTokensSpinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        
        tokensCard.add(maxTokensSpinner);
        
        mainPanel.add(tokensCard);
        mainPanel.add(Box.createVerticalStrut(25));
        
        // ========================================
        // STATUS LABEL
        // ========================================
        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(statusLabel);
        mainPanel.add(Box.createVerticalStrut(10));
        
        // Scroll pane para el contenido
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
        
        // ========================================
        // PANEL DE BOTONES
        // ========================================
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 20));
        buttonPanel.setBackground(BACKGROUND);
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR));
        
        if (!isFirstTime) {
            cancelButton = new JButton("Cancelar");
            cancelButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            cancelButton.setPreferredSize(new Dimension(120, 42));
            cancelButton.setFocusPainted(false);
            cancelButton.setBorderPainted(false);
            cancelButton.setBackground(BACKGROUND);
            cancelButton.setForeground(TEXT_SECONDARY);
            cancelButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            cancelButton.addActionListener(e -> {
                configSaved = false;
                dispose();
            });
            
            // Hover effect
            cancelButton.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    cancelButton.setBackground(BORDER_COLOR);
                }
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    cancelButton.setBackground(BACKGROUND);
                }
            });
            
            buttonPanel.add(cancelButton);
        }
        
        saveButton = new JButton(isFirstTime ? "💾 Guardar y Comenzar" : "💾 Guardar Cambios");
        saveButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        saveButton.setPreferredSize(new Dimension(isFirstTime ? 200 : 180, 42));
        saveButton.setBackground(PRIMARY_COLOR);
        saveButton.setForeground(Color.WHITE);
        saveButton.setFocusPainted(false);
        saveButton.setBorderPainted(false);
        saveButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveButton.addActionListener(e -> saveConfiguration());
        
        // Hover effect
        saveButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (saveButton.isEnabled()) {
                    saveButton.setBackground(PRIMARY_HOVER);
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (saveButton.isEnabled()) {
                    saveButton.setBackground(PRIMARY_COLOR);
                }
            }
        });
        
        buttonPanel.add(saveButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Enter para guardar
        apiKeyField.addActionListener(e -> saveConfiguration());
    }
    
    /**
     * Crea una tarjeta moderna con sombra
     */
    private JPanel createModernCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(20, 20, 20, 20)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        
        return card;
    }
    
    private void toggleApiKeyVisibility() {
        if (apiKeyField.getEchoChar() == (char) 0) {
            apiKeyField.setEchoChar('•');
            showHideButton.setText("👁");
        } else {
            apiKeyField.setEchoChar((char) 0);
            showHideButton.setText("🙈");
        }
    }
    
    private void loadCurrentConfig() {
        try {
            File configFile = new File("src/main/resources/config.properties");
            if (!configFile.exists()) {
                return;
            }
            
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
            }
            
            // Si ya existe API Key encriptado, desencriptarlo y mostrarlo
            String encrypted = props.getProperty("anthropic.api-key.encrypted", "");
            if (!encrypted.isEmpty()) {
                try {
                    String masterKey = generateMasterKey();
                    String decrypted = EncryptionUtil.decrypt(encrypted, masterKey);
                    apiKeyField.setText(decrypted);
                } catch (Exception e) {
                    apiKeyField.setText("");
                    showStatus("⚠️ No se pudo cargar el API Key existente", ERROR_COLOR);
                }
            }
            
            // Cargar modelo
            String model = props.getProperty("anthropic.model", "claude-sonnet-4-20250514");
            modelCombo.setSelectedItem(model);
            
            // Cargar max tokens
            int maxTokens = Integer.parseInt(props.getProperty("anthropic.max-tokens", "4096"));
            maxTokensSpinner.setValue(maxTokens);
            
        } catch (Exception e) {
            System.err.println("Error cargando configuración: " + e.getMessage());
        }
    }
    
    private void saveConfiguration() {
        char[] apiKeyChars = apiKeyField.getPassword();
        String apiKey = new String(apiKeyChars).trim();
        
        // Limpiar el array de chars por seguridad
        java.util.Arrays.fill(apiKeyChars, '0');
        
        // Validar API Key
        if (apiKey.isEmpty()) {
            showStatus("❌ Debes ingresar un API Key", ERROR_COLOR);
            apiKeyField.requestFocus();
            return;
        }
        
        // Validar formato
        if (!apiKey.startsWith("sk-ant-api03-")) {
            showStatus("❌ Formato inválido. Debe comenzar con: sk-ant-api03-", ERROR_COLOR);
            apiKeyField.requestFocus();
            return;
        }
        
        if (apiKey.length() < 30) {
            showStatus("❌ API Key demasiado corto. Verifica que esté completo", ERROR_COLOR);
            apiKeyField.requestFocus();
            return;
        }
        
        // Deshabilitar controles mientras guarda
        setControlsEnabled(false);
        saveButton.setText("🔐 Encriptando...");
        showStatus("🔐 Encriptando y guardando configuración...", PRIMARY_COLOR);
        
        // Guardar en un worker para no bloquear la UI
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            String errorMessage = null;
            
            @Override
            protected Boolean doInBackground() throws Exception {
                try {
                    // Encriptar API Key
                    String masterKey = generateMasterKey();
                    String encryptedKey = EncryptionUtil.encrypt(apiKey, masterKey);
                    
                    // Cargar properties existente
                    File configFile = new File("src/main/resources/config.properties");
                    Properties props = new Properties();
                    
                    if (configFile.exists()) {
                        try (FileInputStream fis = new FileInputStream(configFile)) {
                            props.load(fis);
                        }
                    }
                    
                    // Actualizar valores
                    props.setProperty("anthropic.api-key.encrypted", encryptedKey);
                    props.remove("anthropic.api-key"); // Remover texto plano si existe
                    props.setProperty("anthropic.model", (String) modelCombo.getSelectedItem());
                    props.setProperty("anthropic.max-tokens", maxTokensSpinner.getValue().toString());
                    
                    // Guardar manteniendo formato
                    savePropertiesWithComments(configFile, props);
                    
                    return true;
                    
                } catch (Exception e) {
                    errorMessage = e.getMessage();
                    e.printStackTrace();
                    return false;
                }
            }
            
            @Override
            protected void done() {
                try {
                    boolean success = get();
                    
                    if (success) {
                        showStatus("✅ Configuración guardada correctamente", SUCCESS_COLOR);
                        
                        // Esperar 1.5 segundos y cerrar
                        Timer timer = new Timer(1500, e -> {
                            configSaved = true;
                            
                            // Forzar recarga de configuración
                            ClaudeConfig.reload();
                            
                            dispose();
                        });
                        timer.setRepeats(false);
                        timer.start();
                        
                    } else {
                        showStatus("❌ Error: " + (errorMessage != null ? errorMessage : "desconocido"), ERROR_COLOR);
                        setControlsEnabled(true);
                        saveButton.setText(isFirstTime ? "💾 Guardar y Comenzar" : "💾 Guardar Cambios");
                    }
                    
                } catch (Exception e) {
                    showStatus("❌ Error inesperado: " + e.getMessage(), ERROR_COLOR);
                    e.printStackTrace();
                }
            }
        };
        
        worker.execute();
    }
    
    private void setControlsEnabled(boolean enabled) {
        apiKeyField.setEnabled(enabled);
        modelCombo.setEnabled(enabled);
        maxTokensSpinner.setEnabled(enabled);
        showHideButton.setEnabled(enabled);
        saveButton.setEnabled(enabled);
        if (cancelButton != null) {
            cancelButton.setEnabled(enabled);
        }
    }
    
    private void showStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setForeground(color);
    }
    
    private void savePropertiesWithComments(File file, Properties props) throws IOException {
        StringBuilder content = new StringBuilder();
        boolean foundEncrypted = false;
        
        // Leer archivo original para mantener comentarios
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Actualizar o comentar líneas específicas
                    if (line.trim().startsWith("anthropic.api-key=") && 
                        !line.trim().startsWith("anthropic.api-key.encrypted=")) {
                        content.append("# ").append(line).append(" # AUTO-ENCRIPTADO\n");
                        continue;
                    } else if (line.trim().startsWith("anthropic.api-key.encrypted=")) {
                        content.append("anthropic.api-key.encrypted=")
                               .append(props.getProperty("anthropic.api-key.encrypted"))
                               .append("\n");
                        foundEncrypted = true;
                        continue;
                    } else if (line.trim().startsWith("anthropic.model=")) {
                        content.append("anthropic.model=")
                               .append(props.getProperty("anthropic.model"))
                               .append("\n");
                        continue;
                    } else if (line.trim().startsWith("anthropic.max-tokens=")) {
                        content.append("anthropic.max-tokens=")
                               .append(props.getProperty("anthropic.max-tokens"))
                               .append("\n");
                        continue;
                    }
                    
                    content.append(line).append("\n");
                }
            }
        }
        
        // Si no existía la línea encrypted, agregarla
        if (!foundEncrypted && props.containsKey("anthropic.api-key.encrypted")) {
            content.append("\n# API Key Encriptado (generado automáticamente)\n");
            content.append("anthropic.api-key.encrypted=")
                   .append(props.getProperty("anthropic.api-key.encrypted"))
                   .append("\n");
        }
        
        // Escribir de vuelta
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(content.toString());
        }
    }
    
    private String generateMasterKey() {
        String userName = System.getProperty("user.name", "default");
        String osName = System.getProperty("os.name", "unknown");
        String javaVersion = System.getProperty("java.version", "unknown");
        String salt = "ChatBot-IA-O3-MCP-2025";
        
        return salt + "-" + userName + "-" + osName.hashCode() + "-" + javaVersion.hashCode();
    }
    
    public boolean isConfigSaved() {
        return configSaved;
    }
}