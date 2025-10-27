import 'package:flutter/material.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:provider/provider.dart';

import 'core/controllers/ai_chat_controller.dart';
import 'core/controllers/csv_controller.dart';
import 'screens/ai_analysis_view.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Cargar variables de entorno
  await dotenv.load(fileName: '.env');

  runApp(const ComuniDataApp());
}

class ComuniDataApp extends StatelessWidget {
  const ComuniDataApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => AIChatController()),
        ChangeNotifierProvider(create: (_) => CsvController()),
      ],
      child: MaterialApp(
        title: 'ComuniData - Análisis con IA',
        debugShowCheckedModeBanner: false,
        theme: ThemeData(
          colorScheme: ColorScheme.fromSeed(
            seedColor: Colors.blue,
            brightness: Brightness.light,
          ),
          useMaterial3: true,
          fontFamily: 'System',
        ),
        home: const AIAnalysisView(),
      ),
    );
  }
}

