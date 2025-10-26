import 'base_dynamic_response_dto.dart';

class ReportDownloadResponseDTO extends BaseDynamicResponseDTO {
  final String reportId;
  final String downloadUrl;

  const ReportDownloadResponseDTO({
    required super.type,
    super.summary,
    super.analysis,
    required this.reportId,
    required this.downloadUrl,
  });

  factory ReportDownloadResponseDTO.fromJson(Map<String, dynamic> json) {
    return ReportDownloadResponseDTO(
      type: ResponseType.REPORT_DOWNLOAD,
      summary: json['summary'],
      analysis: json['analysis'],
      reportId: json['reportId'] ?? '',
      downloadUrl: json['downloadUrl'] ?? '',
    );
  }

  @override
  Map<String, dynamic> toJson() => {
        'type': type.name,
        'summary': summary,
        'analysis': analysis,
        'reportId': reportId,
        'downloadUrl': downloadUrl,
      };
}
