package com.chatbot.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ConversationLogger {
    private static ConversationLogger instance;
    private BufferedWriter writer;
    private final Object lock = new Object();
    private final ZoneId URUGUAY = ZoneId.of("America/Montevideo");

    private ConversationLogger() {
        try {
            initWriter();
        } catch (Exception e) {
            System.err.println("No se pudo inicializar ConversationLogger: " + e.getMessage());
        }
    }

    private void initWriter() throws IOException {
        String userDir = System.getProperty("user.dir");
        Path logsDir = Paths.get(userDir, "logs");
        Files.createDirectories(logsDir);

        String ts = ZonedDateTime.now(URUGUAY).format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        Path logFile = logsDir.resolve("conversation-" + ts + ".txt");

        writer = Files.newBufferedWriter(logFile, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

        writeHeader();
    }

    private void writeHeader() throws IOException {
        String header = "Conversation log - " + ZonedDateTime.now(URUGUAY).format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")) + System.lineSeparator();
        header += "----------------------------------------" + System.lineSeparator();
        synchronized (lock) {
            writer.write(header);
            writer.flush();
        }
    }

    public static synchronized ConversationLogger getInstance() {
        if (instance == null) {
            instance = new ConversationLogger();
        }
        return instance;
    }

    public void logUser(String message) {
        log("USER", message);
    }

    public void logAssistant(String assistantName, String message) {
        log(assistantName, message);
    }

    public void logMCPQuery(String mdx) {
        log("MCP_QUERY", mdx);
    }

    public void logMCPResponse(String response) {
        log("MCP_RESPONSE", response);
    }

    public void logInfo(String info) {
        log("INFO", info);
    }

    private void log(String tag, String content) {
        String ts = ZonedDateTime.now(URUGUAY).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String line = String.format("[%s] %s: %s", ts, tag, content == null ? "" : content) + System.lineSeparator();

        try {
            if (writer != null) {
                synchronized (lock) {
                    writer.write(line);
                    writer.flush();
                }
            } else {
                // Fallback
                System.out.print(line);
            }
        } catch (IOException e) {
            System.err.println("Error escribiendo log: " + e.getMessage());
        }
    }

    public void close() {
        try {
            if (writer != null) {
                synchronized (lock) {
                    writer.write("\n--- session closed ---\n");
                    writer.flush();
                    writer.close();
                    writer = null;
                }
            }
        } catch (IOException e) {
            System.err.println("Error cerrando logger: " + e.getMessage());
        }
    }
}
