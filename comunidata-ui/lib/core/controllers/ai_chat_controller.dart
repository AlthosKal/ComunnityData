import 'dart:async';
import 'dart:typed_data';
import 'package:flutter/material.dart';
import '../services/chat_service.dart';
import '../../dto/chat/request/chat_dto.dart';
import '../../dto/chat/request/chat_history_dto.dart';
import '../../dto/chat/response/base_dynamic_response_dto.dart';
import '../../dto/chat/response/simple_text_response_dto.dart';
import '../../dto/chat/response/report_download_response_dto.dart';

class AIChatController with ChangeNotifier {
  final ChatService _chatService;
  final List<ChatMessage> _messages = [];
  final List<Map<String, dynamic>> _conversations = [];

  bool _isLoading = false;
  bool _isTyping = false;
  String? _currentConversationId;
  String? _errorMessage;
  Timer? _typingTimer;

  List<ChatMessage> get messages => List.unmodifiable(_messages);
  List<Map<String, dynamic>> get conversations => List.unmodifiable(_conversations);
  bool get isLoading => _isLoading;
  bool get isTyping => _isTyping;
  String? get currentConversationId => _currentConversationId;
  String? get errorMessage => _errorMessage;
  bool get hasActiveConversation => _currentConversationId != null;

  AIChatController({ChatService? chatService})
    : _chatService = chatService ?? ChatService() {
    _initializeDefaultConversations();
  }

  void _initializeDefaultConversations() {
    _conversations.addAll([
      {
        'id': '1',
        'title': 'Análisis de ventas Q1',
        'subtitle': 'Hace 2 días',
        'isActive': false,
      },
      {
        'id': '2',
        'title': 'Tendencias de usuarios',
        'subtitle': 'Hace 1 semana',
        'isActive': false,
      },
    ]);
  }

  /// Inicia una nueva conversación
  Future<void> startNewConversation() async {
    try {
      print('🆕 AIChatController: Iniciando nueva conversación');
      _setLoading(true);
      _clearError();

      // Crear conversación en el backend (si está disponible)
      // String? conversationId = await _chatService.createNewConversation();

      // Por ahora, generar ID local
      final conversationId = DateTime.now().millisecondsSinceEpoch.toString();
      _currentConversationId = conversationId;
      _messages.clear();

      // Desactivar todas las conversaciones
      for (var conversation in _conversations) {
        conversation['isActive'] = false;
      }

      // Agregar nueva conversación
      _conversations.insert(0, {
        'id': conversationId,
        'title': 'Nueva conversación',
        'subtitle': 'Ahora',
        'isActive': true,
      });

      print('✓ AIChatController: Nueva conversación creada: $conversationId');
      notifyListeners();
    } catch (e) {
      print('✗ AIChatController: Error al crear conversación: $e');
      _setError('Error al crear conversación: ${e.toString()}');
    } finally {
      _setLoading(false);
    }
  }

  /// Carga una conversación existente
  Future<void> loadConversation(String conversationId) async {
    try {
      print('📂 AIChatController: Cargando conversación $conversationId');
      _setLoading(true);
      _clearError();

      _currentConversationId = conversationId;

      // Actualizar estado activo
      for (var conversation in _conversations) {
        conversation['isActive'] = conversation['id'] == conversationId;
      }

      // Intentar cargar historial desde el backend
      try {
        final history = await _chatService.getChatHistory(conversationId);
        _messages.clear();
        for (var historyItem in history) {
          _messages.add(ChatMessage(
            content: historyItem.prompt,
            isUser: true,
            timestamp: historyItem.date,
          ));
          if (historyItem.response != null) {
            _messages.add(ChatMessage(
              content: historyItem.response!,
              isUser: false,
              timestamp: historyItem.date,
            ));
          }
        }
        print('✓ AIChatController: Historial cargado desde backend');
      } catch (e) {
        print('⚠ AIChatController: No se pudo cargar historial del backend, usando datos locales');
        _loadLocalConversationData(conversationId);
      }

      notifyListeners();
    } catch (e) {
      print('✗ AIChatController: Error al cargar conversación: $e');
      _setError('Error al cargar conversación: ${e.toString()}');
    } finally {
      _setLoading(false);
    }
  }

