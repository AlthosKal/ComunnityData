class ChatDTO {
  final String? conversationId;
  final String prompt;
  final String? userType;

  ChatDTO({
    this.conversationId,
    required this.prompt,
    this.userType,
  });

  bool needsConversationId() {
    return conversationId == null || conversationId!.trim().isEmpty;
  }

  Map<String, dynamic> toJson() {
    final json = {
      if (conversationId != null) 'conversationId': conversationId,
      'prompt': prompt,
      if (userType != null) 'userType': userType,
    };
    print('📤 ChatDTO: Enviando al backend: $json');
    return json;
  }
}