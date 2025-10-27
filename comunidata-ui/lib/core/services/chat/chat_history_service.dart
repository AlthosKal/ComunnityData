import 'package:frontend/core/services/api_client.dart';

import '../../../dto/chat/request/chat_history_dto.dart';

class ChatHistoryService {
  final _api = ApiClient();

  /// Obtiene todo el historial de conversaciones
  /// GET /api/v1/chat/history/all
  Future<List<ChatHistoryDTO>> getAllHistory() async {
      final response = await _api.getApp('/chat/history/all');
        final List<dynamic> data = response.data as List<dynamic>;
        return data
            .map((json) => ChatHistoryDTO.fromJson(json as Map<String, dynamic>))
            .toList();
  }

  /// Obtiene el historial de una conversación específica por ID
  /// GET /api/v1/chat/history/{conversationId}
  Future<List<ChatHistoryDTO>> getHistoryByConversationId(
      String conversationId) async {
      final response = await _api.getApp('/chat/history/$conversationId');
        final List<dynamic> data = response.data as List<dynamic>;
        return data
            .map((json) => ChatHistoryDTO.fromJson(json as Map<String, dynamic>))
            .toList();
  }

  /// Elimina el historial de una conversación específica
  /// DELETE /api/v1/chat/history/delete/{conversationId}
  Future<void> deleteHistoryByConversationId(String conversationId) async {
      await _api.deleteApp('/chat/history/delete/$conversationId');
  }
}
