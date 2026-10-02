import 'package:flutter/material.dart';
import '../api.dart';
import '../util.dart';

class PedidosScreen extends StatefulWidget {
  const PedidosScreen({super.key});
  @override
  State<PedidosScreen> createState() => _PedidosScreenState();
}

class _PedidosScreenState extends State<PedidosScreen> {
  late Future<List> futuro;

  @override
  void initState() { super.initState(); _cargar(); }
  void _cargar() => futuro = Api.get('/pedidos').then((v) => v as List);

  Future<void> _calificar(int idPedido, int idAliado, String vendedor) async {
    int nota = 5;
    final com = TextEditingController();
    final ok = await showDialog<bool>(context: context, builder: (_) => StatefulBuilder(builder: (ctx, set) => AlertDialog(
      title: Text('Calificar a $vendedor'),
      content: Column(mainAxisSize: MainAxisSize.min, children: [
        Row(mainAxisAlignment: MainAxisAlignment.center, children: [
          for (var i = 1; i <= 5; i++) IconButton(onPressed: () => set(() => nota = i), icon: Icon(i <= nota ? Icons.star : Icons.star_border, color: const Color(0xFFF5A623), size: 32)),
        ]),
        TextField(controller: com, maxLines: 3, decoration: campo('Comentario (opcional)')),
      ]),
      actions: [TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancelar')), FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Enviar'))],
    )));
    if (ok != true) return;
    try {
      await Api.post('/calificaciones', {'idPedido': idPedido, 'idAliado': idAliado, 'calificacion': nota, 'comentario': com.text.trim()});
      if (mounted) { aviso(context, '¡Gracias por calificar!'); setState(_cargar); }
    } catch (e) { if (mounted) aviso(context, '$e', error: true); }
  }

  Future<void> _facturar(Map pedido) async {
    final rfc = TextEditingController(), razon = TextEditingController(), regimen = TextEditingController(text: '601'),
        cp = TextEditingController(), uso = TextEditingController(text: 'G03');
    final ok = await showDialog<bool>(context: context, builder: (ctx) => AlertDialog(
      title: Text('Facturar pedido #${pedido['id']}'),
      content: SingleChildScrollView(child: Column(mainAxisSize: MainAxisSize.min, children: [
        TextField(controller: rfc, textCapitalization: TextCapitalization.characters, decoration: campo('RFC')),
        const SizedBox(height: 8),
        TextField(controller: razon, decoration: campo('Razón social')),
        const SizedBox(height: 8),
        TextField(controller: regimen, decoration: campo('Régimen fiscal (ej. 601, 612, 626)')),
        const SizedBox(height: 8),
        TextField(controller: cp, keyboardType: TextInputType.number, maxLength: 5, decoration: campo('C.P. fiscal')),
        TextField(controller: uso, decoration: campo('Uso de CFDI')),
      ])),
      actions: [TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancelar')), FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Solicitar'))],
    ));
    if (ok != true) return;
    try {
      final f = await Api.post('/facturas', {
        'idPedido': pedido['id'], 'rfcReceptor': rfc.text.trim(), 'razonSocial': razon.text.trim(),
        'regimenFiscal': regimen.text.trim(), 'cpFiscal': cp.text.trim(), 'usoCfdi': uso.text.trim(),
      });
      if (mounted) {
        aviso(context, 'Factura solicitada. Folio ${f['folio']}');
        setState(_cargar);
      }
    } catch (e) { if (mounted) aviso(context, '$e', error: true); }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Mis pedidos')),
      body: !Api.logueado
          ? const Center(child: Text('Inicia sesión para ver tus pedidos.'))
          : RefreshIndicator(
              onRefresh: () async { setState(_cargar); await futuro; },
              child: FutureBuilder<List>(
                future: futuro,
                builder: (_, s) {
                  if (s.connectionState != ConnectionState.done) return const Center(child: CircularProgressIndicator());
                  if (s.hasError) return Center(child: Text('${s.error}', style: const TextStyle(color: rojo)));
                  final l = s.data!;
                  if (l.isEmpty) return const Center(child: Text('Aún no tienes pedidos.'));
                  return ListView(padding: const EdgeInsets.all(12), children: [for (final p in l) _pedido(Map<String, dynamic>.from(p)), const SizedBox(height: 90)]);
                },
              ),
            ),
    );
  }

  Widget _pedido(Map<String, dynamic> p) {
    final detalles = p['detalles'] as List;
    final pagado = ['PAGADO', 'ENVIADO', 'ENTREGADO'].contains(p['estatus']);
    final vistos = <int>{};
    final porCalificar = [for (final d in detalles) if (vistos.add(d['idAliado'] as int) && d['calificado'] != true) d];
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
            Text('Pedido #${p['id']}', style: const TextStyle(fontWeight: FontWeight.bold)),
            Chip(label: Text(p['estatus'], style: const TextStyle(fontSize: 11)), visualDensity: VisualDensity.compact),
          ]),
          Text(fecha(p['fecha']), style: const TextStyle(color: Colors.black54, fontSize: 12)),
          const Divider(),
          for (final d in detalles)
            Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                fotoRed(d['imagen'], h: 56, w: 56),
                const SizedBox(width: 10),
                Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text('${d['nombre']} × ${d['cantidad']}', style: const TextStyle(fontWeight: FontWeight.w600)),
                  Text('${d['modalidad'] == 'PROMOCION' ? 'Promoción' : 'Costo normal'} · ${money(d['importe'])} · ${d['vendedor']}', style: const TextStyle(fontSize: 12, color: Colors.black54)),
                  if (d['fundacionBeneficiaria'] != null) Text('💚 ${d['fundacionBeneficiaria']}', style: const TextStyle(fontSize: 11, color: verde)),
                  if (d['envio'] != null) ...[
                    Text('🚚 ${d['envio']['paqueteria']} · guía ${d['envio']['numeroGuia']}', style: const TextStyle(fontSize: 12)),
                    Text('${d['envio']['estatus']} · llega aprox. ${fecha(d['envio']['fechaEstimada'])}', style: const TextStyle(fontSize: 12, color: naranja, fontWeight: FontWeight.w600)),
                  ],
                ])),
              ]),
            ),
          const Divider(),
          Text('Subtotal ${money(p['subtotal'])} · Envío ${money(p['costoEnvio'])}', style: const TextStyle(fontSize: 12)),
          Text('Total ${money(p['total'])}', style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
          if (pagado) Wrap(spacing: 8, children: [
            if (p['facturado'] == true) const Chip(label: Text('🧾 Factura solicitada'), visualDensity: VisualDensity.compact)
            else OutlinedButton.icon(onPressed: () => _facturar(p), icon: const Icon(Icons.receipt_long), label: const Text('Facturar')),
            for (final d in porCalificar)
              OutlinedButton.icon(onPressed: () => _calificar(p['id'], d['idAliado'], d['vendedor']), icon: const Icon(Icons.star_outline), label: Text('Calificar a ${d['vendedor']}')),
          ]),
        ]),
      ),
    );
  }
}