  /// Carga datos de conversación locales (fallback)
  void _loadLocalConversationData(String conversationId) {
    _messages.clear();
    if (conversationId == '1') {
      _messages.addAll([
        ChatMessage(
          content: '¿Puedes analizar las ventas del primer trimestre?',
          isUser: true,
          timestamp: DateTime.now().subtract(const Duration(minutes: 30)),
        ),
        ChatMessage(
          content: 'He analizado los datos de ventas del Q1. Las ventas totales fueron de \$2.5M, con un crecimiento del 15% respecto al trimestre anterior. Los productos más vendidos fueron:\n\n• Producto A: 35% de participación\n• Producto B: 28% de participación\n• Producto C: 20% de participación\n\n¿Te gustaría que profundice en algún aspecto específico?',
          isUser: false,
          timestamp: DateTime.now().subtract(const Duration(minutes: 29)),
        ),
      ]);
    } else if (conversationId == '2') {
      _messages.addAll([
        ChatMessage(
          content: 'Analiza las tendencias de usuarios activos',
          isUser: true,
          timestamp: DateTime.now().subtract(const Duration(days: 7)),
        ),
        ChatMessage(
          content: 'Basándome en los datos de usuarios activos, he identificado las siguientes tendencias:\n\n📈 **Crecimiento mensual**: +12%\n👥 **Usuarios nuevos**: 2,500\n🔄 **Tasa de retención**: 78%\n📱 **Dispositivo preferido**: Mobile (65%)\n\nLas horas pico de actividad son entre 7-9 PM. ¿Quieres ver más detalles sobre alguna métrica?',
          isUser: false,
          timestamp: DateTime.now().subtract(const Duration(days: 7)),
        ),
      ]);
    }
  }

  /// Elimina una conversación
  Future<void> deleteConversation(String conversationId) async {
    try {
      print('🗑️ AIChatController: Eliminando conversación $conversationId');
      _clearError();

      // Intentar eliminar del backend
      try {
        await _chatService.deleteConversation(conversationId);
        print('✓ AIChatController: Conversación eliminada del backend');
      } catch (e) {
        print('⚠ AIChatController: No se pudo eliminar del backend: $e');
      }

      // Eliminar localmente
      _conversations.removeWhere((conversation) => conversation['id'] == conversationId);

      if (_currentConversationId == conversationId) {
        _currentConversationId = null;
        _messages.clear();
      }

      notifyListeners();
    } catch (e) {
      print('✗ AIChatController: Error al eliminar conversación: $e');
      _setError('Error al eliminar conversación: ${e.toString()}');
    }
  }

  /// Envía un mensaje al chat con soporte para respuestas dinámicas
  Future<void> sendMessage(String message, {String? userType}) async {
    if (message.trim().isEmpty || _isLoading) return;

    print('💬 AIChatController: Enviando mensaje');
    _clearError();

    // Agregar mensaje del usuario
    final userMessage = ChatMessage(
      content: message,
      isUser: true,
      timestamp: DateTime.now(),
    );
    _messages.add(userMessage);

    // Actualizar título de conversación si es nueva
    if (_currentConversationId != null) {
      final conversationIndex = _conversations.indexWhere(
        (conv) => conv['id'] == _currentConversationId,
      );
      if (conversationIndex != -1 && _conversations[conversationIndex]['title'] == 'Nueva conversación') {
        _conversations[conversationIndex]['title'] = message.length > 30
            ? '${message.substring(0, 30)}...'
            : message;
      }
    }

    _isLoading = true;
    _isTyping = true;
    notifyListeners();

    try {
      // Crear DTO para enviar al backend
      final chatDTO = ChatDTO(
        conversationId: _currentConversationId,
        prompt: message,
        userType: userType,
      );

      // Intentar obtener respuesta del backend
      try {
        print('🌐 AIChatController: Llamando al backend...');
        final response = await _chatService.sendChatMessage(chatDTO);

        // Procesar respuesta dinámica según el tipo
        await _processDynamicResponse(response);

        print('✓ AIChatController: Respuesta del backend procesada');
      } catch (e) {
        print('⚠ AIChatController: Error al conectar con backend, usando respuesta simulada');
        // Fallback: generar respuesta simulada
        await Future.delayed(const Duration(seconds: 1));

        final aiResponse = _generateAIResponse(message);
        final aiMessage = ChatMessage(
          content: aiResponse,
          isUser: false,
          timestamp: DateTime.now(),
        );

        _messages.add(aiMessage);
      }
    } catch (e) {
      print('✗ AIChatController: Error al enviar mensaje: $e');
      final errorMessage = ChatMessage(
        content: 'Lo siento, hubo un error procesando tu solicitud. Por favor intenta nuevamente.',
        isUser: false,
        timestamp: DateTime.now(),
      );
      _messages.add(errorMessage);
      _setError('Error al enviar mensaje: ${e.toString()}');
    } finally {
      _isLoading = false;
      _isTyping = false;
      notifyListeners();
    }
  }

