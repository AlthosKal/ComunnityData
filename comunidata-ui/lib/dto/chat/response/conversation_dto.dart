class ConversationDTO {
  final String id;
  final String title;
  final String? subtitle;
  final DateTime lastMessageDate;
  final int messageCount;
  final bool isActive;

  ConversationDTO({
    required this.id,
    required this.title,
    this.subtitle,
    required this.lastMessageDate,
    this.messageCount = 0,
    this.isActive = false,
  });

  factory ConversationDTO.fromJson(Map<String, dynamic> json) {
    return ConversationDTO(
      id: json['id'] ?? json['conversationId'] ?? '',
      title: json['title'] ?? 'Sin título',
      subtitle: json['subtitle'],
      lastMessageDate: json['lastMessageDate'] != null
          ? DateTime.parse(json['lastMessageDate'])
          : DateTime.now(),
      messageCount: json['messageCount'] ?? 0,
      isActive: json['isActive'] ?? false,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'title': title,
      if (subtitle != null) 'subtitle': subtitle,
      'lastMessageDate': lastMessageDate.toIso8601String(),
      'messageCount': messageCount,
      'isActive': isActive,
    };
  }

  /// Crea una copia con algunos campos actualizados
  ConversationDTO copyWith({
    String? id,
    String? title,
    String? subtitle,
    DateTime? lastMessageDate,
    int? messageCount,
    bool? isActive,
  }) {
    return ConversationDTO(
      id: id ?? this.id,
      title: title ?? this.title,
      subtitle: subtitle ?? this.subtitle,
      lastMessageDate: lastMessageDate ?? this.lastMessageDate,
      messageCount: messageCount ?? this.messageCount,
      isActive: isActive ?? this.isActive,
    );
  }

  /// Obtiene un subtítulo formateado basado en la fecha
  String getFormattedSubtitle() {
    if (subtitle != null && subtitle!.isNotEmpty) {
      return subtitle!;
    }

    final now = DateTime.now();
    final difference = now.difference(lastMessageDate);

    if (difference.inMinutes < 1) {
      return 'Ahora';
    } else if (difference.inMinutes < 60) {
      return 'Hace ${difference.inMinutes} min';
    } else if (difference.inHours < 24) {
      return 'Hace ${difference.inHours} hora${difference.inHours > 1 ? 's' : ''}';
    } else if (difference.inDays < 7) {
      return 'Hace ${difference.inDays} día${difference.inDays > 1 ? 's' : ''}';
    } else if (difference.inDays < 30) {
      final weeks = (difference.inDays / 7).floor();
      return 'Hace $weeks semana${weeks > 1 ? 's' : ''}';
    } else {
      final months = (difference.inDays / 30).floor();
      return 'Hace $months mes${months > 1 ? 'es' : ''}';
    }
  }

  @override
  String toString() {
    return 'ConversationDTO(id: $id, title: $title, messageCount: $messageCount, isActive: $isActive)';
  }
}
