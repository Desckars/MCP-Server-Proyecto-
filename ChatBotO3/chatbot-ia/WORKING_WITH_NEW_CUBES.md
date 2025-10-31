# 🔧 GUÍA: Trabajar con Cubos Nuevos

## ❌ Problema Detectado:

Cuando preguntas sobre un cubo que Claude no conoce (ej: Wines), asume nombres de medidas incorrectos:

```
"Muéstrame ventas del cubo Wines"
→ Claude asume: Measures.[Sales]
→ Error: "Sales is not valid"
```

---

## ✅ Solución: Consulta en 2 Pasos

### **Paso 1: Explorar el Cubo**

Primero pregunta qué medidas tiene:

```
"Muéstrame las medidas del cubo Wines"
```

O ejecuta directamente:

```
"Ejecuta: SELECT {Measures.Members} ON COLUMNS FROM [Wines]"
```

**Resultado esperado:**
```
Measures:
- Units Sold
- Revenue  
- Cost
- Profit
...
```

### **Paso 2: Usar las Medidas Correctas**

Ahora que sabes los nombres reales:

```
"Muéstrame todos los vinos con Units Sold"
"Dame Revenue por tipo de vino"
```

---

## 📋 Workflow Recomendado para Cualquier Cubo Nuevo:

```
1️⃣ Explorar medidas:
   "Muéstrame las medidas del cubo [NombreCubo]"
   
2️⃣ Explorar dimensiones principales:
   "Muéstrame las dimensiones del cubo [NombreCubo]"
   
3️⃣ Ver miembros de una dimensión:
   "Muéstrame los miembros de la dimensión Wines"
   
4️⃣ Hacer consultas específicas:
   "Dame Units Sold por Wines del cubo Wines"
```

---

## 🧪 Consultas de Exploración Directas:

### Para Cubo Wines:

```sql
-- Ver todas las medidas
SELECT {Measures.Members} ON COLUMNS FROM [Wines]

-- Ver todos los vinos
SELECT {Wines.Members} ON COLUMNS FROM [Wines]

-- Ver tipos de vino
SELECT {[Types of Wine].Members} ON COLUMNS FROM [Wines]

-- Ver destinos
SELECT {Destinations.Members} ON COLUMNS FROM [Wines]

-- Ver clientes
SELECT {Customers.Members} ON COLUMNS FROM [Wines]

-- Ver vendedores  
SELECT {Salesmen.Members} ON COLUMNS FROM [Wines]
```

---

## 💡 Tips:

1. **Siempre explora primero** cuando trabajes con un cubo nuevo
2. **Copia los nombres exactos** de medidas y dimensiones
3. **Si Claude se equivoca**, corrígelo:
   ```
   "No, la medida se llama 'Units Sold', no 'Sales'"
   ```

4. **Sé específico** con los nombres:
   ```
   ❌ "Dame ventas"  (ambiguo)
   ✅ "Dame Units Sold"  (específico)
   ```

---

## 🎯 Ejemplo Completo: Cubo Wines

```
# Paso 1: Explorar
Tú: "Ejecuta: SELECT {Measures.Members} ON COLUMNS FROM [Wines]"
Claude: [muestra: Units Sold, Revenue, Cost, Profit]

# Paso 2: Ver estructura
Tú: "Ejecuta: SELECT {Wines.Members} ON COLUMNS FROM [Wines]"
Claude: [muestra lista de vinos]

# Paso 3: Consulta específica
Tú: "Dame Units Sold por cada vino"
Claude: SELECT {Measures.[Units Sold]} ON COLUMNS, NON EMPTY {Wines.children} ON ROWS FROM [Wines]
```

---

## 🔄 Si Claude se Equivoca:

**Escenario:** Claude usó una medida incorrecta

**Solución 1:** Corrígelo directamente
```
"Inténtalo de nuevo pero usa 'Units Sold' en lugar de 'Sales'"
```

**Solución 2:** Dale la consulta correcta
```
"Ejecuta: SELECT {Measures.[Units Sold]} ON COLUMNS, NON EMPTY {Wines.children} ON ROWS FROM [Wines]"
```

---

## 📚 Cubos Conocidos y sus Medidas:

### Cubo Demo:
- Units Sold, Cost, Revenue, Commissions, Discount

### Cubo Wines:
- (Explorar primero con SELECT {Measures.Members} ON COLUMNS FROM [Wines])

### Otros Cubos:
- Siempre explora primero antes de consultar

---

¿Quieres que agregue un **modo de auto-exploración** donde Claude primero explora automáticamente el cubo antes de consultar? 🤔