  /// Procesa la respuesta dinámica según su tipo
  Future<void> _processDynamicResponse(BaseDynamicResponseDTO response) async {
    String messageContent;
    String? metadata;

    switch (response.type) {
      case ResponseType.SIMPLE_TEXT:
        final textResponse = response as SimpleTextResponseDTO;
        messageContent = textResponse.text;

        // Agregar summary y analysis si existen
        if (textResponse.summary != null) {
          messageContent = '**Resumen:** ${textResponse.summary}\n\n$messageContent';
        }
        if (textResponse.analysis != null) {
          messageContent = '$messageContent\n\n**Análisis:** ${textResponse.analysis}';
        }
        break;

      case ResponseType.REPORT_DOWNLOAD:
        final reportResponse = response as ReportDownloadResponseDTO;

        // Construir mensaje con información del reporte
        messageContent = '📊 **Reporte Generado**\n\n';
        if (reportResponse.summary != null) {
          messageContent += '**Resumen:** ${reportResponse.summary}\n\n';
        }
        if (reportResponse.analysis != null) {
          messageContent += '**Análisis:** ${reportResponse.analysis}\n\n';
        }
        messageContent += '**ID del Reporte:** ${reportResponse.reportId}\n';
        messageContent += '**URL de Descarga:** ${reportResponse.downloadUrl}\n\n';
        messageContent += '📥 *Puedes descargar el reporte usando el enlace proporcionado.*';

        metadata = reportResponse.reportId;
        break;
    }

    // Agregar mensaje con la respuesta procesada
    final aiMessage = ChatMessage(
      content: messageContent,
      isUser: false,
      timestamp: DateTime.now(),
      metadata: metadata,
      responseType: response.type,
    );

    _messages.add(aiMessage);
  }

  String _generateAIResponse(String userMessage) {
    final lowercaseMessage = userMessage.toLowerCase();
    
    if (lowercaseMessage.contains('tendencia') || lowercaseMessage.contains('analizar tendencias')) {
      return '''📊 **Análisis de Tendencias**

He identificado las siguientes tendencias en tus datos:

🔹 **Tendencia principal**: Crecimiento sostenido del 8% mensual
🔹 **Estacionalidad**: Picos en diciembre y julio
🔹 **Correlaciones**: Alta correlación entre marketing digital y conversiones

**Recomendaciones:**
• Incrementar inversión en Q4
• Optimizar campañas para dispositivos móviles
• Implementar estrategias de retención

¿Te gustaría que profundice en alguna de estas áreas?''';
    }
    
    if (lowercaseMessage.contains('reporte') || lowercaseMessage.contains('generar reportes')) {
      return '''📈 **Generación de Reportes**

He generado un reporte completo con los siguientes elementos:

**Métricas Clave:**
• Revenue: \$1.2M (+15%)
• Conversiones: 8,450 (+12%)
• CAC: \$45 (-8%)
• LTV: \$320 (+18%)

**Gráficos incluidos:**
• Evolución temporal de ventas
• Distribución por canal
• Embudo de conversión
• Análisis de cohortes

**Insights principales:**
• El canal orgánico genera el 40% del tráfico
• Los usuarios móviles tienen 23% más conversión
• El abandono de carrito bajó al 15%

¿Quieres que exporte este reporte o necesitas análisis adicionales?''';
    }
    
    if (lowercaseMessage.contains('patrón') || lowercaseMessage.contains('detectar patrones')) {
      return '''🔍 **Detección de Patrones**

He analizado tus datos y encontré varios patrones interesantes:

**Patrones Temporales:**
• Actividad máxima: Martes y miércoles 2-4 PM
• Conversiones altas: Fines de semana por la mañana
• Abandono frecuente: Lunes en la tarde

**Patrones de Usuario:**
• Usuarios recurrentes: 65% más valor
• Sesiones largas (>5 min): 3x más conversión
• Usuarios móviles: Prefieren checkout rápido

**Anomalías detectadas:**
• Pico inusual el 15 de este mes (+340%)
• Caída atípica en conversiones mobile
• Incremento en devoluciones (+25%)

¿Quieres que investigue alguna de estas anomalías más a fondo?''';
    }
    
    if (lowercaseMessage.contains('predicción') || lowercaseMessage.contains('predicciones')) {
      return '''🔮 **Predicciones con IA**

Basándome en el análisis de datos históricos, aquí están mis predicciones:

**Próximos 30 días:**
• Revenue estimado: \$420K (±15%)
• Nuevos usuarios: 1,850
• Tasa de conversión: 3.2%

**Factores de influencia:**
• Estacionalidad: +12% por temporada alta
• Tendencia actual: +8% crecimiento mensual
• Campañas activas: +5% conversión adicional

**Escenarios:**
🟢 **Optimista**: \$485K (+15%)
🟡 **Realista**: \$420K (base)
🔴 **Conservador**: \$357K (-15%)

**Recomendaciones para maximizar resultados:**
• Aumentar presupuesto de ads en 20%
• Lanzar campaña de retención
• Optimizar landing pages móviles

¿Te interesa que detalle algún escenario específico?''';
    }
    
    // Respuesta genérica
    return '''¡Hola! Soy tu asistente de análisis de datos con IA. 

Puedo ayudarte con:
• 📊 Análisis de tendencias y patrones
• 📈 Generación de reportes automáticos  
• 🔍 Detección de anomalías
• 🔮 Predicciones basadas en datos
• 📱 Insights de comportamiento de usuarios
• 💰 Análisis de métricas de negocio

¿En qué análisis te gustaría que trabaje? Solo describe lo que necesitas y comenzaré a procesar tus datos.''';
  }

