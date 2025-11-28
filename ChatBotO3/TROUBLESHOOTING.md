# TROUBLESHOOTING - Sistema de Persistencia de Queries

## 🚨 Errores Comunes y Soluciones

### Error 1: NumberFormatException en ClaudeConfig
```
Exception: java.lang.NumberFormatException: For input string: "4096 "
Location: ClaudeConfig.java:122
```

**Causa**: El archivo `config.properties` tiene espacios al final de valores numéricos.

**Solución**: 
```properties
# ❌ INCORRECTO
anthropic.max-tokens=4096 

# ✅ CORRECTO
anthropic.max-tokens=4096
```

El código ya está arreglado para hacer `.trim()` automáticamente.

---

### Error 2: API Key no encontrada
```
❌ API Key no configurado
```

**Causa**: La API Key no está configurada en ninguna de las 3 formas soportadas.

**Soluciones**:
1. **Mediante variable de entorno** (mejor para CI/CD):
   ```powershell
   $env:CLAUDE_API_KEY = "sk-ant-..."
   java -jar target/chatbot-ia-executable.jar
   ```

2. **Mediante archivo config.properties**:
   ```properties
   anthropic.api-key=sk-ant-...
   ```
   (Se encriptará automáticamente)

3. **UI de configuración**:
   - Se abrirá automáticamente si falta
   - Ingresa tu API Key y se guardará encriptado

---

### Error 3: Archivo config.properties no encontrado
```
Archivo config.properties no encontrado
Ruta esperada: c:\...
```

**Causa**: El archivo no está en `src/main/resources/`

**Solución**:
```bash
# Ubicación correcta:
src/main/resources/config.properties

# O crear uno nuevo:
copy config.properties.example config.properties
```

---

### Error 4: ClassNotFoundException - QueryPersistenceService
```
java.lang.ClassNotFoundException: com.chatbot.service.QueryPersistenceService
```

**Causa**: El JAR no fue regenerado después de crear la clase.

**Solución**:
```bash
cd chatbot-ia
mvn clean package -DskipTests
```

---

### Error 5: NullPointerException en QueryPersistenceService.getInstance()
```
java.lang.NullPointerException at QueryPersistenceService.getInstance()
```

**Causa**: El singleton no se inicializó correctamente.

**Solución**: 
```java
// Usar siempre de esta forma:
QueryPersistenceService service = QueryPersistenceService.getInstance();

// NO hacer:
QueryPersistenceService service = new QueryPersistenceService(); // Error
```

---

### Error 6: successful_queries.json vacío o corrupto
```
[Advertencia] No se pueden cargar queries previas
```

**Causa**: El archivo JSON está malformado o vacío.

**Soluciones**:
1. **Resetear archivo**:
   ```bash
   del data/queries_data/successful_queries.json
   # Se regenerará al crear la primera query
   ```

2. **Restaurar desde backup**:
   ```bash
   copy backup_queries.json data/queries_data/successful_queries.json
   ```

3. **Validar JSON**:
   ```powershell
   $json = Get-Content data/queries_data/successful_queries.json | ConvertFrom-Json
   # Si falla, el JSON es inválido
   ```

---

### Error 7: MessagePanel no aparece en chat
```
Respuestas de Claude sin botones Like/Dislike
```

**Causa**: ChatUI no está usando MessagePanel.

**Solución**: Verificar que en ChatUI.java:
```java
private void appendMessage(String sender, String content) {
    MessagePanel messagePanel = new MessagePanel(sender, content, lastUserPrompt);
    chatArea.add(messagePanel);  // <-- Debe estar aquí
    chatArea.revalidate();
    chatArea.repaint();
}
```

---

### Error 8: Botones Like/Dislike deshabilitados
```
Los botones no responden después de clickear
```

**Esperado**: Es correcto comportamiento. Una vez que califiques una respuesta:
- Se deshabilitan los botones
- Se guarda la query
- Se muestra confirmación

**Para cambiar**:
```java
// En MessagePanel.java
private void disableActionButtons(JPanel panel) {
    for (Component comp : panel.getComponents()) {
        if (comp instanceof JButton) {
            comp.setEnabled(false);  // Cambiar a true si quieres permitir recalificar
        }
    }
}
```

