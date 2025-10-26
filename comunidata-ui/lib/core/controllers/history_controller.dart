import 'package:flutter/material.dart';
import '../services/history_service.dart';
import '../../dto/chat/request/chat_history_dto.dart';

class HistoryController with ChangeNotifier {
  final HistoryService _historyService;

  // Estado
  List<ChatHistoryDTO> _allHistory = [];
  List<ChatHistoryDTO> _conversationHistory = [];
  List<ChatHistoryDTO> _searchResults = [];
  Map<String, dynamic>? _stats;

  bool _isLoading = false;
  bool _isSearching = false;
  String? _errorMessage;
  String? _currentConversationId;

  // Getters
  List<ChatHistoryDTO> get allHistory => List.unmodifiable(_allHistory);
  List<ChatHistoryDTO> get conversationHistory => List.unmodifiable(_conversationHistory);
  List<ChatHistoryDTO> get searchResults => List.unmodifiable(_searchResults);
  Map<String, dynamic>? get stats => _stats;

  bool get isLoading => _isLoading;
  bool get isSearching => _isSearching;
  String? get errorMessage => _errorMessage;
  String? get currentConversationId => _currentConversationId;

  bool get hasHistory => _allHistory.isNotEmpty;
  bool get hasConversationHistory => _conversationHistory.isNotEmpty;
  bool get hasSearchResults => _searchResults.isNotEmpty;
  bool get hasError => _errorMessage != null;

  HistoryController({HistoryService? historyService})
      : _historyService = historyService ?? HistoryService();

  /// Obtiene todo el historial de conversaciones
  Future<void> fetchAllHistory() async {
    try {
      print('=Ú HistoryController: Obteniendo todo el historial');
      _setLoading(true);
      _clearError();

      _allHistory = await _historyService.getAllHistory();

      print(' HistoryController: ${_allHistory.length} registros obtenidos');
      notifyListeners();
    } catch (e) {
      print(' HistoryController: Error al obtener historial: $e');
      _setError('Error al cargar el historial: ${e.toString()}');
    } finally {
      _setLoading(false);
    }
  }

  /// Obtiene el historial de una conversación específica
  Future<void> fetchConversationHistory(String conversationId) async {
    try {
      print('=Ö HistoryController: Obteniendo historial de conversación $conversationId');
      _setLoading(true);
      _clearError();

      _currentConversationId = conversationId;
      _conversationHistory = await _historyService.getHistoryByConversationId(conversationId);

      print(' HistoryController: ${_conversationHistory.length} mensajes obtenidos');
      notifyListeners();
    } catch (e) {
      print(' HistoryController: Error al obtener historial de conversación: $e');
      _setError('Error al cargar el historial de la conversación: ${e.toString()}');
    } finally {
      _setLoading(false);
    }
  }

  /// Elimina el historial de una conversación
  Future<bool> deleteConversationHistory(String conversationId) async {
    try {
      print('=Ñ HistoryController: Eliminando historial de conversación $conversationId');
      _setLoading(true);
      _clearError();

      await _historyService.deleteHistoryByConversationId(conversationId);

      // Remover del historial local
      _allHistory.removeWhere((item) => item.conversationId == conversationId);

      if (_currentConversationId == conversationId) {
        _conversationHistory.clear();
        _currentConversationId = null;
      }

      print(' HistoryController: Historial eliminado exitosamente');
      notifyListeners();
      return true;
    } catch (e) {
      print(' HistoryController: Error al eliminar historial: $e');
      _setError('Error al eliminar el historial: ${e.toString()}');
      return false;
    } finally {
      _setLoading(false);
    }
  }

