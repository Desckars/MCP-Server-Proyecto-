# Quick Start - Sistema de Persistencia de Queries

## 🎯 Lo que tienes ahora

✅ Sistema completo para guardar queries exitosas validadas por el usuario  
✅ Interfaz con botones Like/Dislike en cada respuesta  
✅ Almacenamiento en JSON (`data/queries_data/successful_queries.json`)  
✅ Integración con Claude para usar ejemplos como referencia  
✅ Estadísticas y gestión de queries  

## 🚀 Cómo Funciona (Resumen)

```
Usuario escribe prompt
    ↓
Claude responde
    ↓
Usuario valida: 👍 Útil o 👎 Inútil
    ↓
Sistema guarda automáticamente
    ↓
Próximo prompt de Claude usa ejemplos como referencia
    ↓
Claude genera queries mejores
```

## 📁 Archivos Nuevos/Modificados

| Archivo | Estado | Descripción |
|---------|--------|-------------|
| `Query.java` | ✅ NUEVO | Modelo de datos para queries |
| `QueryPersistenceService.java` | ✅ NUEVO | Servicio de persistencia JSON |
| `QueryContextProvider.java` | ✅ NUEVO | Intermediario Claude ↔️ Queries |
| `QueryPersistenceTesting.java` | ✅ NUEVO | Utilities para testing |
| `MessagePanel.java` | ✅ NUEVO | UI con botones Like/Dislike |
| `ChatUI.java` | ✅ MODIFICADO | Cambio JTextArea → JPanel |
| `ClaudeService.java` | ✅ MODIFICADO | Incluye ejemplos en system prompt |
| `QUERY_PERSISTENCE_README.md` | 📖 DOC | Documentación detallada |
| `INTEGRATION_GUIDE.java` | 📖 DOC | Guía de integración |

## 🎮 Para el Usuario

1. **Escribir mensaje** → Chat normal
2. **Claude responde** → Ver respuesta
3. **Validar** → Clic en 👍 o 👎
4. **Automático** → Sistema guarda

## 🔧 Para el Desarrollador

```java
// Obtener contexto para Claude
String context = QueryContextProvider.getInstance()
    .getSuccessfulQueriesContext();

// Registrar query exitosa manualmente
QueryContextProvider.getInstance()
    .registerSuccessfulQuery(userPrompt, claudeResponse);

// Ver estadísticas
String stats = QueryContextProvider.getInstance()
    .getStatistics();
```

## 📊 Datos Almacenados

Archivo: `data/queries_data/successful_queries.json`

```json
[
  {
    "id": "1732707123456789",
    "userPrompt": "Muéstrame unidades vendidas",
    "mdxQuery": "SELECT {Measures.[Units Sold]} ...",
    "queryResult": "Resultados...",
    "timestamp": "2025-11-27T18:12:03",
    "successRating": 1,
    "notes": "Validated by user"
  }
]
```

## ⚖️ Evaluación Rápida

### ✅ Beneficios
- **Mejora de precisión**: Claude usa ejemplos reales
- **Menos errores**: Contexto específico del dominio
- **Adaptativo**: Aprende del usuario
- **Visible**: Usuario ve que se guardó

### ⚠️ Consideraciones
- Solo guarda queries que el usuario valida
- Necesita feedback consistente para ser efectivo
- Podría contaminar contexto si hay validaciones incorrectas
- Consume más tokens (mitigation: limitar ejemplos)

## 🧪 Testing

```bash
# Ver si se crea el archivo
ls data/queries_data/

# Validar JSON
cat data/queries_data/successful_queries.json | jq .

# Ver estadísticas en logs del app
# Buscar: "✓ Query guardada"
```

## 📋 Checklist

- [ ] Build compila sin errores: `mvn clean compile`
- [ ] Archivo de queries se crea automáticamente
- [ ] Botones Like/Dislike aparecen en respuestas de Claude
- [ ] Al hacer clic, aparece notificación "Query guardada"
- [ ] El archivo JSON se actualiza correctamente
- [ ] Claude recibe contexto (verificar en logs)

## 🎯 Casos de Uso

### Caso 1: Query Exitosa
```
Usuario: "Top 10 clientes por revenue"
Claude: "SELECT TOP 10 ..."
Usuario: 👍 Útil
→ Se guarda como referencia
```

### Caso 2: Query Fallida
```
Usuario: "Medidas que no existen"
Claude: "SELECT {Measures.[BadMeasure]} ..."
Usuario: 👎 Inútil
→ Se registra para análisis
```

### Caso 3: Segunda Iteración
```
Usuario: "Similar a un prompt anterior"
Claude: [usa ejemplos exitosos como referencia]
→ Genera mejor query inmediatamente
```

## 🔍 Troubleshooting

| Problema | Solución |
|----------|----------|
| No se guardan queries | Verificar permisos en `data/` |
| Botones no aparecen | Verificar imports de MessagePanel en ChatUI |
| Claude no usa contexto | Verificar buildSystemPrompt() en ClaudeService |
| JSON corrupto | Eliminar archivo y regenerar |

## 📞 Documentación Completa

- **QUERY_PERSISTENCE_README.md** → Arquitectura y detalles técnicos
- **INTEGRATION_GUIDE.java** → Cómo integrar en otros servicios
- **PROTOTIPO_PERSISTENCIA_QUERIES.md** → Overview y decisiones

---

**Estado**: ✅ READY TO USE  
**Último Update**: 27 Nov 2025
