import 'dart:io';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:video_player/video_player.dart';
import '../api.dart';
import '../util.dart';

class VendedorScreen extends StatelessWidget {
  const VendedorScreen({super.key});
  @override
  Widget build(BuildContext context) {
    if (Api.rol != 'ALIADO') return Scaffold(appBar: AppBar(title: const Text('Panel de vendedor')), body: const Center(child: Text('Esta sección es solo para vendedores.')));
    return DefaultTabController(
      length: 4,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Panel de vendedor'),
          bottom: const TabBar(isScrollable: true, labelColor: naranja, unselectedLabelColor: Colors.white70, indicatorColor: naranja,
              tabs: [Tab(text: 'Resumen'), Tab(text: 'Publicar'), Tab(text: 'Mis productos'), Tab(text: 'Campañas')]),
        ),
        body: const TabBarView(children: [_Resumen(), _Publicar(), _MisProductos(), _Campanas()]),
      ),
    );
  }
}

class _Resumen extends StatefulWidget {
  const _Resumen();
  @override
  State<_Resumen> createState() => _ResumenState();
}

class _ResumenState extends State<_Resumen> {
  Map? perfil, reporte;
  String? error;
  @override
  void initState() {
    super.initState();
    Future.wait([Api.get('/aliados/mi-perfil'), Api.get('/aliados/mi-reporte')])
        .then((r) { if (mounted) setState(() { perfil = r[0] as Map; reporte = r[1] as Map; }); })
        .catchError((e) { if (mounted) setState(() => error = '$e'); });
  }

  @override
  Widget build(BuildContext context) {
    if (error != null) return Center(child: Text(error!, style: const TextStyle(color: rojo)));
    if (perfil == null) return const Center(child: CircularProgressIndicator());
    final rk = perfil!['ranking'];
    Widget kpi(String t, String v) => Card(child: ListTile(title: Text(v, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w800, color: naranja)), subtitle: Text(t)));
    return ListView(padding: const EdgeInsets.all(14), children: [
      Text(perfil!['nombreComercial'], style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
      if (perfil!['estatusVerificacion'] != 'APROBADO')
        Card(color: const Color(0xFFFFF3E0), child: Padding(padding: const EdgeInsets.all(12), child: Text('Tu cuenta está en estatus ${perfil!['estatusVerificacion']}. El administrador debe aprobarla para que puedas publicar.'))),
      kpi('Nivel de ranking', '${rk['nivel']}  ${rk['calificacionPromedio'] ?? '-'} ★ (${rk['totalCalificaciones']})'),
      kpi('Unidades vendidas', '${reporte!['unidadesVendidas']}'),
      kpi('Ventas totales', money(reporte!['ventasTotales'])),
      kpi('Monto recibido', money(reporte!['montoRecibido'])),
      kpi('Donado a causas', money(reporte!['montoDonado'])),
    ]);
  }
}

class _Publicar extends StatefulWidget {
  const _Publicar();
  @override
  State<_Publicar> createState() => _PublicarState();
}

class _PublicarState extends State<_Publicar> with AutomaticKeepAliveClientMixin {
  @override
  bool get wantKeepAlive => true;
  final nombre = TextEditingController(), desc = TextEditingController(), precio = TextEditingController(),
      peso = TextEditingController(text: '0.5'), stock = TextEditingController(text: '1');
  String tipo = 'PRODUCTO';
  int? categoria;
  List cats = [], paqs = [];
  final elegidas = <int>{};
  final List<Map<String, dynamic>> media = [];
  bool subiendo = false, publicando = false;
  String? error;

  @override
  void initState() {
    super.initState();
    Api.get('/categorias').then((v) { if (mounted) setState(() { cats = v as List; if (cats.isNotEmpty) categoria = cats.first['id']; }); }).catchError((_) {});
    Api.get('/envios/paqueterias').then((v) { if (mounted) setState(() { paqs = v as List; elegidas.addAll(paqs.map<int>((q) => q['id'] as int)); }); }).catchError((_) {});
  }

