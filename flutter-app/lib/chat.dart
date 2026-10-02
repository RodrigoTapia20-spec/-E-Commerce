import 'dart:async';
import 'package:flutter/material.dart';
import 'api.dart';
import 'util.dart';

/// Asistente virtual SIEMPRE presente: botón flotante en todas las pantallas, con mensajes
/// proactivos según la pantalla y respuestas rápidas distintas para Cliente y Vendedor.
class Chat {
  static final ValueNotifier<String> pagina = ValueNotifier<String>('inicio');
  static final GlobalKey<NavigatorState> nav = GlobalKey<NavigatorState>();
  static int? idConv;
  static final List<Map<String, String>> mensajes = [];

  static const tips = {
    'inicio': '👋 ¿Buscas algo? Te ayudo a encontrar productos con causa y promociones.',
    'producto': '💡 Elige costo normal o promoción, y tu paquetería: verás costo y días de entrega.',
    'carrito': '🧾 Revisa tu envío y método de pago. La factura la pides después en "Mis pedidos".',
    'pedidos': '📦 Aquí ves tus guías y tiempos de entrega; puedes facturar y calificar a tu vendedor.',
    'vendedor': '📸 Te acompaño a publicar: cada producto lleva de 5 a 7 archivos (videos de máx. 15 s).',
    'distribuidor': '🚚 Pasa cada envío de PENDIENTE a EN_TRANSITO y luego a ENTREGADO.',
    'admin': '👑 Aprueba vendedores y revisa ventas y donaciones.',
    'login': '🔐 ¿Olvidaste tu contraseña? Te enviamos un enlace por correo.',
  };
  static const sugCliente = ['¿Cómo funcionan las promociones?', '¿Cómo se calcula el envío?', '¿Cómo facturo?', '¿Cómo califico a un vendedor?'];
  static const sugVendedor = ['¿Cómo publico un producto?', '¿Cómo lanzo una campaña?', '¿Cómo subo mi nivel de ranking?', '¿Qué paqueterías puedo usar?'];

  static Future<String> enviar(String texto) async {
    final r = await Api.post('/chatbot/mensaje', {
      'idConversacion': idConv, 'mensaje': texto, 'canal': 'APP',
      'contexto': Api.contexto, 'pagina': pagina.value,
    });
    idConv = (r['id'] as num).toInt();
    final l = r['mensajes'] as List;
    return l.last['mensaje'] as String;
  }
}

/// Se coloca una sola vez encima de todo el Navigator (ver main.dart).
class ChatFab extends StatefulWidget {
  const ChatFab({super.key});
  @override
  State<ChatFab> createState() => _ChatFabState();
}

class _ChatFabState extends State<ChatFab> {
  String? tip;
  final vistos = <String>{};
  Timer? _t;

  @override
  void initState() {
    super.initState();
    Chat.pagina.addListener(_alCambiar);
    WidgetsBinding.instance.addPostFrameCallback((_) => _alCambiar());
  }

  void _alCambiar() {
    final p = Chat.pagina.value;
    final t = Chat.tips[p];
    if (t == null || vistos.contains(p)) return;
    vistos.add(p);
    _t?.cancel();
    _t = Timer(const Duration(milliseconds: 1800), () {
      if (!mounted) return;
      setState(() => tip = t);
      _t = Timer(const Duration(seconds: 7), () { if (mounted) setState(() => tip = null); });
    });
  }

  @override
  void dispose() {
    Chat.pagina.removeListener(_alCambiar);
    _t?.cancel();
    super.dispose();
  }

  void _abrir() {
    setState(() => tip = null);
    final ctx = Chat.nav.currentContext;
    if (ctx == null) return;
    showModalBottomSheet(context: ctx, isScrollControlled: true, useSafeArea: true, builder: (_) => const _ChatSheet());
  }

  @override
  Widget build(BuildContext context) {
    return Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.end, children: [
      if (tip != null)
        GestureDetector(
          onTap: _abrir,
          child: Material(
            elevation: 6, borderRadius: BorderRadius.circular(12), color: Colors.white,
            child: Container(
              constraints: const BoxConstraints(maxWidth: 250), padding: const EdgeInsets.all(10),
              child: Text(tip!, style: const TextStyle(fontSize: 13, color: Colors.black87)),
            ),
          ),
        ),
      const SizedBox(height: 8),
      FloatingActionButton(heroTag: 'chat', backgroundColor: naranja, foregroundColor: azul, onPressed: _abrir, child: const Icon(Icons.chat_bubble)),
    ]);
  }
}

