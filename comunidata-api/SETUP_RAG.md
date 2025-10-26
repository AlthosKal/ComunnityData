# Setup de MongoDB Atlas Vector Search para RAG

## Pasos Obligatorios

### 1. Crear Índice Vectorial en MongoDB Atlas

Antes de poder usar las búsquedas semánticas (RAG), **debes crear un Search Index vectorial** en MongoDB Atlas.

#### Paso a paso:

1. Ve a tu cluster en [MongoDB Atlas](https://cloud.mongodb.com)
2. Selecciona tu base de datos
3. Ve a la colección `citizen_reports`
4. En el menú lateral, selecciona **"Search"**
5. Haz clic en **"Create Search Index"**
6. Selecciona **"JSON Editor"**
7. Pega la siguiente configuración:

```json
{
  "mappings": {
    "dynamic": true,
    "fields": {
      "embedding": {
        "type": "knnVector",
        "dimensions": 1536,
        "similarity": "cosine"
      }
    }
  }
}
```

8. Nombra el índice exactamente como: **`report_embeddings_index`**
9. Haz clic en **"Create Search Index"**

⚠️ **IMPORTANTE**: La creación del índice puede tardar varios minutos. Espera a que el estado sea "Active" antes de intentar búsquedas semánticas.

---

### 2. Variables de Entorno Requeridas

Asegúrate de tener las siguientes variables en tu archivo `.env`:

```env
# MongoDB Atlas
MONGO_URI=mongodb+srv://usuario:password@cluster.mongodb.net/comunidata?retryWrites=true&w=majority

# OpenAI (para embeddings y GPT-5)
OPENAI_KEY=sk-...
OPENAI_MODEL=gpt-4-turbo

# IBM Watsonx (para IBM Granite)
IBM_WATSONX_API_KEY=tu_api_key
IBM_WATSONX_PROJECT_ID=tu_project_id

# Frontend URLs
WEB_URL=http://localhost:3000
MOBILE_URL=http://localhost:8080
```

---

### 3. Verificar que el índice está activo

Una vez creado el índice, puedes verificarlo con:

```bash
# Usando mongosh o desde Spring Boot
# El VectorStore automáticamente usará el índice cuando esté activo
```

---

## Uso del Sistema RAG

### Búsqueda Semántica desde el Chat

El chatbot puede usar búsqueda semántica automáticamente:

```
Usuario: "Busca reportes sobre falta de medicamentos en hospitales"

GPT-5: Llamará a semanticSearchFunction
       → Generará embedding de la consulta
       → Busca en MongoDB Atlas usando similaridad coseno
       → Retorna los 5 reportes más similares
```

### Funciones Disponibles

El sistema tiene las siguientes funciones que GPT-5 puede invocar:

1. **filterByAgeFunction** - Filtra por rango de edad
2. **filterByCityFunction** - Filtra por ciudad
3. **filterByCategoryProblemFunction** - Filtra por categoría (Salud, Educación, etc.)
4. **filterByUrgencyLevelFunction** - Filtra por urgencia
5. **filterByGovernmentAttentionFunction** - Filtra por atención del gobierno
6. **filterByReportDateFunction** - Filtra por fechas
7. **filterByZoneFunction** - Filtra por zona (Rural/Urbana)
8. **semanticSearchFunction** - Búsqueda semántica con RAG
9. **generateReportFunction** - Genera reportes PDF

---

## Arquitectura del Flujo de Datos

```
1. Usuario carga CSV
   ↓
2. Normalización (CsvNormalizationService)
   ↓
3. Procesamiento con IBM Granite (batch de 50)
   - Validación de categorías
   - Detección de sesgos
   ↓
4. Generación de Embeddings (OpenAI text-embedding-3-small)
   ↓
5. Almacenamiento en MongoDB Atlas
   - Datos del reporte
   - Vector embedding (1536 dimensiones)
   ↓
6. Búsquedas Semánticas (RAG)
   - MongoDB Atlas Vector Search
   - Similaridad coseno
```

---

## Solución de Problemas

### Error: "Index not found"
- Verifica que el índice `report_embeddings_index` está creado
- Asegúrate de que está en estado "Active"
- Espera unos minutos después de crearlo

### Error: "Embedding dimension mismatch"
- El modelo text-embedding-3-small genera 1536 dimensiones
- El índice debe estar configurado con `dimensions: 1536`

### Error: "No results found"
- Verifica que hay reportes con embeddings en la BD
- Ajusta el `similarityThreshold` en SemanticSearchFunction

---

## Costos Estimados

Para 10,000 reportes:

- **IBM Granite (filtrado)**: ~200 llamadas (batches de 50) = ~$1.00 USD
- **OpenAI Embeddings**: 10,000 embeddings = ~$0.30 USD
- **GPT-5 (queries)**: Variable según uso

**Total aproximado**: $1.50 USD por procesamiento inicial
