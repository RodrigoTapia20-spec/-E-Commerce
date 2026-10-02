import 'package:flutter/material.dart';
import 'package:video_player/video_player.dart';
import '../api.dart';
import '../cart.dart';
import '../util.dart';

class ProductoScreen extends StatefulWidget {
  final int id;
  const ProductoScreen({super.key, required this.id});
  @override
  State<ProductoScreen> createState() => _ProductoScreenState();
}

class _ProductoScreenState extends State<ProductoScreen> {
  Map<String, dynamic>? p;
  List opiniones = [];
  String? error;
  String modalidad = 'NORMAL';
  int idx = 0, cant = 1;
  Map<String, dynamic>? paq;

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  Future<void> _cargar() async {
    try {
      final r = Map<String, dynamic>.from(await Api.get('/productos/${widget.id}'));
      final ops = await Api.get('/calificaciones/aliado/${r['aliado']['id']}').catchError((_) => []);
      final paqs = (r['paqueterias'] as List);
      setState(() {
        p = r;
        opiniones = ops as List;
        if (paqs.isNotEmpty) paq = Map<String, dynamic>.from(paqs.first);
      });
    } catch (e) {
      setState(() => error = '$e');
    }
  }

  num _envio() {
    if (p!['tipo'] != 'PRODUCTO' || paq == null) return 0;
    final peso = (p!['pesoKg'] as num?) ?? 0.5;
    return (paq!['costoBase'] as num) + (paq!['costoPorKg'] as num) * peso * cant;
  }

  @override
  Widget build(BuildContext context) {
    if (error != null) return Scaffold(appBar: AppBar(), body: Center(child: Text(error!, style: const TextStyle(color: rojo))));
    if (p == null) return Scaffold(appBar: AppBar(), body: const Center(child: CircularProgressIndicator()));
    final pr = p!['promocion'];
    final aliado = p!['aliado'];
    final media = p!['media'] as List;
    final fisico = p!['tipo'] == 'PRODUCTO';
    final unit = (modalidad == 'PROMOCION' && pr != null) ? pr['precioPromocional'] as num : p!['precio'] as num;
    final envio = _envio();
    final agotado = fisico && (p!['existencia'] as int) < 1;

    return Scaffold(
      appBar: AppBar(title: Text(p!['nombre'], overflow: TextOverflow.ellipsis)),
      body: ListView(children: [
        if (media.isNotEmpty) _Visor(key: ValueKey(media[idx]['url']), m: media[idx]),
        if (media.length > 1)
          SizedBox(
            height: 74,
            child: ListView.separated(
              scrollDirection: Axis.horizontal, padding: const EdgeInsets.all(8), itemCount: media.length,
              separatorBuilder: (_, __) => const SizedBox(width: 8),
              itemBuilder: (_, i) => GestureDetector(
                onTap: () => setState(() => idx = i),
                child: Container(
                  width: 58, decoration: BoxDecoration(border: Border.all(color: i == idx ? naranja : Colors.transparent, width: 2), borderRadius: BorderRadius.circular(8)),
                  child: media[i]['tipo'] == 'VIDEO'
                      ? const ColoredBox(color: Colors.black, child: Icon(Icons.play_circle, color: Colors.white))
                      : fotoRed(media[i]['url'], h: 58, w: 58),
                ),
              ),
            ),
          ),
        Padding(
          padding: const EdgeInsets.all(14),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(p!['nombre'], style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
            const SizedBox(height: 4),
            estrellas(aliado['calificacionPromedio'], aliado['totalCalificaciones']),
            Text('Vende: ${aliado['nombreComercial']} · Nivel ${aliado['nivel']}', style: const TextStyle(color: Colors.black54, fontSize: 12)),
            if (aliado['causaSocial'] != null) Text('🤝 ${aliado['causaSocial']}', style: const TextStyle(color: verde, fontSize: 12)),
            const SizedBox(height: 8),
            Text(p!['descripcion'] ?? ''),
            const Divider(height: 28),
            const Text('¿Cómo quieres comprarlo?', style: TextStyle(fontWeight: FontWeight.bold)),
            RadioListTile<String>(
              value: 'NORMAL', groupValue: modalidad, onChanged: (v) => setState(() => modalidad = v!),
              title: const Text('Costo normal'), subtitle: Text(money(p!['precio']), style: const TextStyle(fontWeight: FontWeight.bold)),
            ),
            if (pr != null)
              RadioListTile<String>(
                value: 'PROMOCION', groupValue: modalidad, onChanged: (v) => setState(() => modalidad = v!),
                title: Text('${temporada(pr['tipoTemporada'])}: ${pr['titulo']}'),
                subtitle: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text('${money(pr['precioPromocional'])}  (-${(pr['porcentajeDescuento'] as num).toStringAsFixed(0)}%)', style: const TextStyle(color: rojo, fontWeight: FontWeight.bold)),
                  if (pr['esCausaSocial'] == true)
                    Text('🤝 El ${(pr['porcentajeDonacion'] as num).toStringAsFixed(0)}% de tu compra apoya a ${pr['fundacionBeneficiaria']}', style: const TextStyle(color: verde, fontSize: 12)),
                  Text('Vigente hasta ${fecha(pr['fechaFin'])}', style: const TextStyle(fontSize: 11)),
                ]),
              ),
            if (fisico) ...[
              const SizedBox(height: 8),
              DropdownButtonFormField<int>(
                value: paq?['id'], decoration: campo('Paquetería'),
                items: [for (final q in (p!['paqueterias'] as List)) DropdownMenuItem<int>(value: q['id'], child: Text('${q['nombre']} · ${q['diasMin']}-${q['diasMax']} días', overflow: TextOverflow.ellipsis))],
                onChanged: (v) => setState(() => paq = Map<String, dynamic>.from((p!['paqueterias'] as List).firstWhere((q) => q['id'] == v))),
              ),
            ] else
              const Padding(padding: EdgeInsets.only(top: 8), child: Text('Servicio: no requiere envío.', style: TextStyle(color: Colors.black54))),
            const SizedBox(height: 10),
            Row(children: [
              const Text('Cantidad'), const SizedBox(width: 12),
              IconButton(onPressed: cant > 1 ? () => setState(() => cant--) : null, icon: const Icon(Icons.remove_circle_outline)),
              Text('$cant', style: const TextStyle(fontWeight: FontWeight.bold)),
              IconButton(onPressed: cant < (fisico ? (p!['existencia'] as int) : 99) ? () => setState(() => cant++) : null, icon: const Icon(Icons.add_circle_outline)),
              if (fisico) Text('(${p!['existencia']} disp.)', style: const TextStyle(fontSize: 12, color: Colors.black54)),
            ]),
            Card(
              color: const Color(0xFFFAFAFA),
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Column(children: [
                  _fila('Producto ($cant × ${money(unit)})', money(unit * cant)),
                  if (fisico && paq != null) _fila('Envío ${paq!['nombre']}', money(envio)),
                  const Divider(),
                  _fila('Total', money(unit * cant + envio), fuerte: true),
                ]),
              ),
            ),
            SizedBox(
              width: double.infinity,
              child: FilledButton.icon(
                icon: const Icon(Icons.add_shopping_cart),
                onPressed: agotado ? null : () {
                  Cart.i.agregar(p!, modalidad, paq, cant);
                  aviso(context, 'Agregado al carrito 🛒');
                  Navigator.pop(context);
                },
                label: Text(agotado ? 'Agotado' : 'Agregar al carrito'),
              ),
            ),
            const Divider(height: 30),
            const Text('Opiniones sobre el vendedor', style: TextStyle(fontWeight: FontWeight.bold)),
            if (opiniones.isEmpty) const Padding(padding: EdgeInsets.only(top: 6), child: Text('Aún no tiene opiniones.', style: TextStyle(color: Colors.black54))),
            for (final o in opiniones.take(6))
              ListTile(dense: true, contentPadding: EdgeInsets.zero, title: Text('${'★' * (o['calificacion'] as int)}  ${o['cliente']}', style: const TextStyle(color: Color(0xFFF5A623))), subtitle: Text(o['comentario'] ?? '')),
            const SizedBox(height: 90),
          ]),
        ),
      ]),
    );
  }

  Widget _fila(String a, String b, {bool fuerte = false}) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 2),
        child: Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
          Flexible(child: Text(a, style: TextStyle(fontWeight: fuerte ? FontWeight.bold : null))),
          Text(b, style: TextStyle(fontWeight: FontWeight.bold, fontSize: fuerte ? 17 : 14)),
        ]),
      );
}

