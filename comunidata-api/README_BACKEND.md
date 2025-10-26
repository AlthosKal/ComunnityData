# ComuniData - Backend API Documentation

## Descripción General

ComuniData es un sistema inteligente que analiza automáticamente reportes ciudadanos sobre problemas comunitarios (educación, salud, medio ambiente, seguridad) usando IBM Granite, genera propuestas de solución concretas fundamentadas en los datos y utiliza RAG (Retrieval-Augmented Generation) para búsquedas semánticas.

---

## Arquitectura

### Stack Tecnológico

- **Framework**: Spring Boot 3.5.6
- **Java**: 17
- **Base de Datos**: MongoDB Atlas (con Vector Search)
- **IA**:
  - IBM Watsonx (Granite 3.3-8B-Instruct) - Filtrado y detección de sesgos
  - OpenAI GPT-5 - Chatbot y function calling
  - OpenAI text-embedding-3-small - Generación de embeddings
- **Resiliencia**: Resilience4j (Circuit Breaker, Retry)
- **Documentación**: Springdoc OpenAPI 3
- **Mapeo**: MapStruct
- **Generación de PDFs**: iText PDF, JFreeChart

### Arquitectura de Capas con Vertical Slicing

```
src/main/java/com/senasoft/comunidataapi/
├── chat/                    # Módulo de Chat con IA
│   ├── config/             # Configuración de IA (AiConfiguration)
│   ├── controller/         # REST Controllers
│   ├── service/            # Lógica de negocio
│   │   ├── chat/          # Servicios de chat
│   │   ├── function/      # Function Calling implementations
│   │   │   └── list/      # Funciones individuales
│   │   ├── report/        # Generación de reportes PDF
│   │   └── factory/       # Factories para respuestas dinámicas
│   ├── dto/               # DTOs de request/response
│   ├── entity/            # Entidades MongoDB
│   ├── repository/        # Repositorios MongoDB
│   └── mapper/            # MapStruct mappers
│
└── csv/                     # Módulo de procesamiento CSV
    ├── config/             # Configuración RAG (MongoVectorStoreConfig)
    ├── controller/         # REST Controllers CSV
    ├── service/            # Lógica de negocio
    │   ├── normalization/ # Normalización de datos
    │   └── processing/    # Procesamiento con IA
    ├── dto/               # DTOs de request/response
    ├── entity/            # Entidades MongoDB (CitizenReport)
    ├── repository/        # Repositorios MongoDB
    ├── enums/             # Enumeraciones
    └── mapper/            # MapStruct mappers
```

---

## Flujo de Procesamiento de Datos

### 1. Carga de CSV

```
POST /api/csv/upload
```

**Request:**
- `file`: Archivo CSV con estructura:
  ```
  ID,Nombre,Edad,Género,Ciudad,Comentario,Categoría del problema,
  Nivel de urgencia,Fecha del reporte,Acceso a internet,
  Atención previa del gobierno,Zona rural
  ```

**Proceso:**
1. **Normalización** (`CsvNormalizationServiceImpl`)
   - Valida y limpia datos
   - Edad: rango 0-120
   - Ciudad: capitalización correcta
   - Fechas: formato ISO (yyyy-MM-dd)
   - Comentarios: eliminación de caracteres especiales excesivos
   - Booleanos: conversión de "0"/"1", "Sí"/"No"

2. **Batch Processing con IBM Granite** (`GraniteProcessingServiceImpl`)
   - Divide en batches de 50 registros
   - Procesa 3 batches en paralelo usando `ExecutorService`
   - **Tiempo estimado**: 9 minutos para 10,000 registros
   - **Funcionalidades**:
     - Validación de categorías
     - Detección de sesgos (discriminación, información falsa, etc.)
     - Verificación de legitimidad del reporte
   - **Resiliencia**:
     - Circuit Breaker con Resilience4j
     - Retry con exponential backoff

3. **Generación de Embeddings** (`EmbeddingGenerationServiceImpl`)
   - Usa OpenAI `text-embedding-3-small`
   - Genera vectores de **1536 dimensiones**
   - Incluye contexto (categoría, ciudad, urgencia)
   - **Costo**: ~$0.03 USD por 1,000 embeddings

4. **Almacenamiento en MongoDB Atlas**
   - Guarda datos normalizados
   - Guarda embeddings vectoriales
   - Indexa para búsqueda vectorial (cosine similarity)

---

## Endpoints API

### Módulo CSV

#### 1. Cargar CSV
```http
POST /api/csv/upload
Content-Type: multipart/form-data

file: [archivo.csv]
procesarInmediatamente: true
```

**Response:**
```json
{
  "mensaje": "CSV procesado exitosamente",
  "totalRegistros": 10000,
  "registrosNormalizados": 10000,
  "registrosConError": 45,
  "batchId": "550e8400-e29b-41d4-a716-446655440000",
  "estadoProcesamiento": "PROCESAMIENTO_COMPLETO"
}
```