  Future<void> _fotos() async {
    final archivos = await ImagePicker().pickMultiImage(imageQuality: 85);
    for (final f in archivos) {
      if (media.length >= 7) { if (mounted) aviso(context, 'Máximo 7 archivos por producto.', error: true); break; }
      await _subir(f.path, 'IMAGEN', 0);
    }
  }

  Future<void> _video() async {
    if (media.length >= 7) { aviso(context, 'Máximo 7 archivos por producto.', error: true); return; }
    final f = await ImagePicker().pickVideo(source: ImageSource.gallery, maxDuration: const Duration(seconds: 15));
    if (f == null) return;
    final vc = VideoPlayerController.file(File(f.path));
    await vc.initialize();
    final seg = vc.value.duration.inSeconds;
    await vc.dispose();
    if (seg > 15) { if (mounted) aviso(context, 'El video dura $seg s; el máximo es 15 s.', error: true); return; }
    await _subir(f.path, 'VIDEO', seg < 1 ? 1 : seg);
  }

  Future<void> _subir(String ruta, String tipoArchivo, int seg) async {
    setState(() => subiendo = true);
    try {
      final r = await Api.subir(ruta);
      setState(() => media.add({'tipo': tipoArchivo, 'url': r['url'], 'duracion': seg, 'conAudio': true, 'local': ruta}));
    } catch (e) { if (mounted) aviso(context, '$e', error: true); }
    if (mounted) setState(() => subiendo = false);
  }

  Future<void> _publicar() async {
    if (media.length < 5 || media.length > 7) { setState(() => error = 'La galería debe tener de 5 a 7 archivos (llevas ${media.length}).'); return; }
    setState(() { publicando = true; error = null; });
    final fisico = tipo == 'PRODUCTO';
    try {
      await Api.post('/aliados/productos', {
        'idCategoria': categoria, 'tipo': tipo, 'nombre': nombre.text.trim(), 'descripcion': desc.text.trim(),
        'precio': double.tryParse(precio.text) ?? 0, 'existencia': int.tryParse(stock.text) ?? 0,
        'pesoKg': fisico ? (double.tryParse(peso.text) ?? 0.5) : null,
        'idsPaqueterias': fisico ? elegidas.toList() : [],
        'media': [for (final m in media) {'tipo': m['tipo'], 'url': m['url'], 'duracionSegundos': m['tipo'] == 'VIDEO' ? m['duracion'] : null, 'conAudio': m['tipo'] == 'VIDEO' ? m['conAudio'] : null}],
      });
      if (mounted) {
        aviso(context, '¡Producto publicado!');
        setState(() { nombre.clear(); desc.clear(); precio.clear(); media.clear(); });
      }
    } catch (e) { setState(() => error = '$e'); }
    if (mounted) setState(() => publicando = false);
  }

