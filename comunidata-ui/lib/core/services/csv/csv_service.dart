import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:frontend/core/services/api_client.dart';
import 'package:frontend/dto/chat/response/citizen_report_response_dto.dart';

class CsvService {
  final _api = ApiClient();

  /// Obtiene todos los CSVs procesados
  /// GET /api/v1/csv
  Future<List<CitizenReportResponseDTO>> getAllProcessedCsvs() async {
      final response = await _api.getApp('/csv');
      return CitizenReportResponseDTO().fromJson(response.data);
  }

  /// Obtiene un CSV procesado por ID
  /// GET /api/v1/csv/{id}
  Future<Map<String, dynamic>> getProcessedCsvById(String id) async {
      final response = await _api.getApp('/csv/$id');
      return response.data as Map<String, dynamic>;
  }

  /// Sube un archivo CSV para procesamiento
  /// POST /api/v1/csv
  Future<Map<String, dynamic>> uploadCsvForProcessing(
    Uint8List fileBytes,
    String fileName, {
    bool procesarInmediatamente = true,
  }) async {
    final formData = FormData.fromMap({
        'file': MultipartFile.fromBytes(
          fileBytes,
          filename: fileName,
        ),
        'procesarInmediatamente': procesarInmediatamente,
      });

      final response = await _api.postAppWithOptions(
        "/csv/add",
        formData,
        Options(
          contentType: 'multipart/form-data',
        ),
      );
      return response.data as Map<String, dynamic>;
  }

}