/// Muestra una imagen o reproduce un video de la galería.
class _Visor extends StatefulWidget {
  final Map m;
  const _Visor({super.key, required this.m});
  @override
  State<_Visor> createState() => _VisorState();
}

class _VisorState extends State<_Visor> {
  VideoPlayerController? vc;

  @override
  void initState() {
    super.initState();
    if (widget.m['tipo'] == 'VIDEO') {
      vc = VideoPlayerController.networkUrl(Uri.parse(widget.m['url']))
        ..initialize().then((_) {
          if (!mounted) return;
          vc!.setVolume(widget.m['conAudio'] == true ? 1 : 0);
          setState(() {});
        });
    }
  }

  @override
  void dispose() { vc?.dispose(); super.dispose(); }

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 300, color: Colors.black, alignment: Alignment.center,
      child: vc == null
          ? Image.network(widget.m['url'], fit: BoxFit.contain, errorBuilder: (_, __, ___) => const Icon(Icons.broken_image, color: Colors.white))
          : !vc!.value.isInitialized
              ? const CircularProgressIndicator()
              : Stack(alignment: Alignment.center, children: [
                  AspectRatio(aspectRatio: vc!.value.aspectRatio, child: VideoPlayer(vc!)),
                  IconButton(
                    iconSize: 56, color: Colors.white70,
                    icon: Icon(vc!.value.isPlaying ? Icons.pause_circle : Icons.play_circle),
                    onPressed: () => setState(() => vc!.value.isPlaying ? vc!.pause() : vc!.play()),
                  ),
                ]),
    );
  }
}