  @override
  Widget build(BuildContext context) {
    super.build(context);
    final ok = media.length >= 5 && media.length <= 7;
    return ListView(padding: const EdgeInsets.all(14), children: [
      DropdownButtonFormField<String>(value: tipo, decoration: campo('Tipo'),
          items: const [DropdownMenuItem(value: 'PRODUCTO', child: Text('Producto')), DropdownMenuItem(value: 'SERVICIO', child: Text('Servicio'))],
          onChanged: (v) => setState(() => tipo = v!)),
      const SizedBox(height: 10),
      DropdownButtonFormField<int>(value: categoria, decoration: campo('Categoría'),
          items: [for (final c in cats) DropdownMenuItem<int>(value: c['id'], child: Text(c['nombre']))], onChanged: (v) => setState(() => categoria = v)),
      const SizedBox(height: 10),
      TextField(controller: nombre, decoration: campo('Nombre')),
      const SizedBox(height: 10),
      TextField(controller: desc, maxLines: 3, decoration: campo('Descripción')),
      const SizedBox(height: 10),
      Row(children: [
        Expanded(child: TextField(controller: precio, keyboardType: TextInputType.number, decoration: campo('Precio normal (\$)'))),
        const SizedBox(width: 10),
        Expanded(child: TextField(controller: stock, keyboardType: TextInputType.number, decoration: campo('Existencia'))),
      ]),
      if (tipo == 'PRODUCTO') ...[
        const SizedBox(height: 10),
        TextField(controller: peso, keyboardType: TextInputType.number, decoration: campo('Peso (kg)', ayuda: 'Se usa para calcular el envío')),
        const SizedBox(height: 8),
        const Text('Paqueterías habilitadas', style: TextStyle(fontWeight: FontWeight.bold)),
        for (final q in paqs)
          CheckboxListTile(
            dense: true, contentPadding: EdgeInsets.zero, value: elegidas.contains(q['id']),
            title: Text('${q['nombre']} (${q['diasMin']}-${q['diasMax']} días)'),
            onChanged: (v) => setState(() => v == true ? elegidas.add(q['id']) : elegidas.remove(q['id'])),
          ),
      ],
      const Divider(height: 26),
      Text('Galería: ${media.length} de 5 a 7 archivos', style: TextStyle(fontWeight: FontWeight.bold, color: ok ? verde : rojo)),
      const Text('Imágenes y videos de máx. 15 segundos (con o sin audio).', style: TextStyle(fontSize: 12, color: Colors.black54)),
      const SizedBox(height: 6),
      Row(children: [
        Expanded(child: OutlinedButton.icon(onPressed: subiendo ? null : _fotos, icon: const Icon(Icons.photo_library), label: const Text('Imágenes'))),
        const SizedBox(width: 8),
        Expanded(child: OutlinedButton.icon(onPressed: subiendo ? null : _video, icon: const Icon(Icons.videocam), label: const Text('Video'))),
      ]),
      if (subiendo) const Padding(padding: EdgeInsets.all(8), child: LinearProgressIndicator()),
      const SizedBox(height: 8),
      Wrap(spacing: 8, runSpacing: 8, children: [
        for (var i = 0; i < media.length; i++)
          SizedBox(
            width: 100,
            child: Column(children: [
              Stack(children: [
                media[i]['tipo'] == 'IMAGEN'
                    ? Image.file(File(media[i]['local']), width: 100, height: 80, fit: BoxFit.cover)
                    : Container(width: 100, height: 80, color: Colors.black, child: Center(child: Text('🎬 ${media[i]['duracion']} s', style: const TextStyle(color: Colors.white)))),
                Positioned(right: 0, top: 0, child: InkWell(onTap: () => setState(() => media.removeAt(i)), child: const CircleAvatar(radius: 11, backgroundColor: Colors.black54, child: Icon(Icons.close, size: 14, color: Colors.white)))),
              ]),
              if (media[i]['tipo'] == 'VIDEO')
                Row(children: [Checkbox(visualDensity: VisualDensity.compact, value: media[i]['conAudio'] == true, onChanged: (v) => setState(() => media[i]['conAudio'] = v)), const Text('🔊', style: TextStyle(fontSize: 12))]),
            ]),
          ),
      ]),
      if (error != null) Padding(padding: const EdgeInsets.only(top: 10), child: Text(error!, style: const TextStyle(color: rojo))),
      const SizedBox(height: 12),
      FilledButton(onPressed: publicando ? null : _publicar, child: Text(publicando ? 'Publicando…' : 'Publicar')),
      const SizedBox(height: 90),
    ]);
  }
}

class _MisProductos extends StatefulWidget {
  const _MisProductos();
  @override
  State<_MisProductos> createState() => _MisProductosState();
}

class _MisProductosState extends State<_MisProductos> {
  late Future<List> futuro;
  @override
  void initState() { super.initState(); _c(); }
  void _c() => futuro = Api.get('/aliados/productos').then((v) => v as List);