  /// Cancela la respuesta actual
  void cancelCurrentResponse() {
    print('⏹️ AIChatController: Cancelando respuesta');
    _isLoading = false;
    _isTyping = false;
    _typingTimer?.cancel();
    notifyListeners();
  }

  /// Limpia todos los mensajes de la conversación actual
  void clearMessages() {
    print('🧹 AIChatController: Limpiando mensajes');
    _messages.clear();
    notifyListeners();
  }

  /// Limpia el mensaje de error
  void clearError() {
    _clearError();
  }

  /// Descarga un reporte usando el ChatService
  Future<Uint8List?> downloadReport(String reportId) async {
    try {
      print('📥 AIChatController: Descargando reporte $reportId');
      _setLoading(true);
      _clearError();

      final reportData = await _chatService.downloadReport(reportId);

      print('✓ AIChatController: Reporte descargado exitosamente');
      _setLoading(false);
      return reportData;
    } catch (e) {
      print('✗ AIChatController: Error al descargar reporte: $e');
      _setError('Error al descargar el reporte: ${e.toString()}');
      _setLoading(false);
      return null;
    }
  }

  /// Obtiene todos los mensajes que son reportes
  List<ChatMessage> getReportMessages() {
    return _messages.where((msg) => msg.isReportMessage).toList();
  }

  /// Obtiene el último mensaje de tipo reporte
  ChatMessage? getLastReportMessage() {
    final reportMessages = getReportMessages();
    return reportMessages.isNotEmpty ? reportMessages.last : null;
  }

  // Métodos privados auxiliares
  void _setLoading(bool loading) {
    _isLoading = loading;
    notifyListeners();
  }

  void _setError(String error) {
    _errorMessage = error;
    notifyListeners();
  }

  void _clearError() {
    _errorMessage = null;
  }

  @override
  void dispose() {
    print('🔚 AIChatController: Disposing');
    _typingTimer?.cancel();
    _chatService.dispose();
    super.dispose();
  }
}

class ChatMessage {
  final String content;
  final bool isUser;
  final DateTime timestamp;
  final bool isNew;
  final String? metadata;
  final ResponseType? responseType;

  ChatMessage({
    required this.content,
    required this.isUser,
    required this.timestamp,
    this.isNew = true,
    this.metadata,
    this.responseType,
  });

  /// Verifica si el mensaje es de tipo reporte
  bool get isReportMessage => responseType == ResponseType.REPORT_DOWNLOAD;

  /// Verifica si el mensaje tiene metadata
  bool get hasMetadata => metadata != null && metadata!.isNotEmpty;

  /// Crea una copia del mensaje
  ChatMessage copyWith({
    String? content,
    bool? isUser,
    DateTime? timestamp,
    bool? isNew,
    String? metadata,
    ResponseType? responseType,
  }) {
    return ChatMessage(
      content: content ?? this.content,
      isUser: isUser ?? this.isUser,
      timestamp: timestamp ?? this.timestamp,
      isNew: isNew ?? this.isNew,
      metadata: metadata ?? this.metadata,
      responseType: responseType ?? this.responseType,
    );
  }
}