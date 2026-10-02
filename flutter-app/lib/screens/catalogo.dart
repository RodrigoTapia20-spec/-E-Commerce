import 'package:flutter/material.dart';
import '../api.dart';
import '../cart.dart';
import '../util.dart';
import 'auth.dart';
import 'carrito.dart';
import 'distribuidor.dart';
import 'admin.dart';
import 'pedidos.dart';
import 'producto.dart';
import 'quejas.dart';
import 'vendedor.dart';

class CatalogoScreen extends StatefulWidget {
  const CatalogoScreen({super.key});
  @override
  State<CatalogoScreen> createState() => _CatalogoScreenState();
}

class _CatalogoScreenState extends State<CatalogoScreen> {
  final buscador = TextEditingController();
  List cats = [];
  int? cat;
  String? texto;
  late Future<List> futuro;

  @override
  void initState() {
    super.initState();
    Api.sesion.addListener(_refrescar);
    _cargar();
    Api.get('/categorias').then((v) { if (mounted) setState(() => cats = v as List); }).catchError((_) {});
  }

  @override
  void dispose() { Api.sesion.removeListener(_refrescar); super.dispose(); }
  void _refrescar() { if (mounted) setState(() {}); }

  void _cargar() {
    final q = (texto != null && texto!.isNotEmpty) ? '?buscar=${Uri.encodeQueryComponent(texto!)}' : (cat != null ? '?categoria=$cat' : '');
    futuro = Api.get('/productos$q').then((v) => v as List);
  }

  @override
  Widget build(BuildContext context) {
    final rol = Api.rol;
    return Scaffold(
      appBar: AppBar(
        titleSpacing: 8,
        title: Row(children: [
          Container(
            height: 34, padding: const EdgeInsets.symmetric(horizontal: 6),
            decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(8)),
            child: Image.asset('assets/images/logo.png', errorBuilder: (_, __, ___) => const Center(child: Text('JJM', style: TextStyle(color: naranja, fontWeight: FontWeight.w900)))),
          ),
          const SizedBox(width: 8),
          const Text('con causa', style: TextStyle(fontSize: 13, color: naranja)),
        ]),
        actions: [
          if (rol != 'DISTRIBUIDOR')
            ListenableBuilder(
              listenable: Cart.i,
              builder: (_, __) => Stack(alignment: Alignment.center, children: [
                IconButton(icon: const Icon(Icons.shopping_cart), onPressed: () => ir(context, 'carrito', const CarritoScreen())),
                if (Cart.i.totalItems > 0)
                  Positioned(right: 6, top: 6, child: CircleAvatar(radius: 9, backgroundColor: naranja, child: Text('${Cart.i.totalItems}', style: const TextStyle(fontSize: 10, color: azul, fontWeight: FontWeight.bold)))),
              ]),
            ),
          PopupMenuButton<String>(
            icon: const Icon(Icons.account_circle),
            onSelected: (v) async {
              switch (v) {
                case 'login': ir(context, 'login', const LoginScreen()); break;
                case 'registro': ir(context, 'login', const RegistroScreen()); break;
                case 'pedidos': ir(context, 'pedidos', const PedidosScreen()); break;
                case 'quejas': ir(context, 'quejas', const QuejasScreen()); break;
                case 'vendedor': ir(context, 'vendedor', const VendedorScreen()); break;
                case 'distribuidor': ir(context, 'distribuidor', const DistribuidorScreen()); break;
                case 'admin': ir(context, 'admin', const AdminScreen()); break;
                case 'salir': await Api.cerrarSesion(); Cart.i.vaciar(); if (context.mounted) aviso(context, 'Sesión cerrada'); break;
              }
            },
            itemBuilder: (_) => [
              if (!Api.logueado) ...const [
                PopupMenuItem(value: 'login', child: Text('Iniciar sesión')),
                PopupMenuItem(value: 'registro', child: Text('Registrar')),
              ] else ...[
                PopupMenuItem(enabled: false, child: Text('Hola, ${Api.nombre ?? ''}')),
                if (rol == 'CLIENTE' || rol == 'ALIADO' || rol == 'ADMIN') const PopupMenuItem(value: 'pedidos', child: Text('Mis pedidos')),
                if (rol == 'ALIADO') const PopupMenuItem(value: 'vendedor', child: Text('Panel de vendedor')),
                if (rol == 'DISTRIBUIDOR' || rol == 'ADMIN') const PopupMenuItem(value: 'distribuidor', child: Text('Panel de distribuidor')),
                if (rol == 'ADMIN') const PopupMenuItem(value: 'admin', child: Text('Panel de administrador')),
                const PopupMenuItem(value: 'salir', child: Text('Cerrar sesión')),
              ],
              const PopupMenuItem(value: 'quejas', child: Text('Quejas y sugerencias')),
            ],
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: () async { setState(_cargar); await futuro; },
        child: ListView(padding: const EdgeInsets.all(12), children: [
          TextField(
            controller: buscador,
            textInputAction: TextInputAction.search,
            decoration: InputDecoration(hintText: 'Buscar productos y servicios con causa...', filled: true, fillColor: Colors.white,
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(10)), prefixIcon: const Icon(Icons.search)),
            onSubmitted: (v) => setState(() { texto = v.trim(); cat = null; _cargar(); }),
          ),
          SizedBox(
            height: 46,
            child: ListView(scrollDirection: Axis.horizontal, children: [
              Padding(padding: const EdgeInsets.only(right: 6, top: 6), child: ChoiceChip(label: const Text('Todo'), selected: cat == null && (texto ?? '').isEmpty,
                  onSelected: (_) => setState(() { cat = null; texto = null; buscador.clear(); _cargar(); }))),
              for (final c in cats)
                Padding(padding: const EdgeInsets.only(right: 6, top: 6), child: ChoiceChip(label: Text(c['nombre']), selected: cat == c['id'],
                    onSelected: (_) => setState(() { cat = c['id']; texto = null; buscador.clear(); _cargar(); }))),
            ]),
          ),
          FutureBuilder<List>(
            future: futuro,
            builder: (_, s) {
              if (s.connectionState != ConnectionState.done) return const Padding(padding: EdgeInsets.all(40), child: Center(child: CircularProgressIndicator()));
              if (s.hasError) return Padding(padding: const EdgeInsets.all(16), child: Text('${s.error}', style: const TextStyle(color: rojo)));
              final l = s.data!;
              if (l.isEmpty) return const Padding(padding: EdgeInsets.all(30), child: Center(child: Text('No hay productos por ahora.')));
              return Column(children: [for (final p in l) _Tarjeta(Map<String, dynamic>.from(p))]);
            },
          ),
          const SizedBox(height: 80),
        ]),
      ),
    );
  }
}

