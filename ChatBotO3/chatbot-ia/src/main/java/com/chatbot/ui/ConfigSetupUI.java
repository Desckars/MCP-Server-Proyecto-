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
 * Se integra con el sistema de encriptación existente
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
    
    public ConfigSetupUI(Frame parent) {
        super(parent, "⚙️ Configuración de Claude AI", true);
        
        // Verificar si es primera vez
        this.isFirstTime = !hasExistingConfig();
        
        initComponents();
        loadCurrentConfig();
        
        // Si es primera vez, no permitir cancelar
        if (isFirstTime) {
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            cancelButton.setEnabled(false);
            
            addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    int option = JOptionPane.showConfirmDialog(
                        ConfigSetupUI.this,
                        "❗ Necesitas configurar el API Key para usar el chatbot.\n\n¿Deseas salir de la aplicación?",
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
        setLayout(new BorderLayout(15, 15));
        setSize(650, isFirstTime ? 500 : 450);
        setLocationRelativeTo(getParent());
        setResizable(false);
        
        // Panel principal con fondo
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(25, 30, 25, 30));
        mainPanel.setBackground(new Color(245, 247, 250));
        
        // ========================================
        // ENCABEZADO
        // ========================================
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(new Color(245, 247, 250));
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel iconLabel = new JLabel("🔐");
        iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 48));
        iconLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.add(iconLabel);
        headerPanel.add(Box.createVerticalStrut(10));
        
        JLabel titleLabel = new JLabel(isFirstTime ? 
            "Configuración Inicial" : "Actualizar Configuración");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(8));
        
        String infoText = isFirstTime ? 
            "Tu API Key será encriptado automáticamente y guardado de forma segura." :
            "Actualiza tu configuración. Los cambios se aplicarán inmediatamente.";
        
        infoLabel = new JLabel("<html>" + infoText + "</html>");
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        infoLabel.setForeground(new Color(100, 100, 100));
        infoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.add(infoLabel);
        
        mainPanel.add(headerPanel);
        mainPanel.add(Box.createVerticalStrut(20));
        
        // ========================================
        // API KEY SECTION
        // ========================================
        JPanel apiKeyPanel = createStyledPanel("🔑 API Key de Anthropic");
        
        JPanel apiKeyInputPanel = new JPanel(new BorderLayout(8, 0));
        apiKeyInputPanel.setBackground(Color.WHITE);
        
        apiKeyField = new JPasswordField();
        apiKeyField.setFont(new Font("Consolas", Font.PLAIN, 12));
        apiKeyField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        apiKeyField.setEchoChar('•');
        
        showHideButton = new JButton("👁");
        showHideButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        showHideButton.setPreferredSize(new Dimension(45, 41));
        showHideButton.setFocusPainted(false);
        showHideButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        showHideButton.setToolTipText("Mostrar/Ocultar API Key");
        showHideButton.addActionListener(e -> toggleApiKeyVisibility());
        
        apiKeyInputPanel.add(apiKeyField, BorderLayout.CENTER);
        apiKeyInputPanel.add(showHideButton, BorderLayout.EAST);
        
        apiKeyPanel.add(apiKeyInputPanel);
        
        JLabel hintLabel = new JLabel("Formato: sk-ant-api03-...");
        hintLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        hintLabel.setForeground(new Color(150, 150, 150));
        hintLabel.setBorder(new EmptyBorder(5, 2, 0, 0));
        apiKeyPanel.add(hintLabel);
        
        mainPanel.add(apiKeyPanel);
        mainPanel.add(Box.createVerticalStrut(15));
        
        // ========================================
        // MODELO
        // ========================================
        JPanel modelPanel = createStyledPanel("🤖 Modelo de Claude");
        
        String[] models = {
            "claude-sonnet-4-20250514",
            "claude-opus-4-20250514",
            "claude-haiku-4-20250514"
        };
        modelCombo = new JComboBox<>(models);
        modelCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        modelCombo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        ((JComponent) modelCombo.getRenderer()).setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        modelPanel.add(modelCombo);
        
        mainPanel.add(modelPanel);
        mainPanel.add(Box.createVerticalStrut(15));
        
        // ========================================
        // MAX TOKENS
        // ========================================
        JPanel tokensPanel = createStyledPanel("📊 Max Tokens por Respuesta");
        
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(4096, 1024, 200000, 512);
        maxTokensSpinner = new JSpinner(spinnerModel);
        maxTokensSpinner.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        ((JSpinner.DefaultEditor) maxTokensSpinner.getEditor()).getTextField().setHorizontalAlignment(JTextField.LEFT);
        maxTokensSpinner.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        
        tokensPanel.add(maxTokensSpinner);
        
        mainPanel.add(tokensPanel);
        mainPanel.add(Box.createVerticalStrut(20));
        
        // ========================================
        // STATUS LABEL
        // ========================================
        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel.setBorder(new EmptyBorder(0, 5, 10, 0));
        mainPanel.add(statusLabel);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // ========================================
        // BOTONES
        // ========================================
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 15));
        buttonPanel.setBackground(new Color(245, 247, 250));
        
        cancelButton = new JButton("Cancelar");
        cancelButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cancelButton.setPreferredSize(new Dimension(100, 38));
        cancelButton.setFocusPainted(false);
        cancelButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelButton.addActionListener(e -> {
            configSaved = false;
            dispose();
        });
        
        saveButton = new JButton(isFirstTime ? "💾 Guardar y Comenzar" : "💾 Guardar Cambios");
        saveButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        saveButton.setPreferredSize(new Dimension(isFirstTime ? 180 : 160, 38));
        saveButton.setBackground(new Color(120, 70, 255));
        saveButton.setForeground(Color.WHITE);
        saveButton.setFocusPainted(false);
        saveButton.setBorderPainted(false);
        saveButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveButton.addActionListener(e -> saveConfiguration());
        
        if (!isFirstTime) {
            buttonPanel.add(cancelButton);
        }
        buttonPanel.add(saveButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Enter para guardar
        apiKeyField.addActionListener(e -> saveConfiguration());
    }
    
    private JPanel createStyledPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
            new EmptyBorder(15, 15, 15, 15)
        ));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        panel.add(titleLabel);
        
        return panel;
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
                    showStatus("⚠️ No se pudo cargar el API Key existente", Color.ORANGE);
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
            showStatus("❌ Debes ingresar un API Key", Color.RED);
            apiKeyField.requestFocus();
            return;
        }
        
        // Validar formato
        if (!apiKey.startsWith("sk-ant-api03-")) {
            showStatus("❌ Formato inválido. Debe comenzar con: sk-ant-api03-", Color.RED);
            apiKeyField.requestFocus();
            return;
        }
        
        if (apiKey.length() < 30) {
            showStatus("❌ API Key demasiado corto. Verifica que esté completo", Color.RED);
            apiKeyField.requestFocus();
            return;
        }
        
        // Deshabilitar controles mientras guarda
        saveButton.setEnabled(false);
        cancelButton.setEnabled(false);
        apiKeyField.setEnabled(false);
        modelCombo.setEnabled(false);
        maxTokensSpinner.setEnabled(false);
        
        saveButton.setText("🔐 Encriptando...");
        showStatus("🔐 Encriptando y guardando configuración...", new Color(0, 120, 215));
        
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
                        showStatus("✅ Configuración guardada correctamente", new Color(0, 150, 0));
                        
                        // Esperar 1 segundo y cerrar
                        Timer timer = new Timer(1000, e -> {
                            configSaved = true;
                            
                            // Forzar recarga de configuración
                            ClaudeConfig.reload();
                            
                            dispose();
                        });
                        timer.setRepeats(false);
                        timer.start();
                        
                    } else {
                        showStatus("❌ Error: " + (errorMessage != null ? errorMessage : "desconocido"), Color.RED);
                        
                        // Rehabilitar controles
                        saveButton.setEnabled(true);
                        cancelButton.setEnabled(!isFirstTime);
                        apiKeyField.setEnabled(true);
                        modelCombo.setEnabled(true);
                        maxTokensSpinner.setEnabled(true);
                        saveButton.setText(isFirstTime ? "💾 Guardar y Comenzar" : "💾 Guardar Cambios");
                    }
                    
                } catch (Exception e) {
                    showStatus("❌ Error inesperado: " + e.getMessage(), Color.RED);
                    e.printStackTrace();
                }
            }
        };
        
        worker.execute();
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