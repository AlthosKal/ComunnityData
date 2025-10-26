# 📊 Directorio de Reportes

Este directorio almacena los reportes financieros generados por el sistema KuentecoChat.

## 📁 Tipos de archivos generados:
- **PDF**: Reportes de análisis financiero, estados financieros, análisis de deudas
- **Excel**: Reportes tabulares con datos financieros detallados

## 🕐 Configuración:
- **Tiempo de expiración**: 24 horas (configurable en application.yml)
- **Limpieza automática**: Los archivos expirados se eliminan automáticamente
- **Patrones de nombres**: `reporte_{funcion}_{timestamp}.{extension}`

## 🚫 Control de versiones:
Los archivos de reportes están excluidos del control de versiones (.gitignore) para evitar:
- Contaminación del repositorio con archivos temporales
- Problemas de tamaño del repositorio
- Exposición accidental de datos sensibles

## 🔧 Configuración relacionada:
```yaml
app:
  reports:
    storage:
      path: ./reports  # Esta carpeta
    expiration:
      hours: 24
```