class _ChatSheet extends StatefulWidget {
  const _ChatSheet();
  @override
  State<_ChatSheet> createState() => _ChatSheetState();
}

class _ChatSheetState extends State<_ChatSheet> {
  final c = TextEditingController();
  final scroll = ScrollController();
  bool cargando = false;

  @override
  void initState() {
    super.initState();
    if (Chat.mensajes.isEmpty) {
      Chat.mensajes.add({'e': 'BOT', 't': Api.contexto == 'VENDEDOR'
          ? '¡Hola! Soy tu asistente 🤝. Te acompaño a publicar productos, lanzar campañas y mejorar tu ranking. ¿Con qué empezamos?'
          : '¡Hola! Soy tu asistente 🤝. Te acompaño en tu compra: promociones, envíos, pagos, factura y seguimiento. ¿En qué te ayudo?'});
    }
  }

  Future<void> _enviar(String texto) async {
    if (texto.trim().isEmpty || cargando) return;
    c.clear();
    setState(() { Chat.mensajes.add({'e': 'USUARIO', 't': texto}); cargando = true; });
    _bajar();
    try {
      final r = await Chat.enviar(texto);
      Chat.mensajes.add({'e': 'BOT', 't': r});
    } catch (e) {
      Chat.mensajes.add({'e': 'BOT', 't': 'No pude responder ahora: $e'});
    }
    if (mounted) setState(() => cargando = false);
    _bajar();
  }

  void _bajar() => WidgetsBinding.instance.addPostFrameCallback((_) {
        if (scroll.hasClients) scroll.animateTo(scroll.position.maxScrollExtent + 80, duration: const Duration(milliseconds: 200), curve: Curves.easeOut);
      });

  @override
  Widget build(BuildContext context) {
    final sugs = Api.contexto == 'VENDEDOR' ? Chat.sugVendedor : Chat.sugCliente;
    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
      child: SizedBox(
        height: MediaQuery.of(context).size.height * 0.75,
        child: Column(children: [
          Container(
            color: azul, padding: const EdgeInsets.all(14),
            child: Row(children: [
              const Icon(Icons.smart_toy, color: naranja), const SizedBox(width: 8),
              const Expanded(child: Text('Asistente virtual JJM', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold))),
              IconButton(icon: const Icon(Icons.close, color: Colors.white), onPressed: () => Navigator.pop(context)),
            ]),
          ),
          Expanded(
            child: ListView(controller: scroll, padding: const EdgeInsets.all(12), children: [
              for (final m in Chat.mensajes)
                Align(
                  alignment: m['e'] == 'BOT' ? Alignment.centerLeft : Alignment.centerRight,
                  child: Container(
                    margin: const EdgeInsets.symmetric(vertical: 4), padding: const EdgeInsets.all(10),
                    constraints: BoxConstraints(maxWidth: MediaQuery.of(context).size.width * 0.78),
                    decoration: BoxDecoration(color: m['e'] == 'BOT' ? const Color(0xFFF0F2F4) : naranja, borderRadius: BorderRadius.circular(12)),
                    child: Text(m['t']!),
                  ),
                ),
              if (cargando) const Padding(padding: EdgeInsets.all(8), child: Text('Escribiendo…', style: TextStyle(color: Colors.black45))),
            ]),
          ),
          SizedBox(
            height: 40,
            child: ListView(scrollDirection: Axis.horizontal, padding: const EdgeInsets.symmetric(horizontal: 8), children: [
              for (final s in sugs)
                Padding(padding: const EdgeInsets.only(right: 6), child: ActionChip(label: Text(s, style: const TextStyle(fontSize: 12)), onPressed: () => _enviar(s))),
            ]),
          ),
          Padding(
            padding: const EdgeInsets.all(8),
            child: Row(children: [
              Expanded(child: TextField(controller: c, onSubmitted: _enviar, decoration: campo('Escribe tu pregunta...'))),
              const SizedBox(width: 6),
              IconButton.filled(style: IconButton.styleFrom(backgroundColor: naranja, foregroundColor: azul), onPressed: () => _enviar(c.text), icon: const Icon(Icons.send)),
            ]),
          ),
        ]),
      ),
    );
  }
}