  @override
  Widget build(BuildContext context) => RefreshIndicator(
        onRefresh: () async { setState(_c); await futuro; },
        child: FutureBuilder<List>(
          future: futuro,
          builder: (_, s) {
            if (s.connectionState != ConnectionState.done) return const Center(child: CircularProgressIndicator());
            if (s.hasError) return Center(child: Text('${s.error}', style: const TextStyle(color: rojo)));
            final l = s.data!;
            if (l.isEmpty) return ListView(children: const [Padding(padding: EdgeInsets.all(30), child: Center(child: Text('Aún no has publicado productos.')))]);
            return ListView(padding: const EdgeInsets.all(12), children: [
              for (final p in l)
                Card(child: ListTile(
                  leading: fotoRed(imagenPrincipal(p), h: 50, w: 50),
                  title: Text(p['nombre']),
                  subtitle: Text('${money(p['precio'])} · stock ${p['existencia']} · ${p['estatus']}${p['promocion'] != null ? '\n-${(p['promocion']['porcentajeDescuento'] as num).toStringAsFixed(0)}% ${temporada(p['promocion']['tipoTemporada'])}' : ''}'),
                  isThreeLine: p['promocion'] != null,
                  trailing: TextButton(
                    onPressed: () async {
                      try { await Api.patch('/aliados/productos/${p['id']}/estatus?estatus=${p['estatus'] == 'ACTIVO' ? 'PAUSADO' : 'ACTIVO'}'); setState(_c); }
                      catch (e) { if (mounted) aviso(context, '$e', error: true); }
                    },
                    child: Text(p['estatus'] == 'ACTIVO' ? 'Pausar' : 'Activar'),
                  ),
                )),
            ]);
          },
        ),
      );
}

class _Campanas extends StatefulWidget {
  const _Campanas();
  @override
  State<_Campanas> createState() => _CampanasState();
}

class _CampanasState extends State<_Campanas> {
  final titulo = TextEditingController(), pct = TextEditingController(text: '20'), fund = TextEditingController(),
      don = TextEditingController(text: '10'), ini = TextEditingController(), fin = TextEditingController();
  String temp = 'NAVIDAD';
  bool causa = false, guardando = false;
  List productos = [], campanas = [];
  final sel = <int>{};
  String? error;

  @override
  void initState() { super.initState(); _cargar(); }
  Future<void> _cargar() async {
    try {
      final r = await Future.wait([Api.get('/aliados/productos'), Api.get('/aliados/promociones')]);
      if (mounted) setState(() { productos = r[0] as List; campanas = r[1] as List; });
    } catch (e) { if (mounted) setState(() => error = '$e'); }
  }

  Future<void> _fecha(TextEditingController c) async {
    final d = await showDatePicker(context: context, initialDate: DateTime.now(), firstDate: DateTime.now().subtract(const Duration(days: 1)), lastDate: DateTime.now().add(const Duration(days: 730)));
    if (d != null) c.text = d.toIso8601String().substring(0, 10);
  }

  Future<void> _crear() async {
    setState(() { guardando = true; error = null; });
    try {
      await Api.post('/aliados/promociones', {
        'titulo': titulo.text.trim(), 'tipoTemporada': temp, 'porcentajeDescuento': double.tryParse(pct.text) ?? 0,
        'fechaInicio': ini.text, 'fechaFin': fin.text, 'idProductos': sel.toList(),
        'esCausaSocial': causa, 'fundacionBeneficiaria': causa ? fund.text.trim() : null, 'porcentajeDonacion': causa ? (double.tryParse(don.text) ?? 0) : 0,
      });
      if (mounted) aviso(context, 'Campaña creada');
      titulo.clear(); sel.clear();
      await _cargar();
    } catch (e) { setState(() => error = '$e'); }
    if (mounted) setState(() => guardando = false);
  }