class _Tarjeta extends StatelessWidget {
  final Map<String, dynamic> p;
  const _Tarjeta(this.p);

  @override
  Widget build(BuildContext context) {
    final pr = p['promocion'];
    final aliado = p['aliado'];
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () => ir(context, 'producto', ProductoScreen(id: p['id'])),
        child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Stack(children: [
            fotoRed(imagenPrincipal(p), h: 130, w: 120),
            if (pr != null)
              Positioned(top: 6, left: 6, child: Container(padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2), decoration: BoxDecoration(color: rojo, borderRadius: BorderRadius.circular(6)),
                  child: Text('-${(pr['porcentajeDescuento'] as num).toStringAsFixed(0)}%', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 12)))),
          ]),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.all(10),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(p['nombre'], maxLines: 2, overflow: TextOverflow.ellipsis, style: const TextStyle(fontWeight: FontWeight.w600)),
                const SizedBox(height: 4),
                if (pr != null)
                  Row(children: [
                    Text(money(pr['precioPromocional']), style: const TextStyle(color: rojo, fontWeight: FontWeight.w800, fontSize: 17)),
                    const SizedBox(width: 6),
                    Text(money(p['precio']), style: const TextStyle(decoration: TextDecoration.lineThrough, color: Colors.black54, fontSize: 12)),
                  ])
                else
                  Text(money(p['precio']), style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 17)),
                estrellas(aliado['calificacionPromedio'], aliado['totalCalificaciones']),
                Wrap(spacing: 4, children: [
                  if (p['verificadoIa'] == true) _badge('✔ Verificado IA', verde),
                  if (pr != null && pr['esCausaSocial'] == true) _badge('🤝 ${pr['fundacionBeneficiaria']}', const Color(0xFFA95A00)),
                  if (pr != null) _badge(temporada(pr['tipoTemporada']), const Color(0xFFAD1457)),
                  _badge(aliado['nivel'], const Color(0xFF283593)),
                ]),
                Text('Vende: ${aliado['nombreComercial']}', style: const TextStyle(fontSize: 11, color: Colors.black54)),
              ]),
            ),
          ),
        ]),
      ),
    );
  }

  Widget _badge(String t, Color c) => Container(
        margin: const EdgeInsets.only(top: 3),
        padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
        decoration: BoxDecoration(color: c.withOpacity(.12), borderRadius: BorderRadius.circular(10)),
        child: Text(t, style: TextStyle(fontSize: 10.5, color: c, fontWeight: FontWeight.w600)),
      );
}