  /// Busca en el historial por palabra clave
  Future<void> searchInHistory(String keyword) async {
    if (keyword.trim().isEmpty) {
      _searchResults.clear();
      notifyListeners();
      return;
    }

    try {
      print('= HistoryController: Buscando "$keyword"');
      _isSearching = true;
      _clearError();
      notifyListeners();

      _searchResults = await _historyService.searchHistory(keyword);

      print(' HistoryController: ${_searchResults.length} resultados encontrados');
      notifyListeners();
    } catch (e) {
      print(' HistoryController: Error en búsqueda: $e');
      _setError('Error al buscar en el historial: ${e.toString()}');
    } finally {
      _isSearching = false;
      notifyListeners();
    }
  }

  /// Limpia los resultados de búsqueda
  void clearSearch() {
    print('>ù HistoryController: Limpiando búsqueda');
    _searchResults.clear();
    notifyListeners();
  }

  /// Obtiene estadísticas del historial
  Future<void> fetchHistoryStats() async {
    try {
      print('=Ê HistoryController: Obteniendo estadísticas');
      _setLoading(true);
      _clearError();

      _stats = await _historyService.getHistoryStats();

      print(' HistoryController: Estadísticas obtenidas');
      notifyListeners();
    } catch (e) {
      print(' HistoryController: Error al obtener estadísticas: $e');
      _setError('Error al cargar estadísticas: ${e.toString()}');
    } finally {
      _setLoading(false);
    }
  }

  /// Exporta el historial de una conversación a JSON
  Future<String?> exportConversationHistory(String conversationId) async {
    try {
      print('=å HistoryController: Exportando historial de $conversationId');
      _setLoading(true);
      _clearError();

      final jsonString = await _historyService.exportHistoryAsJson(conversationId);

      print(' HistoryController: Historial exportado');
      _setLoading(false);
      return jsonString;
    } catch (e) {
      print(' HistoryController: Error al exportar: $e');
      _setError('Error al exportar el historial: ${e.toString()}');
      _setLoading(false);
      return null;
    }
  }

  /// Filtra el historial localmente por conversationId
  List<ChatHistoryDTO> filterByConversationId(String conversationId) {
    return _allHistory
        .where((item) => item.conversationId == conversationId)
        .toList();
  }

  /// Filtra el historial localmente por fecha
  List<ChatHistoryDTO> filterByDateRange(DateTime start, DateTime end) {
    return _allHistory
        .where((item) =>
            item.date.isAfter(start) && item.date.isBefore(end))
        .toList();
  }

  /// Obtiene el conteo de mensajes por conversación
  Map<String, int> getMessageCountByConversation() {
    final Map<String, int> counts = {};
    for (var item in _allHistory) {
      counts[item.conversationId] = (counts[item.conversationId] ?? 0) + 1;
    }
    return counts;
  }

  /// Obtiene las conversaciones únicas del historial
  List<String> getUniqueConversations() {
    return _allHistory
        .map((item) => item.conversationId)
        .toSet()
        .toList();
  }

  /// Obtiene el último mensaje de una conversación
  ChatHistoryDTO? getLastMessageFromConversation(String conversationId) {
    final messages = _allHistory
        .where((item) => item.conversationId == conversationId)
        .toList();

    if (messages.isEmpty) return null;

    messages.sort((a, b) => b.date.compareTo(a.date));
    return messages.first;
  }

  /// Recarga el historial actual
  Future<void> refresh() async {
    print('= HistoryController: Refrescando datos');
    if (_currentConversationId != null) {
      await fetchConversationHistory(_currentConversationId!);
    } else {
      await fetchAllHistory();
    }
  }

  /// Limpia todo el historial local (no elimina del servidor)
  void clearLocalHistory() {
    print('>ù HistoryController: Limpiando historial local');
    _allHistory.clear();
    _conversationHistory.clear();
    _searchResults.clear();
    _stats = null;
    _currentConversationId = null;
    notifyListeners();
  }

  /// Limpia el mensaje de error
  void clearError() {
    _clearError();
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
    if (!_isLoading) {
      notifyListeners();
    }
  }

  @override
  void dispose() {
    print('= HistoryController: Disposing');
    _historyService.dispose();
    super.dispose();
  }
}
