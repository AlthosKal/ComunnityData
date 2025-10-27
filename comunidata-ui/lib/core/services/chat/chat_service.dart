import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:frontend/core/services/api_client.dart';

import '../../../dto/chat/request/chat_dto.dart';
import '../../../dto/chat/response/base_dynamic_response_dto.dart' hide ResponseType;

class ChatService {

  final _api = ApiClient();
  /// Envía un mensaje al chat con IA y retorna una respuesta dinámica
  /// POST /api/v1/chat
  Future<BaseDynamicResponseDTO> sendChatMessage(ChatDTO dto) async {
      final response = await _api.postApp('/chat', dto.toJson());
      // Parsear la respuesta dinámica según el tipo
      return BaseDynamicResponseDTO.fromJson(response.data);
  }

  /// Descarga un reporte generado
  /// GET /api/v1/reports/download/{reportId}
  Future<Uint8List> downloadReport(String reportId) async {
    final endpoint = '/reports/download/$reportId';
      final response = await _api.getApp(endpoint, options: Options(
        responseType: ResponseType.bytes,
        headers: {
          'Accept':'application/pdf',
        },
      ),
      );
      return Uint8List.fromList(response.data);
  }
}
