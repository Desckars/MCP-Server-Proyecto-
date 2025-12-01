# ChatBot IA - Claude Sonnet 4 + MCP O3

ChatBot inteligente que integra **Claude Sonnet 4** de Anthropic con consultas MDX sobre cubos usando el protocolo **Model Context Protocol (MCP)**.

## 🎯 Características

- ✅ **Claude Sonnet 4**: IA conversacional de última generación de Anthropic
- ✅ **MCP O3 Integration**: Conexión con cubos OLAP vía Model Context Protocol
- ✅ **Generación Automática de MDX**: Convierte lenguaje natural en consultas MDX
- ✅ **Interpretación de Resultados**: Claude analiza y explica los datos obtenidos
- ✅ **Dual UI**: Interfaz gráfica (Swing) y consola
- ✅ **Detección Inteligente**: Identifica automáticamente cuándo necesitas datos del cubo

## 📋 Requisitos Previos

### Software Necesario
- **Java 21** o superior
- **Maven 3.x**
- **MCP O3 Server** compilado y funcionando
- **Oracle Essbase O3** en ejecución (localhost:7777)

### API Key de Anthropic
1. Regístrate en https://console.anthropic.com/
2. Genera tu API key en la sección "API Keys"

## 🚀 Instalación Rápida

### Paso 1: Configurar API Key
Edita `src/main/resources/config.properties` con tu API key:
```properties
anthropic.api-key=sk-ant-api03-TU_KEY_AQUI
```

### Paso 2: Compilar
```bash
mvn clean compile
```

### Paso 4: Ejecutar
```bash
start.bat
```

O manualmente:
```bash
# Interfaz Gráfica
mvn exec:java

# Modo Consola
mvn exec:java -Dexec.args="--console"
```

## 📊 Ejemplos de Uso

### Consultas Generales
```
Tú: Explícame qué es MDX
Claude: MDX (Multidimensional Expressions) es un lenguaje de consulta...
```

### Consultas MDX Automáticas
```
Tú: Muéstrame las ventas por ubicación

Claude: 
📊 Consulta MDX ejecutada:
SELECT {Measures.[Units Sold]} ON COLUMNS, 
NON EMPTY {Location.children} ON ROWS 
FROM [Demo]

Resultados:
Location | Units Sold
---------|------------
France   | 12,450
Spain    | 8,932
...

💡 Interpretación:
Los datos muestran que Francia lidera en ventas...
```

## 🎮 Comandos en Consola

| Comando | Descripción |
|---------|-------------|
| `/ayuda` | Muestra ayuda |
| `/historial` | Ver conversación |
| `/limpiar` | Borrar historial |
| `/status` | Estado de conexiones |
| `/tools` | Listar herramientas MCP |
| `/salir` | Cerrar |

## 🏗️ Arquitectura

```
chatbot-ia/
├── src/main/java/com/chatbot/
│   ├── Main.java                   ✅ Punto de entrada
│   ├── config/
│   │   ├── ClaudeConfig.java      ✅ Configuración Claude
│   │   └── MCPConfig.java         ✅ Configuración MCP
│   ├── model/
│   │   └── Message.java           ✅ Modelo de mensaje
│   ├── service/
│   │   ├── AIService.java         ✅ Orquestador principal
│   │   ├── ChatService.java       ✅ Gestión conversación
│   │   ├── ClaudeService.java     ✅ Cliente Claude API
│   │   └── MCPService.java        ✅ Cliente MCP O3
│   └── ui/
│       ├── ChatUI.java            ✅ Interfaz gráfica
│       └── ConsoleUI.java         ✅ Interfaz consola
├── config.properties              ✅ Configuración
├── pom.xml                        ✅ Maven
└── start.bat                      ✅ Inicio rápido
```

## 🔧 Configuración

### config.properties
```properties
# Claude AI
anthropic.api-key=sk-ant-api03-TU_KEY_AQUI
anthropic.model=claude-sonnet-4-20250514
anthropic.max-tokens=4096

# MCP O3
mcp.o3.enabled=true
mcp.o3.jar-path=D:/MCP_PRUEBA/mcp_o3/target/mcp_o3-0.0.3-SNAPSHOT.jar
mcp.o3.working-directory=D:/MCP_PRUEBA/mcp_o3
```

## 🐛 Solución de Problemas

### Error: "API Key no configurada"
- Verifica `config.properties`
- La key debe empezar con `sk-ant-api03-`

### Consultas MDX fallan
- Verifica que O3 esté corriendo en localhost:7777
- Revisa logs del MCP: `mcp_o3/logs/MCP_O3_Server.log`

## 💰 Costos (con $5 USD )

| Actividad | Costo | Cantidad con $5 |
|-----------|-------|-----------------|
| Mensaje simple | ~$0.003 | ~1,600 |
| Consulta MDX | ~$0.006 | ~800 |
| Análisis complejo | ~$0.012 | ~400 |

## 📝 Notas

- ⚠️ **No compartir API key**
- 💾 **Historial no persiste**
- 🔄 **MCP se inicia bajo demanda**
- 🌐 **Requiere internet**

## 🆘 Soporte

1. Revisa esta documentación
2. Verifica logs del MCP
3. Consulta: https://docs.anthropic.com/

---

chatbot ahora tiene un sistema completo y seguro para manejar el API Key de Claude:
✅ Encriptación AES-256 automática
✅ Interfaz gráfica amigable
✅ Primera configuración obligatoria
✅ Reconfiguración fácil con botón ⚙️
✅ Sin edición manual de archivos