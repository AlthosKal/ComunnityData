class ApiErrorParser {
  static String extractMessage(dynamic data) {
    if (data is Map<String, dynamic> && data['message'] != null) {
      return data['message'].toString();
    }
    return 'Error desconocido. Intenta nuevamente.';
  }
}