#### 2. Listar Reportes Procesados
```http
GET /api/csv/reports
```

**Response:**
```json
[
  {
    "id": "65f1234567890abcdef12345",
    "edad": 35,
    "ciudad": "Manizales",
    "comentario": "Falta de medicamentos en el hospital local",
    "categoriaProblema": "SALUD",
    "nivelUrgencia": "URGENTE",
    "fechaReporte": "2023-08-11",
    "atencionPreviaGobierno": false,
    "zona": "URBANA",
    "sesgoDetectado": false,
    "estadoProcesamiento": "COMPLETADO",
    "fechaCarga": "2024-01-15T10:30:00",
    "fechaProcesamiento": "2024-01-15T10:45:00"
  }
]
```

#### 3. Obtener Reporte por ID
```http
GET /api/csv/reports/{id}
```

#### 4. Estado del Batch
```http
GET /api/csv/reports/batch/{batchId}/status
```

**Response:**
```json
{
  "batchId": "550e8400-e29b-41d4-a716-446655440000",
  "totalRegistros": 10000,
  "registrosProcesados": 10000,
  "registrosCompletados": 9955,
  "registrosConError": 45,
  "porcentajeCompletado": 99.55,
  "estadoGeneral": "COMPLETADO",
  "tiempoEstimadoRestante": 0
}
```

#### 5. Exportar Reportes como CSV
```http
GET /api/csv/export
```

**Response:** Archivo CSV descargable

```http
POST /api/csv/export/selected
Content-Type: application/json

["id1", "id2", "id3"]
```

---

### Módulo Chat

#### 1. Enviar Mensaje al Chatbot
```http
POST /api/chat
Content-Type: application/json

{
  "prompt": "Dame todos los reportes de salud en Manizales",
  "conversationId": "optional-conversation-id"
}
```

**El chatbot puede invocar automáticamente las siguientes funciones:**

---

## Function Calling - Funciones Disponibles

### 1. filterByAgeFunction
Filtra reportes por rango de edad.

**Ejemplo de uso:**
```
Usuario: "Muéstrame reportes de personas entre 18 y 35 años"
```

**Parámetros:**
- `minAge`: int (edad mínima)
- `maxAge`: int (edad máxima)

---

### 2. filterByCityFunction
Filtra reportes por ciudad.

**Ejemplo de uso:**
```
Usuario: "Dame todos los reportes de Manizales"
```

**Parámetros:**
- `city`: string (nombre de la ciudad)

---

### 3. filterByCategoryProblemFunction
Filtra por categoría del problema.

**Ejemplo de uso:**
```
Usuario: "Muéstrame todos los problemas de salud"
```

**Parámetros:**
- `category`: string (Salud, Educación, Medio Ambiente, Seguridad)

---

### 4. filterByUrgencyLevelFunction
Filtra por nivel de urgencia.

**Ejemplo de uso:**
```
Usuario: "Dame los reportes urgentes"
```

**Parámetros:**
- `urgencyLevel`: string (Urgente, Alta, Media, Baja)

---

### 5. filterByGovernmentAttentionFunction
Filtra por atención previa del gobierno.

**Ejemplo de uso:**
```
Usuario: "Muéstrame problemas que no han sido atendidos"
```

**Parámetros:**
- `hasAttention`: boolean

---

### 6. filterByReportDateFunction
Filtra por rango de fechas.

**Ejemplo de uso:**
```
Usuario: "Dame reportes del último mes"
```

**Parámetros:**
- `startDate`: string (YYYY-MM-DD)
- `endDate`: string (YYYY-MM-DD)

---

### 7. filterByZoneFunction
Filtra por zona geográfica.

**Ejemplo de uso:**
```
Usuario: "Muéstrame reportes de zonas rurales"
```

**Parámetros:**
- `zone`: string (Rural, Urbana)

---

### 8. semanticSearchFunction (RAG)
Búsqueda semántica usando embeddings vectoriales.

**Ejemplo de uso:**
```
Usuario: "Busca reportes similares a 'falta de medicamentos en hospitales'"
```

**Parámetros:**
- `query`: string (consulta en lenguaje natural)
- `topK`: int (número de resultados, default: 5)

**Cómo funciona:**
1. Genera embedding de la consulta usando OpenAI
2. Busca en MongoDB Atlas usando similaridad coseno
3. Retorna los reportes más similares

---

### 9. generateReportFunction
Genera reportes PDF con análisis dinámico.

**Ejemplo de uso:**
```
Usuario: "Genera un reporte PDF de análisis por categoría"
```

**Parámetros:**
- `analysisType`: string (general, por_categoria, por_ciudad, por_urgencia)
- `filters`: string (JSON con filtros adicionales)

---

## Base de Datos - Esquema MongoDB

### Colección: `citizen_reports`

