import 'dart:typed_data';
import 'package:dio/dio.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';

class CsvService {
  final Dio _dio;

  CsvService({String? baseUrl})
      : _dio = Dio(
          BaseOptions(
            baseUrl: baseUrl ?? dotenv.env['API_BASE_URL'] ?? 'http://localhost:8080/api/v1',
            connectTimeout: const Duration(seconds: 60),
            receiveTimeout: const Duration(seconds: 60),
            headers: {
              'Content-Type': 'application/json',
              'Accept': 'application/json',
            },
          ),
        ) {
    // Interceptor para logging
    _dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          print('🌐 [CsvService ${options.method}] ${options.path}');
          return handler.next(options);
        },
        onResponse: (response, handler) {
          print('✅ [CsvService] Response [${response.statusCode}]');
          return handler.next(response);
        },
        onError: (error, handler) {
          print('❌ [CsvService] Error [${error.response?.statusCode}]: ${error.message}');
          return handler.next(error);
        },
      ),
    );
  }

  /// Obtiene todos los CSVs procesados
  /// GET /api/v1/csv
  Future<List<Map<String, dynamic>>> getAllProcessedCsvs() async {
    try {
      print('📋 CsvService: Obteniendo todos los CSVs procesados');
      final response = await _dio.get('/csv');

      if (response.data is List) {
        return List<Map<String, dynamic>>.from(response.data);
      }
      return [];
    } on DioException catch (e) {
      print('✗ CsvService: Error en getAllProcessedCsvs');
      _handleDioError(e, 'getAllProcessedCsvs');
      rethrow;
    }
  }

  /// Obtiene un CSV procesado por ID
  /// GET /api/v1/csv/{id}
  Future<Map<String, dynamic>> getProcessedCsvById(String id) async {
    try {
      print('📄 CsvService: Obteniendo CSV con ID: $id');
      final response = await _dio.get('/csv/$id');
      return response.data as Map<String, dynamic>;
    } on DioException catch (e) {
      print('✗ CsvService: Error en getProcessedCsvById');
      _handleDioError(e, 'getProcessedCsvById');
      rethrow;
    }
  }

  /// Sube un archivo CSV para procesamiento
  /// POST /api/v1/csv
  Future<Map<String, dynamic>> uploadCsvForProcessing(
    Uint8List fileBytes,
    String fileName, {
    bool procesarInmediatamente = true,
  }) async {
    try {
      print('📤 CsvService: Subiendo archivo CSV: $fileName');

      final formData = FormData.fromMap({
        'file': MultipartFile.fromBytes(
          fileBytes,
          filename: fileName,
        ),
        'procesarInmediatamente': procesarInmediatamente,
      });

      final response = await _dio.post(
        '/csv',
        data: formData,
        options: Options(
          contentType: 'multipart/form-data',
        ),
      );

      print('✓ CsvService: CSV subido exitosamente');
      return response.data as Map<String, dynamic>;
    } on DioException catch (e) {
      print('✗ CsvService: Error en uploadCsvForProcessing');
      _handleDioError(e, 'uploadCsvForProcessing');
      rethrow;
    }
  }

  /// Descarga un CSV procesado por ID (usa endpoint de exportación)
  /// POST /api/v1/csv/export con IDs específicos
  Future<Uint8List> downloadProcessedCsv(String id) async {
    try {
      print('📥 CsvService: Descargando CSV con ID: $id');
      final response = await _dio.post<List<int>>(
        '/csv/export',
        data: [id], // Array con un solo ID
        options: Options(responseType: ResponseType.bytes),
      );

      print('✓ CsvService: CSV descargado');
      return Uint8List.fromList(response.data ?? []);
    } on DioException catch (e) {
      print('✗ CsvService: Error en downloadProcessedCsv');
      _handleDioError(e, 'downloadProcessedCsv');
      rethrow;
    }
  }

  /// Exporta datos como CSV desde el backend
  /// GET /api/v1/csv/export
  Future<Uint8List> exportDataAsCsv({Map<String, dynamic>? filters}) async {
    try {
      print('📥 CsvService: Exportando datos como CSV');
      final response = await _dio.get<List<int>>(
        '/csv/export',
        queryParameters: filters,
        options: Options(responseType: ResponseType.bytes),
      );

      print('✓ CsvService: Datos exportados');
      return Uint8List.fromList(response.data ?? []);
    } on DioException catch (e) {
      print('✗ CsvService: Error en exportDataAsCsv');
      _handleDioError(e, 'exportDataAsCsv');
      rethrow;
    }
  }

  /// Elimina un CSV procesado por ID
  /// DELETE /api/v1/csv/{id}
  Future<void> deleteProcessedCsv(String id) async {
    try {
      print('🗑️ CsvService: Eliminando CSV con ID: $id');
      await _dio.delete('/csv/$id');
      print('✓ CsvService: CSV eliminado');
    } on DioException catch (e) {
      print('✗ CsvService: Error en deleteProcessedCsv');
      _handleDioError(e, 'deleteProcessedCsv');
      rethrow;
    }
  }

  /// Obtiene el estado de procesamiento de un batch
  /// GET /api/v1/csv/batch/{batchId}/status
  Future<Map<String, dynamic>> getBatchStatus(String batchId) async {
    try {
      print('📊 CsvService: Obteniendo estado del batch: $batchId');
      final response = await _dio.get('/csv/batch/$batchId/status');

      print('✓ CsvService: Estado del batch obtenido');
      return response.data as Map<String, dynamic>;
    } on DioException catch (e) {
      print('✗ CsvService: Error en getBatchStatus');
      _handleDioError(e, 'getBatchStatus');
      rethrow;
    }
  }

  /// Valida un archivo CSV antes de subirlo (DEPRECADO - el backend no tiene este endpoint)
  /// La validación ahora se hace durante la subida con procesarInmediatamente=false
  @Deprecated('El backend no tiene endpoint de validación. Usar uploadCsvForProcessing con procesarInmediatamente=false')
  Future<Map<String, dynamic>> validateCsv(
    Uint8List fileBytes,
    String fileName,
  ) async {
    // Hacer validación local básica
    print('⚠️ CsvService: Validación local de CSV: $fileName');

    try {
      // Validar que el archivo no esté vacío
      if (fileBytes.isEmpty) {
        throw Exception('El archivo está vacío');
      }

      // Validar que sea texto CSV (contenga comas)
      final content = String.fromCharCodes(fileBytes);
      if (!content.contains(',')) {
        throw Exception('El archivo no parece ser un CSV válido');
      }

      print('✓ CsvService: Validación local completada');
      return {
        'valid': true,
        'message': 'Archivo válido (validación local)',
        'fileName': fileName,
        'size': fileBytes.length,
      };
    } catch (e) {
      print('✗ CsvService: Error en validación local');
      rethrow;
    }
  }

  /// Manejo centralizado de errores de Dio
  void _handleDioError(DioException e, String methodName) {
    String errorMessage;

    switch (e.type) {
      case DioExceptionType.connectionTimeout:
        errorMessage = 'Tiempo de conexión agotado';
        break;
      case DioExceptionType.sendTimeout:
        errorMessage = 'Tiempo de envío agotado';
        break;
      case DioExceptionType.receiveTimeout:
        errorMessage = 'Tiempo de recepción agotado';
        break;
      case DioExceptionType.badResponse:
        final statusCode = e.response?.statusCode;
        final message = e.response?.data?['message'] ??
            e.response?.data?['error'] ??
            e.response?.statusMessage;

        if (statusCode == 400) {
          errorMessage = 'Archivo CSV inválido: ${message ?? 'formato incorrecto'}';
        } else if (statusCode == 404) {
          errorMessage = 'CSV no encontrado';
        } else if (statusCode == 413) {
          errorMessage = 'El archivo es demasiado grande';
        } else if (statusCode == 415) {
          errorMessage = 'Tipo de archivo no soportado';
        } else if (statusCode == 500) {
          errorMessage = 'Error interno del servidor';
        } else {
          errorMessage = 'Error del servidor ($statusCode): ${message ?? 'Error desconocido'}';
        }
        break;
      case DioExceptionType.cancel:
        errorMessage = 'Solicitud cancelada';
        break;
      case DioExceptionType.connectionError:
        errorMessage = 'Error de conexión. Verifica tu internet';
        break;
      default:
        errorMessage = e.message ?? 'Error desconocido';
    }

    print('❌ CsvService.$methodName: $errorMessage');
    throw Exception(errorMessage);
  }

  /// Cierra el cliente Dio
  void dispose() {
    print('🔚 CsvService: Disposing');
    _dio.close();
  }
}
