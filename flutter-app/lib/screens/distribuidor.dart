import 'package:flutter/material.dart';
import '../api.dart';
import '../util.dart';

/// Panel del Distribuidor: actualiza el estatus de cada guía (PENDIENTE → EN_TRANSITO → ENTREGADO).
class DistribuidorScreen extends StatefulWidget {
  const DistribuidorScreen({super.key});
  @override
  State<DistribuidorScreen> createState() => _DistribuidorScreenState();
}

class _DistribuidorScreenState extends State<DistribuidorScreen> {
  String filtro = '';
  late Future<List> futuro;
  @override
  void initState() { super.initState(); _c(); }
  void _c() => futuro = Api.get('/distribuidor/envios${filtro.isEmpty ? '' : '?estatus=$filtro'}').then((v) => v as List);

  @override
  Widget build(BuildContext context) {
    if (Api.rol != 'DISTRIBUIDOR' && Api.rol != 'ADMIN') return Scaffold(appBar: AppBar(title: const Text('Distribuidor')), body: const Center(child: Text('Solo para distribuidores.')));
    return Scaffold(
      appBar: AppBar(title: const Text('Panel de distribuidor')),
      body: Column(children: [
        SizedBox(height: 50, child: ListView(scrollDirection: Axis.horizontal, padding: const EdgeInsets.all(8), children: [
          for (final f in ['', 'PENDIENTE', 'EN_TRANSITO', 'ENTREGADO'])
            Padding(padding: const EdgeInsets.only(right: 6), child: ChoiceChip(label: Text(f.isEmpty ? 'Todos' : f), selected: filtro == f, onSelected: (_) => setState(() { filtro = f; _c(); }))),
        ])),
        Expanded(child: RefreshIndicator(
          onRefresh: () async { setState(_c); await futuro; },
          child: FutureBuilder<List>(
            future: futuro,
            builder: (_, s) {
              if (s.connectionState != ConnectionState.done) return const Center(child: CircularProgressIndicator());
              if (s.hasError) return Center(child: Text('${s.error}', style: const TextStyle(color: rojo)));
              final l = s.data!;
              if (l.isEmpty) return ListView(children: const [Padding(padding: EdgeInsets.all(30), child: Center(child: Text('Sin envíos.')))]);
              return ListView(padding: const EdgeInsets.all(12), children: [
                for (final e in l)
                  Card(child: Padding(padding: const EdgeInsets.all(12), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Text('Guía ${e['numeroGuia']} · ${e['paqueteria']}', style: const TextStyle(fontWeight: FontWeight.bold)),
                    Text('Pedido #${e['idPedido']} · ${e['producto']} × ${e['cantidad']}'),
                    Text('Cliente: ${e['cliente']}\n${e['direccion']}', style: const TextStyle(fontSize: 12, color: Colors.black54)),
                    Text('Vendedor: ${e['vendedor']} · llega aprox. ${fecha(e['fechaEstimada'])}', style: const TextStyle(fontSize: 12)),
                    Row(children: [
                      Chip(label: Text(e['estatus'], style: const TextStyle(fontSize: 11)), visualDensity: VisualDensity.compact),
                      const Spacer(),
                      if (e['estatus'] != 'ENTREGADO')
                        FilledButton(
                          onPressed: () async {
                            final sig = e['estatus'] == 'PENDIENTE' ? 'EN_TRANSITO' : 'ENTREGADO';
                            try { await Api.patch('/distribuidor/envios/${e['id']}/estatus?estatus=$sig'); setState(_c); }
                            catch (x) { if (context.mounted) aviso(context, '$x', error: true); }
                          },
                          child: Text(e['estatus'] == 'PENDIENTE' ? 'Marcar en tránsito' : 'Marcar entregado'),
                        ),
                    ]),
                  ]))),
                const SizedBox(height: 90),
              ]);
            },
          ),
        )),
      ]),
    );
  }
}
