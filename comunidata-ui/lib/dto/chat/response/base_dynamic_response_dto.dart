import 'package:meta/meta.dart';
import 'report_download_response_dto.dart';
import 'simple_text_response_dto.dart';

/// Enum equivalente a ResponseType en Java
enum ResponseType {
  SIMPLE_TEXT,
  REPORT_DOWNLOAD,
}

/// Clase base abstracta para las respuestas dinámicas
@immutable
abstract class BaseDynamicResponseDTO {
  final ResponseType type;
  final String? summary;
  final String? analysis;

  const BaseDynamicResponseDTO({
    required this.type,
    this.summary,
    this.analysis,
  });

  /// Fábrica para deserializar según el campo "type"
  factory BaseDynamicResponseDTO.fromJson(Map<String, dynamic> json) {
    final typeStr = json['type'] as String?;
    final responseType = ResponseType.values.firstWhere(
      (e) => e.name == typeStr,
      orElse: () => throw Exception('Tipo de respuesta no reconocido: $typeStr'),
    );

    switch (responseType) {
      case ResponseType.SIMPLE_TEXT:
        return SimpleTextResponseDTO.fromJson(json);
      case ResponseType.REPORT_DOWNLOAD:
        return ReportDownloadResponseDTO.fromJson(json);
    }
  }

  Map<String, dynamic> toJson();
}
