import 'dart:typed_data';
import 'package:dio/dio.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import '../../dto/chat/request/chat_dto.dart';
import '../../dto/chat/request/chat_history_dto.dart';
import '../../dto/chat/response/base_dynamic_response_dto.dart' hide ResponseType;
import '../../dto/chat/response/chat_response_dto.dart';

class ChatService {
  final Dio _dio;

  ChatService({String? baseUrl}) : _dio = Dio(
    BaseOptions(
      baseUrl: baseUrl ?? dotenv.env['API_BASE_URL'] ?? 'http://localhost:8080/api/v1',
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
          print('🌐 [${options.method}] ${options.path}');
          print('📤 Request Data: ${options.data}');
          return handler.next(options);
        },
        onResponse: (response, handler) {
          print('✅ Response [${response.statusCode}]: ${response.data}');
          return handler.next(response);
        },
        onError: (error, handler) {
          print('❌ Error [${error.response?.statusCode}]: ${error.message}');
          return handler.next(error);
        },
      ),
    );
  }

  /// Envía un mensaje al chat con IA y retorna una respuesta dinámica
  /// POST /api/v1/chat
  Future<BaseDynamicResponseDTO> sendChatMessage(ChatDTO chatDTO) async {
    try {
      print('📨 ChatService: Enviando mensaje...');
      final response = await _dio.post(
        '/chat',
        data: chatDTO.toJson(),
      );

      print('✓ ChatService: Respuesta recibida');
      final responseData = response.data as Map<String, dynamic>;

      // Parsear la respuesta dinámica según el tipo
      return BaseDynamicResponseDTO.fromJson(responseData);
    } on DioException catch (e) {
      print('✗ ChatService: Error en sendChatMessage');
      _handleDioError(e, 'sendChatMessage');
      rethrow;
    }
  }

  /// Envía un mensaje al chat con IA (versión legacy que retorna Map)
  /// POST /api/v1/chat
  @Deprecated('Usar sendChatMessage que retorna BaseDynamicResponseDTO')
  Future<Map<String, dynamic>> sendChatMessageRaw(ChatDTO chatDTO) async {
    try {
      print('📨 ChatService: Enviando mensaje (raw)...');
      final response = await _dio.post(
        '/chat',
        data: chatDTO.toJson(),
      );

      print('✓ ChatService: Respuesta recibida');
      return response.data as Map<String, dynamic>;
    } on DioException catch (e) {
      print('✗ ChatService: Error en sendChatMessageRaw');
      _handleDioError(e, 'sendChatMessageRaw');
      rethrow;
    }
  }

  /// Obtiene el historial de conversación
  /// GET /api/v1/chat/history/{conversationId}
  Future<List<ChatHistoryDTO>> getChatHistory(String conversationId) async {
    try {
      print('📚 ChatService: Obteniendo historial de $conversationId');
      final response = await _dio.get('/chat/history/$conversationId');

      final List<dynamic> data = response.data as List<dynamic>;
      return data.map((json) => ChatHistoryDTO.fromJson(json)).toList();
    } on DioException catch (e) {
      print('✗ ChatService: Error en getChatHistory');
      _handleDioError(e, 'getChatHistory');
      rethrow;
    }
  }

  /// Obtiene todas las conversaciones del usuario
  /// GET /api/v1/chat/history/all
  Future<List<Map<String, dynamic>>> getConversations() async {
    try {
      print('💬 ChatService: Obteniendo todas las conversaciones');
      final response = await _dio.get('/chat/history/all');

      // El backend retorna un objeto con 'data' que contiene el array
      if (response.data is Map && response.data['data'] != null) {
        return List<Map<String, dynamic>>.from(response.data['data']);
      }
      return List<Map<String, dynamic>>.from(response.data);
    } on DioException catch (e) {
      print('✗ ChatService: Error en getConversations');
      _handleDioError(e, 'getConversations');
      rethrow;
    }
  }

  /// Crea una nueva conversación (DEPRECADO - el backend genera IDs automáticamente)
  /// El backend crea automáticamente un conversationId al enviar el primer mensaje
  @Deprecated('El backend genera conversationId automáticamente al enviar mensajes sin conversationId')
  Future<String> createNewConversation() async {
    // Generar un ID temporal en el cliente
    // El backend lo reemplazará por uno definitivo al enviar el primer mensaje
    final tempId = 'temp_${DateTime.now().millisecondsSinceEpoch}';
    print('ℹ️ ChatService: Generando conversationId temporal: $tempId');
    return tempId;
  }

  /// Elimina una conversación
  /// DELETE /api/v1/chat/history/delete/{conversationId}
  Future<void> deleteConversation(String conversationId) async {
    try {
      print('🗑️ ChatService: Eliminando conversación $conversationId');
      await _dio.delete('/chat/history/delete/$conversationId');
      print('✓ ChatService: Conversación eliminada');
    } on DioException catch (e) {
      print('✗ ChatService: Error en deleteConversation');
      _handleDioError(e, 'deleteConversation');
      rethrow;
    }
  }

  /// Descarga un reporte generado
  /// GET /api/v1/reports/download/{reportId}
  Future<Uint8List> downloadReport(String reportId) async {
    try {
      print('📥 ChatService: Descargando reporte $reportId');
      final response = await _dio.get<List<int>>(
        '/reports/download/$reportId',
        options: Options(responseType: ResponseType.bytes),
      );

      print('✓ ChatService: Reporte descargado');
      return Uint8List.fromList(response.data ?? []);
    } on DioException catch (e) {
      print('✗ ChatService: Error en downloadReport');
      _handleDioError(e, 'downloadReport');
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
        final message = e.response?.data?['message'] ?? e.response?.data?['error'];
        errorMessage = 'Error del servidor ($statusCode): ${message ?? 'Error desconocido'}';
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

    print('❌ $methodName: $errorMessage');
    throw Exception(errorMessage);
  }

  /// Cierra el cliente Dio
  void dispose() {
    _dio.close();
  }
}
