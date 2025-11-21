# 🐛 Problema: Chatbot se Congela en Consultas Encadenadas

## 🔍 Diagnóstico

Cuando preguntas algo como "cuál es el mejor vino", Claude:

1. ✅ Primera consulta: Obtiene tipos de vino (Blend, Varietal)
2. 🔄 Intenta segunda consulta: Ver vinos individuales
3. ❌ **Se congela** esperando respuesta

---

## ⚠️ Causas Posibles:

1. **Timeout muy corto** (60 segundos) para consultas complejas
2. **Claude intenta múltiples tool calls** en una misma respuesta
3. **MCP se desconecta** o tarda mucho
4. **Límite de tokens** en la conversación

---

## ✅ Soluciones Aplicadas:

### 1️⃣ Aumentar Timeouts
Ya actualicé los timeouts a:
- Connect: 120 segundos
- Read: 120 segundos  
- Write: 120 segundos
- Call total: 180 segundos

### 2️⃣ Recomendaciones de Uso:

**❌ Evita preguntas muy abiertas:**
```
"Cuál es el mejor vino" (requiere múltiples consultas)
```

**✅ Sé más específico:**
```
"Muéstrame todos los vinos con sus ventas"
"Dame los top 5 vinos por revenue"
"Cuáles son los vinos más vendidos"
```

**✅ O da la consulta directa:**
```
"Ejecuta: SELECT {Measures.[Units Sold]} ON COLUMNS, NON EMPTY {Wines.children} ON ROWS FROM [Wines]"
```

---

## 🧪 Pruebas Sugeridas para Cubo Wines:

### Exploración Básica:

```
1. "Muéstrame las dimensiones del cubo Wines"
   → SELECT {Measures.Members} ON COLUMNS FROM [Wines]

2. "Dame todos los vinos disponibles"
   → SELECT {Measures.[Units Sold]} ON COLUMNS, NON EMPTY {Wines.children} ON ROWS FROM [Wines]

3. "Muéstrame ventas por tipo de vino"
   → SELECT {Measures.[Units Sold]} ON COLUMNS, NON EMPTY {[Types of Wine].children} ON ROWS FROM [Wines]

4. "Dame ventas por destino"
   → SELECT {Measures.[Units Sold]} ON COLUMNS, NON EMPTY {Destinations.children} ON ROWS FROM [Wines]
```

### Consultas Específicas:

```
5. "Muéstrame los 10 vinos más vendidos"
   → SELECT TopCount({Wines.children}, 10, Measures.[Units Sold]) ON ROWS FROM [Wines]

6. "Ventas de vinos Blend vs Varietal"
   → SELECT {Measures.[Units Sold]} ON COLUMNS, {[Types of Wine].children} ON ROWS FROM [Wines]

7. "Ventas por vendedor"
   → SELECT {Measures.[Units Sold]} ON COLUMNS, NON EMPTY {Salesmen.children} ON ROWS FROM [Wines]
```

---

## 🔧 Si Sigue Congelándose:

### Opción 1: Reiniciar el Chatbot
```bash
Ctrl+C  (detener)
start.bat  (reiniciar)
```

### Opción 2: Hacer Consultas Más Simples
Divide tu pregunta en pasos:
```
Tú: "Dame todos los vinos"
Claude: [muestra lista]

Tú: "Ahora ordénalos por ventas"
Claude: [ordena]
```

### Opción 3: Usar Modo Consola
```bash
start.bat
Opción [2] Modo Consola
```
En consola es más fácil ver dónde se atascó.

---

## 📊 Estructura del Cubo Wines:

**Dimensiones:**
- Date (Fechas)
- Wines (Vinos individuales)
- Types of Wine (Tipos: Blend, Varietal)
- Customers (Clientes)
- Salesmen (Vendedores)
- Destinations (Destinos)

**Medidas Comunes:**
- Units Sold (Unidades vendidas)
- Revenue (Ingresos)
- Cost (Costo)
- Profit (Ganancia)

---

## 💡 Tip: Pregunta en 2 Pasos

**Paso 1: Exploración**
```
"Dame la lista de todos los vinos del cubo Wines"
```

**Paso 2: Análisis**  
```
"De esos vinos, muéstrame el que tiene más ventas"
```

Esto evita que Claude intente hacer todo en una sola conversación.

---

## 🚀 Compilar Cambios:

```bash
cd D:\MCP_PRUEBA\chatbot-ia
mvn clean compile
start.bat
```

---
