package com.chatbot.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Utilidad para encriptar/desencriptar API Keys usando AES-256
 * 
 * Uso:
 * 1. Encriptar: java -cp ... com.chatbot.security.EncryptionUtil encrypt "sk-ant-..." "tu-password"
 * 2. Desencriptar: String apiKey = EncryptionUtil.decrypt(encrypted, password);
 */
public class EncryptionUtil {
    
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int KEY_LENGTH = 256;
    private static final int ITERATION_COUNT = 65536;
    
    /**
     * Encripta un texto usando una contraseña
     */
    public static String encrypt(String plainText, String password) throws Exception {
        byte[] salt = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);
        
        SecretKey key = deriveKey(password, salt);
        
        byte[] iv = new byte[16];
        random.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));
        
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String ivB64 = Base64.getEncoder().encodeToString(iv);
        String encryptedB64 = Base64.getEncoder().encodeToString(encrypted);
        
        return saltB64 + ":" + ivB64 + ":" + encryptedB64;
    }
    
    /**
     * Desencripta un texto encriptado
     */
    public static String decrypt(String encryptedText, String password) throws Exception {
        String[] parts = encryptedText.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Formato de texto encriptado inválido");
        }
        
        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] iv = Base64.getDecoder().decode(parts[1]);
        byte[] encrypted = Base64.getDecoder().decode(parts[2]);
        
        SecretKey key = deriveKey(password, salt);
        
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] decrypted = cipher.doFinal(encrypted);
        
        return new String(decrypted, "UTF-8");
    }
    
    private static SecretKey deriveKey(String password, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_ALGORITHM);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }
    
    public static boolean isEncrypted(String text) {
        if (text == null || text.isEmpty()) return false;
        String[] parts = text.split(":");
        return parts.length == 3 && 
               parts[0].matches("^[A-Za-z0-9+/=]+$") &&
               parts[1].matches("^[A-Za-z0-9+/=]+$") &&
               parts[2].matches("^[A-Za-z0-9+/=]+$");
    }
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso:");
            System.out.println("  Encriptar: java ... EncryptionUtil encrypt \"sk-ant-...\" \"password\"");
            System.out.println("  Desencriptar: java ... EncryptionUtil decrypt \"encrypted\" \"password\"");
            return;
        }
        
        try {
            switch (args[0].toLowerCase()) {
                case "encrypt":
                    String encrypted = encrypt(args[1], args[2]);
                    System.out.println("\n✅ Encriptado:");
                    System.out.println(encrypted);
                    System.out.println("\nCopia a config.properties:");
                    System.out.println("anthropic.api-key.encrypted=" + encrypted);
                    break;
                case "decrypt":
                    String decrypted = decrypt(args[1], args[2]);
                    System.out.println("\n✅ Desencriptado:");
                    System.out.println(decrypted);
                    break;
            }
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }
}