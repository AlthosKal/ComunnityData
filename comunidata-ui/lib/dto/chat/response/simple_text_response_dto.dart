import 'base_dynamic_response_dto.dart';

class SimpleTextResponseDTO extends BaseDynamicResponseDTO {
  final String text;

  const SimpleTextResponseDTO({
    required super.type,
    super.summary,
    super.analysis,
    required this.text,
  });

  factory SimpleTextResponseDTO.fromJson(Map<String, dynamic> json) {
    return SimpleTextResponseDTO(
      type: ResponseType.SIMPLE_TEXT,
      summary: json['summary'],
      analysis: json['analysis'],
      text: json['text'] ?? '',
    );
  }

  @override
  Map<String, dynamic> toJson() => {
        'type': type.name,
        'summary': summary,
        'analysis': analysis,
        'text': text,
      };
}
