import 'package:flutter/material.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'api.dart';
import 'chat.dart';
import 'util.dart';
import 'screens/catalogo.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initializeDateFormatting('es');
  await Api.init();
  runApp(const JjmApp());
}

class JjmApp extends StatelessWidget {
  const JjmApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'JJM con Causa',
      debugShowCheckedModeBanner: false,
      navigatorKey: Chat.nav,
      navigatorObservers: [PaginaObserver()],
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(seedColor: naranja, primary: azul, secondary: naranja),
        scaffoldBackgroundColor: const Color(0xFFEAEDED),
        appBarTheme: const AppBarTheme(backgroundColor: azul, foregroundColor: Colors.white),
        filledButtonTheme: FilledButtonThemeData(style: FilledButton.styleFrom(backgroundColor: naranja, foregroundColor: azul)),
      ),
      // El asistente vive encima de TODAS las pantallas (cliente y vendedor).
      builder: (context, child) => Stack(textDirection: TextDirection.ltr, children: [
        child!,
        const Positioned(right: 14, bottom: 14, child: SafeArea(child: ChatFab())),
      ]),
      onGenerateInitialRoutes: (_) => [
        MaterialPageRoute(settings: const RouteSettings(name: 'inicio'), builder: (_) => const CatalogoScreen()),
      ],
    );
  }
}
