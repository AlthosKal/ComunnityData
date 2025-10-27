class ApiResponse<T> {
  final bool success;
  final String message;
  final T data;
  final String path;
  final DateTime timestamp;

  ApiResponse({
    required this.success,
    required this.message,
    required this.data,
    required this.path,
    required this.timestamp,
  });

  factory ApiResponse.fromJson(
    Map<String, dynamic> json,
    T Function(dynamic) fromDataJson,
  ) {
    return ApiResponse<T>(
      success: json['success'],
      message: json['message'],
      data: fromDataJson(json['data']),
      path: json['path'],
      timestamp: DateTime.parse(json['timestamp']),
    );
  }
}