---

### Error 9: Memoria insuficiente con muchas queries
```
OutOfMemoryError: Java heap space
```

**Causa**: Demasiadas queries guardadas en memoria.

**Soluciones**:
1. **Limpiar queries antiguas**:
   ```bash
   java -cp target/chatbot-ia-executable.jar com.chatbot.util.QueryCleaner
   ```

2. **Aumentar memoria Java**:
   ```bash
   java -Xmx2048m -jar target/chatbot-ia-executable.jar
   ```

3. **Exportar y limpiar**:
   ```java
   service.exportToFile("backup_old.json");
   // Eliminar queries viejas
   service.deleteQuery(queryId);
   ```

---

### Error 10: Queries no se guardan
```
✓ Query guardada: ...
// Pero el archivo no se actualiza
```

**Causa**: Permisos de escritura en el directorio.

**Soluciones**:
1. **Verificar permisos**:
   ```powershell
   Get-Acl "data/queries_data" | Format-List
   ```

2. **Crear directorio manualmente**:
   ```powershell
   New-Item -ItemType Directory -Path "data/queries_data" -Force
   ```

3. **Ejecutar como Administrador**:
   ```powershell
   Start-Process powershell -Verb RunAs
   ```

---

## 📊 Debugging

### Ver logs detallados
```bash
java -jar target/chatbot-ia-executable.jar > debug.log 2>&1
```

### Verificar que QueryPersistenceService está activo
```java
// En Main.java o chatbot
QueryPersistenceService service = QueryPersistenceService.getInstance();
Map stats = service.getStatistics();
System.out.println("Total queries: " + stats.get("total_queries"));
```

### Validar JSON generado
```powershell
$json = Get-Content data/queries_data/successful_queries.json -Raw
$parsed = $json | ConvertFrom-Json
$parsed | Format-Table -AutoSize
```

---

## ✅ Checklist de Verificación

- [ ] Java 21+ instalado: `java -version`
- [ ] config.properties existe en `src/main/resources/`
- [ ] API Key configurada (variable, archivo o UI)
- [ ] Compilación exitosa: `mvn clean compile -q`
- [ ] JAR generado: `target/chatbot-ia-executable.jar`
- [ ] Directorio `data/queries_data` tiene permisos de escritura
- [ ] Archivo `successful_queries.json` es válido JSON
- [ ] MessagePanel aparece en respuestas de Claude
- [ ] Botones Like/Dislike son clickeables
- [ ] Primera query se guarda correctamente

---

## 🔍 Validación Manual

### Test 1: Crear directorio
```powershell
$dir = "data/queries_data"
if (-not (Test-Path $dir)) {
    New-Item -ItemType Directory -Path $dir -Force
    Write-Host "✓ Directorio creado"
}
```

### Test 2: Escribir archivo de prueba
```powershell
$testFile = "data/queries_data/test.json"
"[]" | Set-Content $testFile
if (Test-Path $testFile) {
    Write-Host "✓ Permisos de escritura OK"
    Remove-Item $testFile
}
```

### Test 3: Cargar QueryPersistenceService
```bash
cd chatbot-ia
mvn compile exec:java -Dexec.mainClass="com.chatbot.service.QueryPersistenceService"
```

---

## 📞 Contacto y Recursos

- **Documentación**: Ver `SISTEMA_PERSISTENCIA_COMPLETO.md`
- **API Reference**: Ver `QUERY_PERSISTENCE_API_REFERENCE.java`
- **Quick Start**: Ejecutar `START.ps1`
- **Logs**: Ver `logs/conversation-*.txt`

---

## 🐛 Report de Bug

Si encuentras un error:

1. **Captura la excepción completa** (stack trace)
2. **Nota el paso exacto** donde ocurre
3. **Incluye versión de Java**: `java -version`
4. **Sistema operativo**: Windows/Linux/Mac
5. **Archivo de log**: `logs/conversation-*.txt`

Ejemplo:
```
Error: NumberFormatException en ClaudeConfig
Java version: 21.0.1
OS: Windows 11
Paso: Al abrir interfaz gráfica
Stack trace: [pega aquí]
```