  @override
  Widget build(BuildContext context) {
    return ListView(padding: const EdgeInsets.all(14), children: [
      const Text('Nueva campaña de temporada', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
      const SizedBox(height: 10),
      TextField(controller: titulo, decoration: campo('Título (ej. Navidad con causa)')),
      const SizedBox(height: 10),
      DropdownButtonFormField<String>(value: temp, decoration: campo('Temporada'), items: const [
        DropdownMenuItem(value: 'NAVIDAD', child: Text('🎄 Navidad')), DropdownMenuItem(value: 'DIA_DE_MUERTOS', child: Text('💀 Día de Muertos')),
        DropdownMenuItem(value: 'FIESTAS_PATRIAS', child: Text('🇲🇽 Fiestas Patrias')), DropdownMenuItem(value: 'BUEN_FIN', child: Text('🛍️ Buen Fin')),
        DropdownMenuItem(value: 'DIA_DE_LAS_MADRES', child: Text('💐 Día de las Madres')), DropdownMenuItem(value: 'SAN_VALENTIN', child: Text('❤️ San Valentín')),
        DropdownMenuItem(value: 'GENERAL', child: Text('🎁 General')),
      ], onChanged: (v) => setState(() => temp = v!)),
      const SizedBox(height: 10),
      Wrap(spacing: 6, children: [for (final n in ['20', '30', '40', '50']) ChoiceChip(label: Text('$n%'), selected: pct.text == n, onSelected: (_) => setState(() => pct.text = n))]),
      const SizedBox(height: 6),
      TextField(controller: pct, keyboardType: TextInputType.number, onChanged: (_) => setState(() {}), decoration: campo('% de descuento (5 a 90)')),
      const SizedBox(height: 10),
      Row(children: [
        Expanded(child: TextField(controller: ini, readOnly: true, onTap: () => _fecha(ini), decoration: campo('Inicio'))),
        const SizedBox(width: 10),
        Expanded(child: TextField(controller: fin, readOnly: true, onTap: () => _fecha(fin), decoration: campo('Fin'))),
      ]),
      const SizedBox(height: 8),
      const Text('Productos incluidos', style: TextStyle(fontWeight: FontWeight.bold)),
      for (final p in productos)
        CheckboxListTile(dense: true, contentPadding: EdgeInsets.zero, value: sel.contains(p['id']), title: Text(p['nombre']),
            onChanged: (v) => setState(() => v == true ? sel.add(p['id']) : sel.remove(p['id']))),
      SwitchListTile(contentPadding: EdgeInsets.zero, value: causa, onChanged: (v) => setState(() => causa = v), title: const Text('Dirigida a una asociación civil / fundación')),
      if (causa) ...[
        TextField(controller: fund, decoration: campo('Nombre de la asociación o fundación')),
        const SizedBox(height: 10),
        TextField(controller: don, keyboardType: TextInputType.number, decoration: campo('% del importe que se dona', ayuda: 'Sale de tu parte de la venta')),
      ],
      if (error != null) Padding(padding: const EdgeInsets.only(top: 8), child: Text(error!, style: const TextStyle(color: rojo))),
      const SizedBox(height: 10),
      FilledButton(onPressed: guardando ? null : _crear, child: const Text('Lanzar campaña')),
      const Divider(height: 30),
      const Text('Mis campañas', style: TextStyle(fontWeight: FontWeight.bold)),
      for (final c in campanas)
        Card(child: ListTile(
          title: Text('${temporada(c['tipoTemporada'])} · ${c['titulo']}'),
          subtitle: Text('-${(c['porcentajeDescuento'] as num).toStringAsFixed(0)}% · ${fecha(c['fechaInicio'])} → ${fecha(c['fechaFin'])}${c['esCausaSocial'] == true ? '\n💚 ${c['fundacionBeneficiaria']} (${(c['porcentajeDonacion'] as num).toStringAsFixed(0)}%)' : ''}'),
          isThreeLine: c['esCausaSocial'] == true,
          trailing: TextButton(
            onPressed: () async {
              try { await Api.patch('/aliados/promociones/${c['id']}/estatus?estatus=${c['estatus'] == 'ACTIVA' ? 'PAUSADA' : 'ACTIVA'}'); await _cargar(); }
              catch (e) { if (mounted) aviso(context, '$e', error: true); }
            },
            child: Text(c['estatus'] == 'ACTIVA' ? 'Pausar' : 'Activar'),
          ),
        )),
      const SizedBox(height: 90),
    ]);
  }
}
