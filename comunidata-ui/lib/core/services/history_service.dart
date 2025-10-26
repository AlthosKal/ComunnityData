import 'package:dio/dio.dart';
import '../../dto/chat/request/chat_history_dto.dart';

class HistoryService {
  final Dio _dio;

  HistoryService({String? baseUrl})
      : _dio = Dio(
          BaseOptions(
            baseUrl: baseUrl ?? 'http://localhost:8080/api/v1',
            connectTimeout: const Duration(seconds: 30),
            receiveTimeout: const Duration(seconds: 30),
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
          print('🌐 [HistoryService ${options.method}] ${options.path}');
          if (options.data != null) {
            print('📤 Request Data: ${options.data}');
          }
          return handler.next(options);
        },
        onResponse: (response, handler) {
          print('✅ [HistoryService] Response [${response.statusCode}]');
          return handler.next(response);
        },
        onError: (error, handler) {
          print('❌ [HistoryService] Error [${error.response?.statusCode}]: ${error.message}');
          return handler.next(error);
        },
      ),
    );
  }

  /// Obtiene todo el historial de conversaciones
  /// GET /api/v1/chat/history/all
  Future<List<ChatHistoryDTO>> getAllHistory() async {
    try {
      print('📚 HistoryService: Obteniendo todo el historial');
      final response = await _dio.get('/chat/history/all');

      if (response.data is List) {
        final List<dynamic> data = response.data as List<dynamic>;
        final historyList = data
            .map((json) => ChatHistoryDTO.fromJson(json as Map<String, dynamic>))
            .toList();

        print('✓ HistoryService: ${historyList.length} registros de historial obtenidos');
        return historyList;
      } else {
        throw Exception('Formato de respuesta inválido: se esperaba una lista');
      }
    } on DioException catch (e) {
      print('✗ HistoryService: Error en getAllHistory');
      _handleDioError(e, 'getAllHistory');
      rethrow;
    }
  }

  /// Obtiene el historial de una conversación específica por ID
  /// GET /api/v1/chat/history/{conversationId}
  Future<List<ChatHistoryDTO>> getHistoryByConversationId(
      String conversationId) async {
    try {
      print('📖 HistoryService: Obteniendo historial de conversación $conversationId');
      final response = await _dio.get('/chat/history/$conversationId');

      if (response.data is List) {
        final List<dynamic> data = response.data as List<dynamic>;
        final historyList = data
            .map((json) => ChatHistoryDTO.fromJson(json as Map<String, dynamic>))
            .toList();

        print('✓ HistoryService: ${historyList.length} mensajes obtenidos');
        return historyList;
      } else {
        throw Exception('Formato de respuesta inválido: se esperaba una lista');
      }
    } on DioException catch (e) {
      print('✗ HistoryService: Error en getHistoryByConversationId');
      _handleDioError(e, 'getHistoryByConversationId');
      rethrow;
    }
  }

  /// Elimina el historial de una conversación específica
  /// DELETE /api/v1/chat/history/delete/{conversationId}
  Future<void> deleteHistoryByConversationId(String conversationId) async {
    try {
      print('🗑️ HistoryService: Eliminando historial de conversación $conversationId');
      await _dio.delete('/chat/history/delete/$conversationId');
      print('✓ HistoryService: Historial eliminado exitosamente');
    } on DioException catch (e) {
      print('✗ HistoryService: Error en deleteHistoryByConversationId');
      _handleDioError(e, 'deleteHistoryByConversationId');
      rethrow;
    }
  }

  /// Busca en el historial por palabra clave (método opcional adicional)
  /// Nota: Este método asume que el endpoint existe en el backend
  Future<List<ChatHistoryDTO>> searchHistory(String keyword) async {
    try {
      print('🔍 HistoryService: Buscando en historial: "$keyword"');
      final response = await _dio.get(
        '/chat/history/search',
        queryParameters: {'q': keyword},
      );

      if (response.data is List) {
        final List<dynamic> data = response.data as List<dynamic>;
        final historyList = data
            .map((json) => ChatHistoryDTO.fromJson(json as Map<String, dynamic>))
            .toList();

        print('✓ HistoryService: ${historyList.length} resultados encontrados');
        return historyList;
      } else {
        throw Exception('Formato de respuesta inválido: se esperaba una lista');
      }
    } on DioException catch (e) {
      print('✗ HistoryService: Error en searchHistory');
      _handleDioError(e, 'searchHistory');
      rethrow;
    }
  }

  /// Obtiene estadísticas del historial (método opcional adicional)
  /// Nota: Este método asume que el endpoint existe en el backend
  Future<Map<String, dynamic>> getHistoryStats() async {
    try {
      print('📊 HistoryService: Obteniendo estadísticas del historial');
      final response = await _dio.get('/chat/history/stats');

      print('✓ HistoryService: Estadísticas obtenidas');
      return response.data as Map<String, dynamic>;
    } on DioException catch (e) {
      print('✗ HistoryService: Error en getHistoryStats');
      _handleDioError(e, 'getHistoryStats');
      rethrow;
    }
  }

  /// Exporta el historial de una conversación a formato JSON
  Future<String> exportHistoryAsJson(String conversationId) async {
    try {
      print('📥 HistoryService: Exportando historial de $conversationId a JSON');
      final history = await getHistoryByConversationId(conversationId);

      final jsonList = history.map((item) => item.toJson()).toList();
      final jsonString = jsonList.toString();

      print('✓ HistoryService: Historial exportado');
      return jsonString;
    } catch (e) {
      print('✗ HistoryService: Error en exportHistoryAsJson: $e');
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

        if (statusCode == 404) {
          errorMessage = 'Historial no encontrado';
        } else if (statusCode == 403) {
          errorMessage = 'No tienes permisos para acceder a este historial';
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

    print('❌ HistoryService.$methodName: $errorMessage');
    throw Exception(errorMessage);
  }

  /// Cierra el cliente Dio
  void dispose() {
    print('🔚 HistoryService: Disposing');
    _dio.close();
  }
}
