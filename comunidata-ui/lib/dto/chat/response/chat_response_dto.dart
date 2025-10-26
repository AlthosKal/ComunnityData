class ChatResponseDTO {
  final String? conversationId;
  final String response;
  final String? analysisType;
  final Map<String, dynamic>? metadata;
  final DateTime timestamp;

  ChatResponseDTO({
    this.conversationId,
    required this.response,
    this.analysisType,
    this.metadata,
    DateTime? timestamp,
  }) : timestamp = timestamp ?? DateTime.now();

  factory ChatResponseDTO.fromJson(Map<String, dynamic> json) {
    return ChatResponseDTO(
      conversationId: json['conversationId'],
      response: json['response'] ?? json['message'] ?? '',
      analysisType: json['analysisType'],
      metadata: json['metadata'],
      timestamp: json['timestamp'] != null
          ? DateTime.parse(json['timestamp'])
          : DateTime.now(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      if (conversationId != null) 'conversationId': conversationId,
      'response': response,
      if (analysisType != null) 'analysisType': analysisType,
      if (metadata != null) 'metadata': metadata,
      'timestamp': timestamp.toIso8601String(),
    };
  }

  bool hasConversationId() {
    return conversationId != null && conversationId!.isNotEmpty;
  }

  @override
  String toString() {
    return 'ChatResponseDTO(conversationId: $conversationId, response: ${response.substring(0, response.length > 50 ? 50 : response.length)}..., analysisType: $analysisType)';
  }
}
