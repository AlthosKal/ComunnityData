import 'package:dio/dio.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';

class ApiClient {
  // Singleton
  static final ApiClient _instance = ApiClient._internal();

  factory ApiClient() {
    return _instance;
  }

  late final Dio _dioApp;

  late final String baseUrlApp;
  late final String baseUrlChat;

  bool _isInitialized = false;

  ApiClient._internal() {
    baseUrlApp = dotenv.get('API_BASE_URL');
    _dioApp = _createDio(baseUrlApp);
    _isInitialized = true;
  }

  Dio _createDio(String baseUrl) {
    final dio = Dio(
      BaseOptions(
        baseUrl: baseUrl,
        connectTimeout: const Duration(seconds: 10),
        receiveTimeout: const Duration(seconds: 10),
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        },
      ),
    );

    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          print('🌐 [${options.method}] ${options.path}');
          print('📤 Request Data: ${options.data}');
          return handler.next(options);
        },
        onResponse: (response, handler){
          print('✅ Response [${response.statusCode}]: ${response.data}');
          return handler.next(response);
        },
        onError: (DioException error, handler) {
          print('❌ Error [${error.response?.statusCode}]: ${error.message}');
          return handler.next(error);
        },
      ),
    );

    return dio;
  }

  //Verificar si está inicializado
  bool get isInitialized => _isInitialized;

  // Métodos públicos para usar las APIs
  Future<Response> getApp(
    String path, {
    Map<String, dynamic>? queryParameters,
        Options? options,
  }) => _dioApp.get(path, queryParameters: queryParameters, options: options);

  Future<Response> postApp(String path, dynamic data) =>
      _dioApp.post(path, data: data);
  Future<Response> postAppWithOptions(String path, dynamic data, Options? options) =>
      _dioApp.post(path, data: data, options: options);

  Future<Response> putApp(String path, dynamic data) =>
      _dioApp.put(path, data: data);

  Future<Response> patchApp(String path, dynamic data) =>
      _dioApp.patch(path, data: data);

  Future<Response> deleteApp(String path) => _dioApp.delete(path);

  Future<Response> downloadFile(String path, String savePath) {
    return _dioApp.download(
      path,
      savePath,
      options: Options(responseType: ResponseType.bytes),
    );
  }
}
