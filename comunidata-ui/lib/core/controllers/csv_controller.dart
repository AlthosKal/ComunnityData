import 'dart:typed_data';
import 'package:flutter/foundation.dart';
import '../services/csv_service.dart';

// Import condicional para descarga de archivos
import '../../widgets/utils/csv_download_stub.dart'
    if (dart.library.html) '../../widgets/utils/csv_download_web.dart'
    if (dart.library.io) '../../widgets/utils/csv_download_mobile.dart' as download;

class CsvController extends ChangeNotifier {
  final CsvService _csvService;

  // Estado
  List<Map<String, dynamic>> _processedCsvs = [];
  Map<String, dynamic>? _currentCsv;

  bool _isLoading = false;
  bool _isUploading = false;
  bool _isDownloading = false;
  String? _errorMessage;
  String? _successMessage;
  String? _selectedFileName;
  String? _uploadedCsvId;

  // Getters
  List<Map<String, dynamic>> get processedCsvs => List.unmodifiable(_processedCsvs);
  Map<String, dynamic>? get currentCsv => _currentCsv;

  bool get isLoading => _isLoading;
  bool get isUploading => _isUploading;
  bool get isDownloading => _isDownloading;
  String? get errorMessage => _errorMessage;
  String? get successMessage => _successMessage;
  String? get selectedFileName => _selectedFileName;
  String? get uploadedCsvId => _uploadedCsvId;

  bool get hasProcessedCsvs => _processedCsvs.isNotEmpty;
  bool get hasError => _errorMessage != null;

  CsvController({CsvService? csvService})
      : _csvService = csvService ?? CsvService();

  /// Obtiene todos los CSVs procesados
  Future<void> fetchAllProcessedCsvs() async {
    try {
      print('📋 CsvController: Obteniendo CSVs procesados');
      _setLoading(true);
      _clearError();

      _processedCsvs = await _csvService.getAllProcessedCsvs();

      print('✓ CsvController: ${_processedCsvs.length} CSVs obtenidos');
      notifyListeners();
    } catch (e) {
      print('✗ CsvController: Error al obtener CSVs: $e');
      _setError('Error al cargar los CSVs: ${e.toString()}');
    } finally {
      _setLoading(false);
    }
  }

  /// Obtiene un CSV procesado por ID
  Future<void> fetchProcessedCsvById(String id) async {
    try {
      print('📄 CsvController: Obteniendo CSV $id');
      _setLoading(true);
      _clearError();

      _currentCsv = await _csvService.getProcessedCsvById(id);

      print('✓ CsvController: CSV obtenido');
      notifyListeners();
    } catch (e) {
      print('✗ CsvController: Error al obtener CSV: $e');
      _setError('Error al cargar el CSV: ${e.toString()}');
    } finally {
      _setLoading(false);
    }
  }

  /// Importar/Subir datos desde archivo CSV
  Future<bool> importData(dynamic file) async {
    print('📤 CsvController: Iniciando importación...');
    _isUploading = true;
    _clearError();
    notifyListeners();

    try {
      // Obtener el nombre del archivo y bytes
      String fileName = 'archivo.csv';
      Uint8List? bytes;

      // Compatibilidad con PlatformFile (file_picker)
      if (file.name != null) {
        fileName = file.name;
      }

      // Leer el contenido del archivo
      if (file.bytes != null) {
        bytes = file.bytes;
      } else if (file.path != null && file.path.isNotEmpty) {
        // Fallback para plataformas que usan path
        fileName = file.path.split('/').last;
        bytes = await file.readAsBytes();
      } else {
        throw Exception('No se pudo leer el archivo');
      }

      setSelectedFile(fileName);

      if (bytes == null) {
        throw Exception('El archivo está vacío');
      }

      // Validar contenido CSV localmente
      final content = String.fromCharCodes(bytes);
      if (!_validateCsvContent(content)) {
        throw Exception('El archivo no tiene un formato CSV válido');
      }

      // Validar con el backend (opcional)
      try {
        print('✔️ CsvController: Validando CSV con backend');
        await _csvService.validateCsv(bytes, fileName);
      } catch (e) {
        print('⚠️ CsvController: Validación del backend falló: $e');
        // Continuar con la subida aunque falle la validación
      }

      // Subir el archivo al backend
      final response = await _csvService.uploadCsvForProcessing(bytes, fileName);

      // Guardar el ID del CSV subido
      _uploadedCsvId = response['id'] ?? response['csvId'];

      _setSuccess('Datos importados exitosamente desde $fileName');
      print('✓ CsvController: Importación completada');

      // Actualizar lista de CSVs procesados
      await fetchAllProcessedCsvs();

      return true;
    } catch (e) {
      print('✗ CsvController: Error en importación: $e');
      _setError('Error al importar datos: ${e.toString()}');
      setSelectedFile(null);
      return false;
    } finally {
      _isUploading = false;
      notifyListeners();
    }
  }

