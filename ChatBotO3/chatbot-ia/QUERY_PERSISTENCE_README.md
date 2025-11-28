# Documentación: Sistema de Persistencia de Queries Exitosas

## Descripción General

Este sistema permite al usuario validar queries como "exitosas" o "fallidas" mediante botones en la interfaz, almacenando ejemplos exitosos que el LLM puede usar como referencia para mejorar futuras consultas.

## Componentes Implementados

### 1. **Query.java** (Modelo de Datos)
- Representa una query almacenada con metadata
- Propiedades: `id`, `userPrompt`, `mdxQuery`, `queryResult`, `timestamp`, `successRating`, `notes`
- Métodos:
  - `markAsSuccessful()` / `markAsFailed()`: Calificar queries
  - `toInstructionString()`: Convertir a texto legible para el LLM

### 2. **QueryPersistenceService.java** (Persistencia)
- Servicio singleton para gestionar queries
- Almacena queries en: `data/queries_data/successful_queries.json`
- Métodos principales:
  - `saveQuery(Query)`: Guardar una query
  - `getSuccessfulQueries()`: Obtener solo las exitosas
  - `generateInstructionsFromSuccessfulQueries()`: Generar contexto para Claude
  - `getStatistics()`: Estadísticas de queries
  - `exportToFile() / importFromFile()`: Backup y restauración

### 3. **QueryContextProvider.java** (Intermediario)
- Facilita la integración con ClaudeService
- Métodos:
  - `getSuccessfulQueriesContext()`: Obtener contexto para incluir en system prompt
  - `registerSuccessfulQuery()`: Registrar query exitosa
  - `getStatistics()`: Obtener estadísticas

### 4. **MessagePanel.java** (UI)
- Panel personalizado para cada mensaje
- Muestra botones de acción para respuestas de Claude:
  - 👍 **Útil**: Marca la query como exitosa
  - 👎 **Inútil**: Marca como fallida
  - 📋 **Ver Detalles**: Muestra información de la query

### 5. **ChatUI.java** (Interfaz Modificada)
- Cambio de `JTextArea` a `JPanel` para mensajes interactivos
- Integración de `MessagePanel` para cada mensaje
- Rastreo del último prompt del usuario (`lastUserPrompt`)

## Flujo de Funcionamiento

```
1. Usuario escribe un prompt
   ↓
2. ChatUI envía a Claude
   ↓
3. Claude genera respuesta/query
   ↓
4. Se muestra en MessagePanel con botones de Like/Dislike
   ↓
5. Usuario hace clic en botón
   ↓
6. Se guarda en queries_data/successful_queries.json
   ↓
7. Próxima solicitud a Claude incluye ejemplos exitosos en el system prompt
```

## Cómo Integrar en ClaudeService

Para completar la integración y que Claude use los ejemplos exitosos como referencia:

### Paso 1: Modificar `buildSystemPrompt()`

En el método `buildSystemPrompt()` de `ClaudeService.java`, agregar al inicio:

```java
private String buildSystemPrompt() {
    // Obtener ejemplos de queries exitosas
    String successfulExamples = QueryContextProvider.getInstance()
        .getSuccessfulQueriesContext();
    
    // Construcción del system prompt base
    String basePrompt = """
        [Tu sistema prompt actual]
        """;
    
    // Retornar el prompt combinado
    return basePrompt + successfulExamples;
}
```

### Paso 2: Pasar contexto entre objetos

Asegurar que `QueryContextProvider` sea accesible cuando se llama `buildSystemPrompt()`:

```java
// En ChatUI o AIService
QueryContextProvider contextProvider = QueryContextProvider.getInstance();
```

## Estructura de Datos Persistida

El archivo `data/queries_data/successful_queries.json` tiene este formato:

```json
[
  {
    "id": "1732707123456789",
    "userPrompt": "Muéstrame todas las unidades vendidas por cliente",
    "mdxQuery": "SELECT {Measures.[Units Sold]} ON COLUMNS, {Customers.Customers.[Major Accounts]} ON ROWS FROM Demo",
    "queryResult": "[datos de resultado]",
    "timestamp": "2025-11-27T18:12:03",
    "successRating": 1,
    "notes": "Validated as successful by user"
  }
]
```

## Opciones y Riesgos

### Oportunidades
✅ **Mejora de Precisión**: El LLM tendrá ejemplos reales de queries exitosas
✅ **Adaptación Contextual**: Aprende del dominio específico del usuario
✅ **Reducción de Errores**: Menos queries inválidas o incompletas
✅ **Retroalimentación Visual**: El usuario ve que sus validaciones se guardan
✅ **Análisis Histórico**: Se pueden ver patrones de queries exitosas/fallidas

### Riesgos
⚠️ **Contaminación del Contexto**: Si hay muchas queries fallidas, podrían afectar negativamente
⚠️ **Tamaño del Context**: Demasiados ejemplos pueden exceder límites de tokens
⚠️ **Falta de Validación**: El usuario podría marcar queries incorrectas como exitosas
⚠️ **Dependencia de Feedback**: Si no hay suficiente feedback del usuario, el sistema es limitado

## Soluciones Propuestas

### Para Contaminación del Contexto
- Solo usar queries con `successRating == 1` (exitosas verificadas)
- Limitar a los últimos N ejemplos más recientes
- Implementar un sistema de puntuación (confianza basada en frecuencia)

### Para Tamaño del Contexto
- Truncar instrucciones a máximo X caracteres
- Usar solo las últimas 5-10 queries exitosas más relevantes
- Separar por tipo de query (exploratorias vs. complejas)

### Para Validación del Usuario
- Agregar un campo de "confianza" que se incrementa con validaciones consistentes
- Implementar review manual de queries cuestionables
- Mantener un historial de validaciones del usuario

### Para Falta de Feedback
- Implementar validación automática basada en éxito de ejecución
- Crear sistema de sugerencia ("¿Esta query fue útil?")
- Panel de administración para curar queries manualmente

## Próximos Pasos

1. ✅ Crear modelo de persistencia (Query.java)
2. ✅ Crear servicio de persistencia (QueryPersistenceService.java)
3. ✅ Crear UI interactiva (MessagePanel.java, ChatUI.java)
4. ⏳ Integrar en ClaudeService para incluir ejemplos en system prompt
5. ⏳ Crear panel de administración de queries guardadas
6. ⏳ Implementar límites de tamaño de contexto
7. ⏳ Agregar validación automática de queries

## Uso

### Para el Usuario Final
1. Escribir un prompt en el chatbot
2. Claude genera una respuesta/query
3. Si la respuesta es útil: clic en **👍 Útil**
4. Si no: clic en **👎 Inútil**
5. El sistema guarda la información automáticamente

### Para el Desarrollador
```java
// Obtener estadísticas
QueryContextProvider provider = QueryContextProvider.getInstance();
String stats = provider.getStatistics();

// Registrar query exitosa manualmente
provider.registerSuccessfulQuery(userPrompt, claudeResponse);

// Obtener contexto para Claude
String context = provider.getSuccessfulQueriesContext();
```

## Notas Técnicas

- **Thread Safety**: `QueryPersistenceService` usa `synchronized` para operaciones de escritura
- **Singleton Pattern**: Ambos servicios son singletons para garantizar una única instancia
- **Lazy Loading**: Las queries se cargan solo una vez al iniciar
- **JSON Serialization**: Usa GSON con TypeAdapter personalizado para `LocalDateTime`
- **File I/O**: Crea directorios automáticamente si no existen

## Testing

Para validar el sistema:

```bash
# 1. Generar algunas queries exitosas
# 2. Verificar que exista: data/queries_data/successful_queries.json
# 3. Validar que el JSON sea válido
# 4. Verificar que Claude reciba las instrucciones en el system prompt
# 5. Confirmar que las nuevas queries se generan siguiendo los ejemplos
```
