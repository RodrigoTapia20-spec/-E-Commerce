import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'chat.dart';

const azul = Color(0xFF0F1B2B);
const naranja = Color(0xFFE58325);
const verde = Color(0xFF0A7D3B);
const rojo = Color(0xFFB3261E);

final _fmt = NumberFormat.currency(locale: 'es_MX', symbol: '\$');
String money(num? n) => _fmt.format(n ?? 0);

String fecha(dynamic f) {
  if (f == null) return '';
  final d = DateTime.tryParse(f.toString());
  return d == null ? f.toString() : DateFormat('dd MMM yyyy', 'es').format(d);
}

void aviso(BuildContext c, String m, {bool error = false}) {
  ScaffoldMessenger.of(c).hideCurrentSnackBar();
  ScaffoldMessenger.of(c).showSnackBar(SnackBar(content: Text(m), backgroundColor: error ? rojo : azul));
}

/// Navega dando nombre a la pantalla: el chatbot lo usa para acompañar según el contexto.
Future<T?> ir<T>(BuildContext c, String nombre, Widget pantalla) =>
    Navigator.of(c).push<T>(MaterialPageRoute(settings: RouteSettings(name: nombre), builder: (_) => pantalla));

class PaginaObserver extends NavigatorObserver {
  @override
  void didPush(Route route, Route? previous) => Chat.pagina.value = route.settings.name ?? 'inicio';
  @override
  void didPop(Route route, Route? previous) => Chat.pagina.value = previous?.settings.name ?? 'inicio';
}

String temporada(String? t) => const {
      'NAVIDAD': '🎄 Navidad', 'DIA_DE_MUERTOS': '💀 Día de Muertos', 'FIESTAS_PATRIAS': '🇲🇽 Fiestas Patrias',
      'BUEN_FIN': '🛍️ Buen Fin', 'DIA_DE_LAS_MADRES': '💐 Día de las Madres', 'SAN_VALENTIN': '❤️ San Valentín',
    }[t] ?? '🎁 Promoción';

Widget estrellas(dynamic prom, dynamic total) {
  if (prom == null) return const Text('Sin calificaciones', style: TextStyle(fontSize: 11, color: Colors.black54));
  final n = (prom as num).round();
  return Text('${'★' * n}${'☆' * (5 - n)} ${prom.toStringAsFixed(1)} ($total)',
      style: const TextStyle(fontSize: 12, color: Color(0xFFF5A623)));
}

String? imagenPrincipal(Map p) {
  for (final m in (p['media'] as List? ?? [])) {
    if (m['tipo'] == 'IMAGEN') return m['url'] as String;
  }
  return null;
}

Widget fotoRed(String? url, {double? h, double? w}) => url == null
    ? Container(height: h, width: w, color: Colors.grey.shade300, child: const Icon(Icons.image_not_supported))
    : Image.network(url, height: h, width: w, fit: BoxFit.cover,
        errorBuilder: (_, __, ___) => Container(height: h, width: w, color: Colors.grey.shade300, child: const Icon(Icons.broken_image)));

/// Campo de contraseña con ojito (mostrar/ocultar).
class CampoPassword extends StatefulWidget {
  final TextEditingController c;
  final String etiqueta;
  const CampoPassword(this.c, {super.key, this.etiqueta = 'Contraseña'});
  @override
  State<CampoPassword> createState() => _CampoPasswordState();
}

class _CampoPasswordState extends State<CampoPassword> {
  bool oculto = true;
  @override
  Widget build(BuildContext context) => TextField(
        controller: widget.c,
        obscureText: oculto,
        decoration: InputDecoration(
          labelText: widget.etiqueta,
          border: const OutlineInputBorder(),
          suffixIcon: IconButton(icon: Icon(oculto ? Icons.visibility : Icons.visibility_off), onPressed: () => setState(() => oculto = !oculto)),
        ),
      );
}

InputDecoration campo(String etiqueta, {String? ayuda}) =>
    InputDecoration(labelText: etiqueta, helperText: ayuda, border: const OutlineInputBorder(), isDense: true);
