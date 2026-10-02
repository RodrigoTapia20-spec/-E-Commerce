import 'package:flutter/material.dart';
import '../api.dart';
import '../cart.dart';
import '../util.dart';
import 'auth.dart';
import 'pedidos.dart';

const metodosPago = {
  'TARJETA_CREDITO': 'Tarjeta de crédito', 'TARJETA_DEBITO': 'Tarjeta de débito', 'TRANSFERENCIA': 'Transferencia',
  'OXXO': 'OXXO', 'SEVEN': 'Seven', 'VALE_DESPENSA': 'Vale de despensa', 'BILLETERA_DIGITAL': 'Billetera digital',
};

class CarritoScreen extends StatefulWidget {
  const CarritoScreen({super.key});
  @override
  State<CarritoScreen> createState() => _CarritoScreenState();
}

class _CarritoScreenState extends State<CarritoScreen> {
  final dir = TextEditingController(), cp = TextEditingController();
  String metodo = 'TARJETA_CREDITO';
  bool pagando = false;
  String? error;

  Future<void> _pagar() async {
    if (!Api.logueado) {
      aviso(context, 'Inicia sesión para completar tu compra.');
      ir(context, 'login', const LoginScreen());
      return;
    }
    final nav = Navigator.of(context);
    setState(() { pagando = true; error = null; });
    try {
      final r = await Api.post('/pedidos', {
        'metodoPago': metodo, 'direccionEnvio': dir.text.trim(), 'cpEnvio': cp.text.trim(),
        'items': [for (final i in Cart.i.items) {'idProducto': i.p['id'], 'cantidad': i.cantidad, 'modalidad': i.modalidad, 'idPaqueteria': i.paq?['id']}],
      });
      if (r['estatus'] == 'CANCELADO') {
        setState(() => error = 'El pago fue rechazado. Intenta con otro método.');
      } else {
        Cart.i.vaciar();
        if (!mounted) return;
        await showDialog(context: context, builder: (_) => AlertDialog(
          title: Text('✅ Pedido #${r['id']} confirmado'),
          content: SingleChildScrollView(child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text('Total pagado: ${money(r['total'])} (envío ${money(r['costoEnvio'])})'),
            if ((r['montoCausa'] as num) > 0) Text('💚 Donado a causas: ${money(r['montoCausa'])}', style: const TextStyle(color: verde)),
            const SizedBox(height: 8),
            for (final d in (r['detalles'] as List).where((d) => d['envio'] != null))
              Padding(padding: const EdgeInsets.only(bottom: 6), child: Text('📦 ${d['nombre']}\n${d['envio']['paqueteria']} · guía ${d['envio']['numeroGuia']}\nLlega aprox. ${fecha(d['envio']['fechaEstimada'])}', style: const TextStyle(fontSize: 13))),
          ])),
          actions: [TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cerrar')), FilledButton(onPressed: () { Navigator.pop(context); }, child: const Text('OK'))],
        ));
        nav.pop();
        nav.push(MaterialPageRoute(settings: const RouteSettings(name: 'pedidos'), builder: (_) => const PedidosScreen()));
      }
    } catch (e) {
      setState(() => error = '$e');
    }
    if (mounted) setState(() => pagando = false);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Mi carrito')),
      body: ListenableBuilder(
        listenable: Cart.i,
        builder: (_, __) {
          final c = Cart.i;
          if (c.items.isEmpty) return const Center(child: Text('Tu carrito está vacío.'));
          return ListView(padding: const EdgeInsets.all(12), children: [
            for (final x in c.items)
              Card(child: Padding(padding: const EdgeInsets.all(10), child: Row(children: [
                fotoRed(imagenPrincipal(x.p), h: 70, w: 70),
                const SizedBox(width: 10),
                Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text(x.p['nombre'], style: const TextStyle(fontWeight: FontWeight.w600)),
                  Text(x.modalidad == 'PROMOCION' ? '${temporada(x.p['promocion']['tipoTemporada'])} · ${money(x.precioUnit)}' : 'Costo normal · ${money(x.precioUnit)}', style: TextStyle(fontSize: 12, color: x.modalidad == 'PROMOCION' ? rojo : Colors.black54)),
                  if (x.modalidad == 'PROMOCION' && x.p['promocion']['esCausaSocial'] == true)
                    Text('🤝 ${x.p['promocion']['fundacionBeneficiaria']}', style: const TextStyle(fontSize: 11, color: verde)),
                  if (x.fisico && x.paq != null) Text('🚚 ${x.paq!['nombre']} · ${x.paq!['diasMin']}-${x.paq!['diasMax']} días · ${money(x.envio)}', style: const TextStyle(fontSize: 12)),
                  Row(children: [
                    IconButton(visualDensity: VisualDensity.compact, onPressed: () => c.cambiarCantidad(x, x.cantidad - 1), icon: const Icon(Icons.remove_circle_outline)),
                    Text('${x.cantidad}'),
                    IconButton(visualDensity: VisualDensity.compact, onPressed: () => c.cambiarCantidad(x, x.cantidad + 1), icon: const Icon(Icons.add_circle_outline)),
                    const Spacer(),
                    Text(money(x.importe), style: const TextStyle(fontWeight: FontWeight.bold)),
                    IconButton(onPressed: () => c.quitar(x), icon: const Icon(Icons.delete_outline, color: rojo)),
                  ]),
                ])),
              ]))),
            if (c.hayFisicos) ...[
              const SizedBox(height: 8),
              TextField(controller: dir, decoration: campo('Dirección de entrega', ayuda: 'Calle, número, colonia y ciudad')),
              const SizedBox(height: 10),
              TextField(controller: cp, keyboardType: TextInputType.number, maxLength: 5, decoration: campo('Código postal')),
            ],
            DropdownButtonFormField<String>(
              value: metodo, decoration: campo('Método de pago'),
              items: [for (final e in metodosPago.entries) DropdownMenuItem(value: e.key, child: Text(e.value))],
              onChanged: (v) => setState(() => metodo = v!),
            ),
            const SizedBox(height: 10),
            Card(child: Padding(padding: const EdgeInsets.all(14), child: Column(children: [
              _l('Subtotal', money(c.subtotal)), _l('Envío', money(c.envio)), const Divider(), _l('Total', money(c.total), fuerte: true),
            ]))),
            if (error != null) Padding(padding: const EdgeInsets.all(8), child: Text(error!, style: const TextStyle(color: rojo))),
            FilledButton(onPressed: pagando ? null : _pagar, child: Text(pagando ? 'Procesando…' : 'Pagar ${money(c.total)}')),
            const SizedBox(height: 90),
          ]);
        },
      ),
    );
  }

  Widget _l(String a, String b, {bool fuerte = false}) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 2),
        child: Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
          Text(a, style: TextStyle(fontWeight: fuerte ? FontWeight.bold : null)),
          Text(b, style: TextStyle(fontWeight: FontWeight.bold, fontSize: fuerte ? 18 : 14)),
        ]),
      );
}
