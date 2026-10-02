import 'package:flutter/material.dart';
import '../api.dart';
import '../util.dart';

/// Administrador (dueño): resumen de ventas, aprobación de vendedores y alta de distribuidores.
class AdminScreen extends StatefulWidget {
  const AdminScreen({super.key});
  @override
  State<AdminScreen> createState() => _AdminScreenState();
}

class _AdminScreenState extends State<AdminScreen> {
  Map? kpis;
  List pendientes = [];
  String? error;

  @override
  void initState() { super.initState(); _c(); }
  Future<void> _c() async {
    try {
      final r = await Future.wait([Api.get('/reportes/ventas-general'), Api.get('/admin/vendedores/pendientes')]);
      if (mounted) setState(() { kpis = r[0] as Map; pendientes = r[1] as List; });
    } catch (e) { if (mounted) setState(() => error = '$e'); }
  }

  Future<void> _distribuidor() async {
    final n = TextEditingController(), a = TextEditingController(), c = TextEditingController(), p = TextEditingController();
    final ok = await showDialog<bool>(context: context, builder: (ctx) => AlertDialog(
      title: const Text('Nuevo distribuidor'),
      content: SingleChildScrollView(child: Column(mainAxisSize: MainAxisSize.min, children: [
        TextField(controller: n, decoration: campo('Nombre')), const SizedBox(height: 8),
        TextField(controller: a, decoration: campo('Apellidos')), const SizedBox(height: 8),
        TextField(controller: c, keyboardType: TextInputType.emailAddress, decoration: campo('Correo')), const SizedBox(height: 8),
        CampoPassword(p, etiqueta: 'Contraseña (mín. 8)'),
      ])),
      actions: [TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancelar')), FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Crear'))],
    ));
    if (ok != true) return;
    try {
      await Api.post('/admin/usuarios', {'nombre': n.text.trim(), 'apellidos': a.text.trim(), 'correo': c.text.trim(), 'password': p.text, 'rol': 'DISTRIBUIDOR'});
      if (mounted) aviso(context, 'Distribuidor creado');
    } catch (e) { if (mounted) aviso(context, '$e', error: true); }
  }

  @override
  Widget build(BuildContext context) {
    if (Api.rol != 'ADMIN') return Scaffold(appBar: AppBar(title: const Text('Administrador')), body: const Center(child: Text('Solo para el administrador.')));
    Widget kpi(String t, String v) => Card(child: ListTile(title: Text(v, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w800, color: naranja)), subtitle: Text(t)));
    return Scaffold(
      appBar: AppBar(title: const Text('Panel de administrador')),
      body: error != null
          ? Center(child: Text(error!, style: const TextStyle(color: rojo)))
          : kpis == null
              ? const Center(child: CircularProgressIndicator())
              : RefreshIndicator(
                  onRefresh: _c,
                  child: ListView(padding: const EdgeInsets.all(14), children: [
                    kpi('Pedidos pagados', '${kpis!['totalPedidosPagados']}'),
                    kpi('Ventas', money(kpis!['totalVentas'])),
                    kpi('Envíos cobrados', money(kpis!['totalEnvios'])),
                    kpi('Donado a causas', money(kpis!['totalDonadoACausas'])),
                    kpi('Comisión JJM', money(kpis!['totalComisionJJM'])),
                    FilledButton.icon(onPressed: _distribuidor, icon: const Icon(Icons.person_add), label: const Text('Crear distribuidor')),
                    const SizedBox(height: 16),
                    const Text('Vendedores por aprobar', style: TextStyle(fontWeight: FontWeight.bold)),
                    if (pendientes.isEmpty) const Padding(padding: EdgeInsets.all(8), child: Text('Ninguno pendiente.', style: TextStyle(color: Colors.black54))),
                    for (final v in pendientes)
                      Card(child: ListTile(
                        title: Text(v['nombreComercial']), subtitle: Text('${v['correo']}\nRFC ${v['rfc'] ?? '-'} · convenio ${v['porcentajeConvenio']}%'), isThreeLine: true,
                        trailing: Row(mainAxisSize: MainAxisSize.min, children: [
                          IconButton(icon: const Icon(Icons.check_circle, color: verde), onPressed: () async { await Api.patch('/admin/vendedores/${v['id']}/verificacion?aprobar=true'); _c(); }),
                          IconButton(icon: const Icon(Icons.cancel, color: rojo), onPressed: () async { await Api.patch('/admin/vendedores/${v['id']}/verificacion?aprobar=false'); _c(); }),
                        ]),
                      )),
                    const SizedBox(height: 90),
                  ]),
                ),
    );
  }
}
