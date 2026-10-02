import 'dart:convert';
import 'dart:io' show Platform;
import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'package:http_parser/http_parser.dart';
import 'package:shared_preferences/shared_preferences.dart';

class ApiException implements Exception {
  final String message;
  ApiException(this.message);
  @override
  String toString() => message;
}

/// Cliente de la API REST de JJM (la misma que usa la web).
class Api {
  static String base = '';
  static String? token;
  static String? rol;
  static String? nombre;
  static final ValueNotifier<int> sesion = ValueNotifier<int>(0);

  /// Emulador Android → 10.0.2.2 es el "localhost" de tu computadora.
  /// Simulador iOS → localhost. Celular físico → cambia la IP desde el ícono ⚙ del login.
  /// También se puede fijar al compilar:  flutter run --dart-define=API_URL=http://192.168.1.50:8080/api
  static String get baseInicial {
    const desdeBuild = String.fromEnvironment('API_URL');
    if (desdeBuild.isNotEmpty) return desdeBuild;
    return (!kIsWeb && Platform.isAndroid) ? 'http://10.0.2.2:8080/api' : 'http://localhost:8080/api';
  }

  static Future<void> init() async {
    final p = await SharedPreferences.getInstance();
    base = p.getString('api_base') ?? baseInicial;
    token = p.getString('token');
    rol = p.getString('rol');
    nombre = p.getString('nombre');
  }

  static Future<void> setBase(String v) async {
    base = v.trim().replaceAll(RegExp(r'/+$'), '');
    final p = await SharedPreferences.getInstance();
    await p.setString('api_base', base);
  }

  static bool get logueado => token != null;
  static String get contexto =>
      rol == 'ALIADO' ? 'VENDEDOR' : (rol == 'DISTRIBUIDOR' || rol == 'ADMIN') ? rol! : 'CLIENTE';

  static Future<void> guardarSesion(Map d) async {
    token = d['token'] as String?;
    rol = d['rol'] as String?;
    nombre = d['nombre'] as String?;
    final p = await SharedPreferences.getInstance();
    await p.setString('token', token ?? '');
    await p.setString('rol', rol ?? '');
    await p.setString('nombre', nombre ?? '');
    sesion.value++;
  }

  static Future<void> cerrarSesion() async {
    token = null;
    rol = null;
    nombre = null;
    final p = await SharedPreferences.getInstance();
    await p.remove('token');
    await p.remove('rol');
    await p.remove('nombre');
    sesion.value++;
  }

  static Map<String, String> _headers() => {
        'Content-Type': 'application/json',
        if (token != null) 'Authorization': 'Bearer $token',
      };

  static dynamic _procesar(http.Response r) {
    dynamic data;
    try {
      data = jsonDecode(utf8.decode(r.bodyBytes));
    } catch (_) {}
    if (r.statusCode >= 200 && r.statusCode < 300) return data;
    if (r.statusCode == 401 && token != null) {
      cerrarSesion();
      throw ApiException('Tu sesión expiró. Inicia sesión de nuevo.');
    }
    final msg = data is Map ? (data['mensaje'] ?? data['error'] ?? 'Error ${r.statusCode}') : 'Error ${r.statusCode}';
    throw ApiException(msg.toString());
  }

  static Future<dynamic> _run(Future<http.Response> Function() f, {int segundos = 30}) async {
    try {
      return _procesar(await f().timeout(Duration(seconds: segundos)));
    } on ApiException {
      rethrow;
    } catch (_) {
      throw ApiException('No hay conexión con el servidor ($base). Revisa que el backend esté corriendo y la dirección sea correcta.');
    }
  }

  static Future<dynamic> get(String path) => _run(() => http.get(Uri.parse('$base$path'), headers: _headers()));
  static Future<dynamic> post(String path, [Object? body]) => _run(() =>
      http.post(Uri.parse('$base$path'), headers: _headers(), body: body == null ? null : jsonEncode(body)));
  static Future<dynamic> patch(String path) => _run(() => http.patch(Uri.parse('$base$path'), headers: _headers()));

  /// Sube una imagen o video a /aliados/media y devuelve {url, tipo}.
  static Future<Map<String, dynamic>> subir(String ruta) async {
    const tipos = {
      'jpg': 'image/jpeg', 'jpeg': 'image/jpeg', 'png': 'image/png', 'webp': 'image/webp',
      'mp4': 'video/mp4', 'mov': 'video/quicktime', 'webm': 'video/webm',
    };
    final ext = ruta.split('.').last.toLowerCase();
    final partes = (tipos[ext] ?? 'image/jpeg').split('/');
    final req = http.MultipartRequest('POST', Uri.parse('$base/aliados/media'));
    if (token != null) req.headers['Authorization'] = 'Bearer $token';
    req.files.add(await http.MultipartFile.fromPath('archivo', ruta, contentType: MediaType(partes[0], partes[1])));
    final res = await _run(() async => http.Response.fromStream(await req.send()), segundos: 180);
    return Map<String, dynamic>.from(res as Map);
  }
}
