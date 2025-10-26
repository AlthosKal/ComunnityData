class ChatHistoryDTO {
  final String conversationId;
  final String prompt;
  final String? response;
  final DateTime date;

  ChatHistoryDTO({
    required this.conversationId,
    required this.prompt,
    this.response,
    required this.date,
  });

  factory ChatHistoryDTO.fromJson(Map<String, dynamic> json) {
    return ChatHistoryDTO(
      conversationId: json['conversationId'],
      prompt: json['prompt'],
      response: json['response'],
      date: DateTime.parse(json['date']),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'conversationId': conversationId,
      'prompt': prompt,
      if (response != null) 'response': response,
      'date': date.toIso8601String(),
    };
  }
}