  /// Exportar datos a CSV (genera CSV de ejemplo o descarga del backend)
  Future<void> exportData({String? csvId, Map<String, dynamic>? filters}) async {
    print('📥 CsvController: Iniciando exportación...');
    _isDownloading = true;
    _clearError();
    notifyListeners();

    try {
      Uint8List csvBytes;
      String filename;

      if (csvId != null) {
        // Descargar CSV específico del backend
        csvBytes = await _csvService.downloadProcessedCsv(csvId);
        filename = 'csv_procesado_$csvId.csv';
      } else {
        // Exportar datos generales del backend
        try {
          csvBytes = await _csvService.exportDataAsCsv(filters: filters);
          filename = 'datos_comunidata_${DateTime.now().millisecondsSinceEpoch}.csv';
        } catch (e) {
          print('⚠️ CsvController: Backend no disponible, generando CSV de ejemplo');
          // Fallback: generar CSV de ejemplo
          final csvContent = _generateSampleCsv();
          csvBytes = Uint8List.fromList(csvContent.codeUnits);
          filename = 'datos_ejemplo_${DateTime.now().millisecondsSinceEpoch}.csv';
        }
      }

      // Usar descarga multiplataforma
      download.downloadFile(csvBytes, filename);

      _setSuccess('Datos exportados exitosamente: $filename');
      print('✓ CsvController: Exportación completada');
    } catch (e) {
      print('✗ CsvController: Error en exportación: $e');
      _setError('Error al exportar datos: ${e.toString()}');
    } finally {
      _isDownloading = false;
      notifyListeners();
    }
  }

  /// Descarga un CSV procesado específico
  Future<void> downloadProcessedCsv(String id, {String? customFilename}) async {
    await exportData(csvId: id);
  }

  /// Elimina un CSV procesado
  Future<bool> deleteProcessedCsv(String id) async {
    try {
      print('🗑️ CsvController: Eliminando CSV $id');
      _setLoading(true);
      _clearError();

      await _csvService.deleteProcessedCsv(id);

      // Remover de la lista local
      _processedCsvs.removeWhere((csv) => csv['id'] == id);

      if (_currentCsv?['id'] == id) {
        _currentCsv = null;
      }

      _setSuccess('CSV eliminado exitosamente');
      print('✓ CsvController: CSV eliminado');
      notifyListeners();
      return true;
    } catch (e) {
      print('✗ CsvController: Error al eliminar CSV: $e');
      _setError('Error al eliminar el CSV: ${e.toString()}');
      return false;
    } finally {
      _setLoading(false);
    }
  }

  /// Valida un archivo CSV
  Future<bool> validateCsvFile(dynamic file) async {
    try {
      print('✔️ CsvController: Validando archivo CSV');
      _clearError();

      // Obtener bytes del archivo
      String fileName = 'archivo.csv';
      Uint8List? bytes;

      if (file.name != null) {
        fileName = file.name;
      }

      if (file.bytes != null) {
        bytes = file.bytes;
      } else if (file.path != null && file.path.isNotEmpty) {
        fileName = file.path.split('/').last;
        bytes = await file.readAsBytes();
      }

      if (bytes == null) {
        throw Exception('No se pudo leer el archivo');
      }

      // Validar localmente
      final content = String.fromCharCodes(bytes);
      if (!_validateCsvContent(content)) {
        _setError('El archivo no tiene un formato CSV válido');
        return false;
      }

      // Validar con el backend
      final result = await _csvService.validateCsv(bytes, fileName);

      final isValid = result['valid'] ?? true;
      if (!isValid) {
        _setError(result['message'] ?? 'CSV inválido');
        return false;
      }

      _setSuccess('CSV válido');
      return true;
    } catch (e) {
      print('✗ CsvController: Error al validar CSV: $e');
      _setError('Error al validar el archivo: ${e.toString()}');
      return false;
    }
  }

  /// Validar contenido CSV localmente
  bool _validateCsvContent(String content) {
    if (content.isEmpty) return false;

    // Verificar que tenga al menos una línea con separadores
    final lines = content.split('\n');
    if (lines.isEmpty) return false;

    // Verificar que la primera línea tenga al menos un separador
    final firstLine = lines[0].trim();
    if (!firstLine.contains(',') && !firstLine.contains(';')) return false;

    return true;
  }

  /// Generar CSV de ejemplo (fallback cuando el backend no está disponible)
  String _generateSampleCsv() {
    final buffer = StringBuffer();

    // Encabezados
    buffer.writeln('ID,Fecha,Descripción,Categoría,Monto,Tipo');

    // Datos de ejemplo
    buffer.writeln('1,2025-01-15,Análisis de datos cliente A,Consultoría,1500.00,Ingreso');
    buffer.writeln('2,2025-01-16,Procesamiento dataset municipal,Procesamiento,2300.00,Ingreso');
    buffer.writeln('3,2025-01-17,Informe mensual sector salud,Reportes,1800.00,Ingreso');
    buffer.writeln('4,2025-01-18,Servidor cloud mensual,Infraestructura,150.00,Gasto');
    buffer.writeln('5,2025-01-19,Licencias software análisis,Software,500.00,Gasto');

    return buffer.toString();
  }

  /// Establece el archivo seleccionado
  void setSelectedFile(String? fileName) {
    _selectedFileName = fileName;
    notifyListeners();
  }

  /// Limpia mensajes de error y éxito
  void clearMessages() {
    _errorMessage = null;
    _successMessage = null;
    notifyListeners();
  }

  /// Limpia todos los datos locales
  void clearData() {
    _errorMessage = null;
    _successMessage = null;
    _selectedFileName = null;
    _uploadedCsvId = null;
    _currentCsv = null;
    _processedCsvs.clear();
    notifyListeners();
  }

  /// Limpia el error
  void clearError() {
    _clearError();
  }

  // Métodos privados auxiliares
  void _setLoading(bool loading) {
    _isLoading = loading;
    notifyListeners();
  }

  void _setError(String error) {
    _errorMessage = error;
    _successMessage = null;
    notifyListeners();
  }

  void _setSuccess(String success) {
    _successMessage = success;
    _errorMessage = null;
    notifyListeners();
  }

  void _clearError() {
    _errorMessage = null;
  }

  @override
  void dispose() {
    print('🔚 CsvController: Disposing');
    _csvService.dispose();
    super.dispose();
  }
}
