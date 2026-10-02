import 'package:flutter/material.dart';
import '../api.dart';
import '../util.dart';

class QuejasScreen extends StatefulWidget {
  const QuejasScreen({super.key});
  @override
  State<QuejasScreen> createState() => _QuejasScreenState();
}

class _QuejasScreenState extends State<QuejasScreen> {
  final asunto = TextEditingController(), desc = TextEditingController();
  String tipo = 'QUEJA';
  List lista = [];
  bool enviando = false;

  @override
  void initState() { super.initState(); _cargar(); }
  Future<void> _cargar() async {
    if (!Api.logueado) return;
    try { final r = await Api.get('/quejas/mias'); if (mounted) setState(() => lista = r as List); } catch (_) {}
  }

  Future<void> _enviar() async {
    if (!Api.logueado) { aviso(context, 'Inicia sesión para enviar tu mensaje.', error: true); return; }
    setState(() => enviando = true);
    try {
      await Api.post('/quejas', {'tipo': tipo, 'asunto': asunto.text.trim(), 'descripcion': desc.text.trim()});
      asunto.clear(); desc.clear();
      if (mounted) aviso(context, 'Recibimos tu mensaje. ¡Gracias!');
      await _cargar();
    } catch (e) { if (mounted) aviso(context, '$e', error: true); }
    if (mounted) setState(() => enviando = false);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Quejas y sugerencias')),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        DropdownButtonFormField<String>(value: tipo, decoration: campo('Tipo'),
            items: const [DropdownMenuItem(value: 'QUEJA', child: Text('Queja')), DropdownMenuItem(value: 'SUGERENCIA', child: Text('Sugerencia'))],
            onChanged: (v) => setState(() => tipo = v!)),
        const SizedBox(height: 10),
        TextField(controller: asunto, decoration: campo('Asunto')),
        const SizedBox(height: 10),
        TextField(controller: desc, maxLines: 4, decoration: campo('Descripción')),
        const SizedBox(height: 10),
        FilledButton(onPressed: enviando ? null : _enviar, child: const Text('Enviar')),
        const Divider(height: 30),
        const Text('Mis mensajes', style: TextStyle(fontWeight: FontWeight.bold)),
        for (final q in lista)
          Card(child: ListTile(title: Text('${q['tipo']}: ${q['asunto']}'), subtitle: Text('${q['descripcion']}${q['respuesta'] != null ? '\n\nRespuesta: ${q['respuesta']}' : ''}'), trailing: Text(q['estatus'], style: const TextStyle(fontSize: 11)))),
        const SizedBox(height: 90),
      ]),
    );
  }
}
