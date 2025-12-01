# ChatBot IA - Claude Sonnet 4 + MCP O3

**¡Versión actualizada a Spring Boot 3.3.5!** 🚀

ChatBot inteligente que integra **Claude Sonnet 4** de Anthropic con consultas MDX sobre cubos usando el protocolo **Model Context Protocol (MCP)**.

Ahora con **interfaz web moderna**, **API REST completa** y **consola interactiva**, mientras mantiene todas las funcionalidades originales.

## 🎯 Características

- ✅ **Claude Sonnet 4**: IA conversacional de última generación de Anthropic
- ✅ **MCP O3 Integration**: Conexión con cubos OLAP vía Model Context Protocol
- ✅ **Generación Automática de MDX**: Convierte lenguaje natural en consultas MDX
- ✅ **Interpretación de Resultados**: Claude analiza y explica los datos obtenidos
- ✅ **Dual UI**: 
  - Interfaz gráfica web moderna (GUI)
  - Consola web interactiva
  - API REST completa
- ✅ **Detección Inteligente**: Identifica automáticamente cuándo necesitas datos del cubo
- ✅ **Spring Boot**: Arquitectura moderna y escalable

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

**Opción A: Variable de Entorno (Recomendado)**
```powershell
$env:CLAUDE_API_KEY = "sk-ant-api03-tu-key-aqui"
```

**Opción B: Archivo application.yml**
```yaml
anthropic:
  api-key: sk-ant-api03-tu-key-aqui
```

### Paso 2: Ejecutar
```bash
# En Windows, ejecuta:
start.bat

# O manualmente:
mvn clean package
java -jar target/chatbot-ia-*.jar
```

### Paso 3: Acceder
- **Interfaz Web**: http://localhost:8080
- **Consola Web**: http://localhost:8080/console
- **Health Check**: http://localhost:8080/actuator/health

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
| `/status` | Estado del sistema |
| `/tools` | Listar herramientas MCP |
| `/mdx <query>` | Ejecutar MDX directo |
| `/ui` | Ir a interfaz gráfica |
| `/salir` | Cerrar |

## 🏗️ Arquitectura Spring Boot

```
chatbot-ia/
├── src/main/java/com/chatbot/
│   ├── ChatbotIaApplication.java          ✅ Punto de entrada Spring Boot
│   ├── controller/
│   │   ├── ChatController.java            ✅ API REST /api/chat
│   │   ├── AIController.java              ✅ API REST /api/ai
│   │   ├── ViewController.java            ✅ Vistas HTML
│   │   └── ChatBotHealthIndicator.java    ✅ Health check
│   ├── config/
│   │   ├── BeansConfiguration.java        ✅ Configuración de beans
│   │   ├── ApplicationStartupListener.java ✅ Lifecycle
│   │   ├── AnthropicProperties.java       ✅ Propiedades
│   │   └── MCPProperties.java             ✅ Propiedades MCP
│   ├── service/                           ✅ Servicios (originales mantenidos)
│   │   ├── ChatService.java
│   │   ├── ClaudeService.java
│   │   ├── MCPService.java
│   │   ├── AIService.java
│   │   └── ...
│   └── model/
│       └── Message.java
├── src/main/resources/
│   ├── application.yml                    ✅ Configuración Spring
│   ├── logback-spring.xml                 ✅ Logging
│   └── templates/
│       ├── index.html                     ✅ Interfaz gráfica
│       └── console.html                   ✅ Consola web
├── pom.xml                                ✅ Maven (actualizado)
└── MIGRACION_SPRING_BOOT.md               ✅ Documentación
```

## 📡 API REST Endpoints

### Chat
```
POST /api/chat/message
{
  "message": "Hola, ¿cómo estás?"
}

GET /api/chat/history
POST /api/chat/clear
GET /api/chat/stats
```

### IA Avanzada
```
GET /api/ai/status
GET /api/ai/tools
POST /api/ai/execute-mdx
{
  "query": "SELECT ... FROM [Demo]"
}
POST /api/ai/clear-context
```

### Monitoreo
```
GET /actuator/health
GET /actuator/info
GET /actuator/metrics
```

## 🔧 Configuración

### application.yml
```yaml
server:
  port: 8080

anthropic:
  api-key: ${CLAUDE_API_KEY:}
  model: claude-sonnet-4-20250514
  max-tokens: 4096

mcp:
  o3:
    enabled: true
    jar-path: mcp/mcp_o3-0.0.4-SNAPSHOT.jar

logging:
  level:
    com.chatbot: DEBUG
  file:
    name: logs/application.log
```

### Variables de Entorno
```
CLAUDE_API_KEY=sk-ant-api03-...
SERVER_PORT=8080
```

## 🔄 Migración desde Versión Anterior

Si vienes de la versión anterior (Standalone Java):

1. **Los servicios se mantienen igual** - `ChatService`, `ClaudeService`, etc. sin cambios
2. **La interfaz es web** - Ya no es Swing Desktop
3. **Configuración en application.yml** - Además de config.properties
4. **API REST disponible** - Puedes acceder desde cualquier cliente HTTP

Para documentación completa de la migración, ver `MIGRACION_SPRING_BOOT.md`

## 🐛 Solución de Problemas

### Error: "API Key no configurada"
- Verifica `application.yml` o variable de entorno `CLAUDE_API_KEY`
- La key debe empezar con `sk-ant-api03-`

### Puerto 8080 ocupado
- Cambia el puerto en `application.yml`:
  ```yaml
  server:
    port: 8081
  ```

### Errores de compilación
```bash
mvn clean install -DskipTests
```

### Consultas MDX fallan
- Verifica que O3 esté corriendo en localhost:7777
- Revisa logs: `logs/application.log`

## 💰 Costos (con $5 USD )

| Actividad | Costo | Cantidad con $5 |
|-----------|-------|-----------------|
| Mensaje simple | ~$0.003 | ~1,600 |
| Consulta MDX | ~$0.006 | ~800 |
| Análisis complejo | ~$0.012 | ~400 |

## 📝 Notas

- ⚠️ **No compartir API key** - Usar variables de entorno
- 💾 **Historial persiste** en memoria durante la sesión
- 🔄 **MCP se inicia bajo demanda**
- 🌐 **Requiere internet** para conectar con Claude API
- 🔐 **HTTPS recomendado** para producción

## 📚 Documentación Adicional

- `MIGRACION_SPRING_BOOT.md` - Guía completa de migración
- `RESUMEN_MIGRACION_SPRING_BOOT.md` - Resumen técnico
- `QUERY_PERSISTENCE_README.md` - Persistencia de consultas
- `SISTEMA_PERSISTENCIA_COMPLETO.md` - Sistema de persistencia

## 🚀 Deployment

### Docker (próximamente)
```dockerfile
FROM eclipse-temurin:21-jdk
COPY target/chatbot-ia-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Cloud (Azure, AWS, GCP)
La aplicación Spring Boot es compatible con:
- Azure App Service
- AWS Elastic Beanstalk
- Google Cloud Run
- Kubernetes

## 🔗 Enlaces Útiles

- [Documentación de Claude API](https://docs.anthropic.com/)
- [Documentación de Spring Boot](https://spring.io/projects/spring-boot)
- [Model Context Protocol](https://spec.modelcontextprotocol.io/)

---

**Versión**: 2.0 Spring Boot Edition  
**Java**: 21  
**Spring Boot**: 3.3.5  
**Última actualización**: 2025-11-29

¡Gracias por usar ChatBot IA! 🚀