```javascript
{
  _id: ObjectId,
  edad: Int32,
  ciudad: String,
  comentario: String,
  categoriaProblema: String, // SALUD, EDUCACION, MEDIO_AMBIENTE, SEGURIDAD
  nivelUrgencia: String, // URGENTE, ALTA, MEDIA, BAJA
  fechaReporte: ISODate,
  atencionPreviaGobierno: Boolean,
  zona: String, // RURAL, URBANA

  // Procesamiento IA
  sesgoDetectado: Boolean,
  descripcionSesgo: String,
  categoriaOriginal: String,

  // RAG - Vector Embedding
  embedding: [Double], // Array de 1536 dimensiones

  // Control
  estadoProcesamiento: String, // PENDIENTE, NORMALIZANDO, PROCESANDO_IA, GENERANDO_EMBEDDINGS, COMPLETADO, ERROR
  fechaCarga: ISODate,
  fechaProcesamiento: ISODate,
  errorMensaje: String,

  // Metadatos
  batchId: String,
  batchIndex: Int32,
  comentarioOriginal: String
}
```

### Índices

1. **Índices normales**:
   - `ciudad`
   - `categoriaProblema`
   - `fechaReporte`
   - `zona`
   - `estadoProcesamiento`
   - `batchId`

2. **Índice Vectorial** (MongoDB Atlas Search):
   - Nombre: `report_embeddings_index`
   - Campo: `embedding`
   - Tipo: `knnVector`
   - Dimensiones: 1536
   - Similaridad: `cosine`

---

## Configuración

### Variables de Entorno (.env)

```env
# MongoDB
MONGO_URI=mongodb+srv://user:pass@cluster.mongodb.net/comunidata?retryWrites=true&w=majority

# OpenAI
OPENAI_KEY=sk-...
OPENAI_MODEL=gpt-4-turbo

# IBM Watsonx
IBM_WATSONX_API_KEY=your_api_key
IBM_WATSONX_PROJECT_ID=your_project_id

# URLs
WEB_URL=http://localhost:3000
MOBILE_URL=http://localhost:8080
```

### Application.yml (ya configurado)

- Spring AI con OpenAI y Watsonx
- MongoDB Atlas con auto-index-creation
- Resilience4j Circuit Breaker y Rate Limiter
- Cache con Caffeine
- Configuración de reportes PDF

---

## Ejecución

### Requisitos Previos

1. Java 17
2. Maven 3.8.3+
3. MongoDB Atlas configurado con Search Index
4. API Keys de OpenAI e IBM Watsonx

### Compilar

```bash
mvn clean install
```

### Ejecutar

```bash
mvn spring-boot:run
```

### Acceder

- **API**: http://localhost:8080/api
- **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **API Docs**: http://localhost:8080/api/api-docs

---

## Principios de Diseño Aplicados

### SOLID

- **S**ingle Responsibility: Cada servicio tiene una única responsabilidad
- **O**pen/Closed: Extensible mediante nuevas funciones sin modificar existentes
- **L**iskov Substitution: Interfaces bien definidas
- **I**nterface Segregation: Interfaces específicas por funcionalidad
- **D**ependency Inversion: Inyección de dependencias con Spring

### Patrones de Diseño

- **Strategy**: Diferentes estrategias de normalización
- **Factory**: `DynamicResponseFactory` para crear respuestas
- **Repository**: Acceso a datos con Spring Data
- **Circuit Breaker**: Resiliencia con Resilience4j
- **Batch Processing**: Procesamiento en lotes con paralelización

### Buenas Prácticas

- DTOs para separar capa de presentación
- Mappers con MapStruct para conversiones
- Logging estructurado con SLF4J
- Validación con Bean Validation
- Documentación con Javadoc y OpenAPI
- Manejo centralizado de excepciones

---

## Costos Estimados (10,000 reportes)

| Servicio | Uso | Costo Aprox. |
|----------|-----|--------------|
| IBM Granite | 200 llamadas (batches de 50) | $1.00 USD |
| OpenAI Embeddings | 10,000 embeddings | $0.30 USD |
| OpenAI GPT-5 | Variable por consultas | $0.20 USD |
| MongoDB Atlas | 1GB storage + queries | Gratis (tier M0) |
| **TOTAL** | Procesamiento inicial | **~$1.50 USD** |

---

## Próximos Pasos

1. ✅ Módulo CSV completado
2. ✅ Function Calling implementado
3. ✅ RAG configurado
4. ⏳ Adaptar generación de reportes PDF para métricas ciudadanas
5. ⏳ Implementar análisis predictivo con IA
6. ⏳ Dashboard de visualización en Flutter

---

## Soporte

Para dudas o problemas:
1. Revisa `SETUP_RAG.md` para configuración de RAG
2. Verifica logs en consola
3. Consulta Swagger UI para documentación de endpoints

---

**Desarrollado con ❤️ usando Spring Boot, MongoDB Atlas, OpenAI y IBM Watsonx**
