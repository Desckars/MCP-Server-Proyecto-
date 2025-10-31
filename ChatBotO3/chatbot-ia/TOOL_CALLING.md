# 🎯 CLAUDE CON TOOL CALLING - Cómo Funciona

## 🚀 ¿Qué Cambió?

### **ANTES (Sistema Forzado):**
```
Usuario → Detecta palabras clave → Fuerza MDX → Error si no aplica
```

### **AHORA (Claude Decide):**
```
Usuario → Claude analiza → ¿Necesito MDX? 
                             ↓ SÍ
                        Llama executeCustomMdxQuery tool
                             ↓ NO
                        Responde normalmente
```

---

## 🧠 Cómo Funciona Igual que Yo

Ahora Claude en tu chatbot tiene acceso a **tools** (herramientas), exactamente como yo tengo acceso a `web_search`, `filesystem`, etc.

### **Tool Disponible:**
```
executeCustomMdxQuery(mdxQuery: string)
  
Descripción: Ejecuta consultas MDX contra cubo Demo
  
Ejemplos incluidos:
- SELECT {Measures.[Units Sold]} ON COLUMNS, NON EMPTY {Location.children} ON ROWS FROM [Demo]
- SELECT {Measures.[Cost], Measures.[Units Sold]} ON COLUMNS, {Customers.[Major Accounts]} ON ROWS FROM [Demo]
```

---

## 💬 Ejemplos de Uso

### **Ejemplo 1: Conversación Normal**
```
Tú: Hola, ¿cómo estás?

Claude (piensa): "No necesito ningún tool para esto"
                 ↓
Claude: ¡Hola! Soy Claude. ¿En qué puedo ayudarte?
```

### **Ejemplo 2: Pregunta sobre MDX**
```
Tú: Explícame qué es MDX

Claude (piensa): "Es una pregunta educativa, no necesito consultar datos"
                 ↓
Claude: MDX (Multidimensional Expressions) es un lenguaje...
```

### **Ejemplo 3: Consulta de Datos**
```
Tú: Dame las ventas por ubicación

Claude (piensa): "Necesito datos del cubo, usaré executeCustomMdxQuery"
                 ↓
Claude llama: executeCustomMdxQuery("SELECT {Measures.[Units Sold]} ON COLUMNS, NON EMPTY {Location.children} ON ROWS FROM [Demo]")
                 ↓
MCP O3 ejecuta y retorna resultados
                 ↓
Claude: Aquí están las ventas por ubicación:
        Location | Units Sold
        ---------|------------
        France   | 12,450
        ...
```

### **Ejemplo 4: Precio del Dólar**
```
Tú: ¿Cuál es el precio del dólar en Uruguay?

Claude (piensa): "Esto no es sobre el cubo Demo, no necesito tools"
                 ↓
Claude: Según mi conocimiento hasta enero 2025, el dólar...
```

---

## ✅ Ventajas

1. **Natural:** Pregunta como hablarías conmigo
2. **Inteligente:** Claude decide si necesita el tool o no
3. **Sin Errores:** No fuerza MDX cuando no corresponde
4. **Automático:** Los ejemplos del MCP guían a Claude
5. **Extensible:** Fácil agregar más tools en el futuro

---

## 🧪 Pruebas Recomendadas

Ejecuta el chatbot y prueba:

### ✅ **Deberían funcionar PERFECTO:**

1. **Conversación normal:**
   ```
   "Hola, ¿cómo estás?"
   "Explícame qué es OLAP"
   "¿Cuál es la capital de Francia?"
   ```

2. **Consultas MDX:**
   ```
   "Dame ventas por ubicación"
   "Muéstrame costos para Major Accounts"
   "Cuánto se vendió en France"
   ```

3. **Mixtas:**
   ```
   "¿Qué dimensiones tiene el cubo? Y luego dame ventas por Location"
   ```

---

## 🔍 Lo que Verás en la Consola

```
╔════════════════════════════════════════╗
║  CLAUDE CON TOOL CALLING               ║
╚════════════════════════════════════════╝
📝 Mensaje: Dame ventas por ubicación
🔧 Claude quiere usar tool: executeCustomMdxQuery
📝 Con parámetros: {"mdxQuery":"SELECT {Measures.[Units Sold]} ON COLUMNS, NON EMPTY {Location.children} ON ROWS FROM [Demo]"}
📊 Ejecutando MDX: SELECT {Measures.[Units Sold]} ON COLUMNS, NON EMPTY {Location.children} ON ROWS FROM [Demo]
✅ Resultado del tool obtenido
🔄 Enviando resultado a Claude para interpretación...
```

---

## 💰 Costo

- **Sin tool:** ~$0.001 por mensaje simple
- **Con tool:** ~$0.005 por consulta MDX completa
- **Tu presupuesto:** $5 USD = ~900 consultas

---

## 🚀 Cómo Ejecutar

```bash
cd D:\MCP_PRUEBA\chatbot-ia
mvn clean compile
start.bat
```

Selecciona [1] Interfaz Gráfica

---

## 🎯 Diferencia Clave

**ANTES:**
- ❌ Sistema decide con palabras clave
- ❌ Muchos falsos positivos
- ❌ Forzaba MDX innecesariamente

**AHORA:**
- ✅ Claude decide inteligentemente
- ✅ Sin falsos positivos
- ✅ Usa MDX solo cuando corresponde
- ✅ Exactamente como yo funciono

---

¡Pruébalo ahora! 🚀
