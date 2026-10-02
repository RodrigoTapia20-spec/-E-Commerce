import 'package:flutter/material.dart';
import '../api.dart';
import '../util.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});
  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final correo = TextEditingController(), pass = TextEditingController(), codigo = TextEditingController();
  bool paso2fa = false, cargando = false;
  String? error;

  Future<void> _entrar() async {
    setState(() { cargando = true; error = null; });
    try {
      final r = paso2fa
          ? await Api.post('/auth/verificar-codigo', {'correo': correo.text.trim(), 'codigo': codigo.text.trim()})
          : await Api.post('/auth/login', {'correo': correo.text.trim(), 'password': pass.text});
      if (r['requiere2fa'] == true) {
        setState(() => paso2fa = true);
      } else {
        await Api.guardarSesion(r as Map);
        if (mounted) Navigator.of(context).popUntil((r) => r.isFirst);
      }
    } catch (e) {
      setState(() => error = '$e');
    }
    if (mounted) setState(() => cargando = false);
  }

  Future<void> _servidor() async {
    final c = TextEditingController(text: Api.base);
    final ok = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Dirección del servidor'),
        content: Column(mainAxisSize: MainAxisSize.min, children: [
          TextField(controller: c, decoration: campo('URL de la API', ayuda: 'Emulador Android: http://10.0.2.2:8080/api\nCelular físico: http://IP-de-tu-PC:8080/api')),
        ]),
        actions: [TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancelar')), FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Guardar'))],
      ),
    );
    if (ok == true) { await Api.setBase(c.text); if (mounted) aviso(context, 'Servidor: ${Api.base}'); }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Iniciar sesión'), actions: [IconButton(icon: const Icon(Icons.settings), onPressed: _servidor)]),
      body: ListView(padding: const EdgeInsets.all(20), children: [
        Card(child: Padding(padding: const EdgeInsets.all(18), child: Column(children: [
          if (!paso2fa) ...[
            TextField(controller: correo, keyboardType: TextInputType.emailAddress, decoration: campo('Correo electrónico')),
            const SizedBox(height: 12),
            CampoPassword(pass),
            Align(alignment: Alignment.centerRight, child: TextButton(onPressed: () => ir(context, 'login', const RecuperarScreen()), child: const Text('¿Olvidaste tu contraseña?'))),
          ] else ...[
            const Text('Te enviamos un código de verificación a tu correo.'),
            const SizedBox(height: 10),
            TextField(controller: codigo, keyboardType: TextInputType.number, decoration: campo('Código de 6 dígitos')),
          ],
          if (error != null) Padding(padding: const EdgeInsets.only(top: 10), child: Text(error!, style: const TextStyle(color: rojo))),
          const SizedBox(height: 12),
          SizedBox(width: double.infinity, child: FilledButton(onPressed: cargando ? null : _entrar, child: Text(cargando ? 'Entrando…' : (paso2fa ? 'Verificar' : 'Iniciar sesión')))),
          TextButton(onPressed: () => ir(context, 'login', const RegistroScreen()), child: const Text('¿No tienes cuenta? Regístrate')),
        ]))),
      ]),
    );
  }
}

class RegistroScreen extends StatefulWidget {
  const RegistroScreen({super.key});
  @override
  State<RegistroScreen> createState() => _RegistroScreenState();
}

class _RegistroScreenState extends State<RegistroScreen> {
  final nombre = TextEditingController(), apellidos = TextEditingController(), correo = TextEditingController(),
      tel = TextEditingController(), pass = TextEditingController(), negocio = TextEditingController(),
      rfc = TextEditingController(), convenio = TextEditingController(text: '10'), causa = TextEditingController();
  String rol = 'CLIENTE';
  bool cargando = false;
  String? error;

  Future<void> _registrar() async {
    setState(() { cargando = true; error = null; });
    try {
      await Api.post('/auth/registro', {
        'nombre': nombre.text.trim(), 'apellidos': apellidos.text.trim(), 'correo': correo.text.trim(),
        'telefono': tel.text.trim(), 'password': pass.text, 'rol': rol,
        if (rol == 'ALIADO') ...{
          'nombreComercial': negocio.text.trim(), 'rfc': rfc.text.trim().toUpperCase(),
          'porcentajeConvenio': double.tryParse(convenio.text) ?? 10,
          'causaSocial': causa.text.trim().isEmpty ? null : causa.text.trim(),
        },
      });
      if (mounted) {
        aviso(context, '¡Cuenta creada! Ahora inicia sesión.');
        Navigator.pop(context);
      }
    } catch (e) {
      setState(() => error = '$e');
    }
    if (mounted) setState(() => cargando = false);
  }

  @override
  Widget build(BuildContext context) {
    Widget g(Widget w) => Padding(padding: const EdgeInsets.only(bottom: 12), child: w);
    return Scaffold(
      appBar: AppBar(title: const Text('Crear cuenta')),
      body: ListView(padding: const EdgeInsets.all(20), children: [
        g(DropdownButtonFormField<String>(
          value: rol, decoration: campo('Quiero registrarme como'),
          items: const [DropdownMenuItem(value: 'CLIENTE', child: Text('Cliente')), DropdownMenuItem(value: 'ALIADO', child: Text('Vendedor'))],
          onChanged: (v) => setState(() => rol = v!),
        )),
        g(TextField(controller: nombre, decoration: campo('Nombre'))),
        g(TextField(controller: apellidos, decoration: campo('Apellidos'))),
        g(TextField(controller: correo, keyboardType: TextInputType.emailAddress, decoration: campo('Correo electrónico'))),
        g(TextField(controller: tel, keyboardType: TextInputType.phone, decoration: campo('Teléfono (opcional)'))),
        g(CampoPassword(pass, etiqueta: 'Contraseña (mínimo 8 caracteres)')),
        if (rol == 'ALIADO') ...[
          g(TextField(controller: negocio, decoration: campo('Nombre comercial'))),
          g(TextField(controller: rfc, textCapitalization: TextCapitalization.characters, decoration: campo('RFC'))),
          g(TextField(controller: convenio, keyboardType: TextInputType.number, decoration: campo('% de convenio para JJM'))),
          g(TextField(controller: causa, decoration: campo('Causa social que apoyas (opcional)'))),
        ],
        if (error != null) Padding(padding: const EdgeInsets.only(bottom: 10), child: Text(error!, style: const TextStyle(color: rojo))),
        FilledButton(onPressed: cargando ? null : _registrar, child: Text(cargando ? 'Creando…' : 'Registrarme')),
      ]),
    );
  }
}

/// Recuperación de contraseña por correo: 1) pedir enlace  2) pegar el enlace/código y escribir la nueva contraseña.
class RecuperarScreen extends StatefulWidget {
  const RecuperarScreen({super.key});
  @override
  State<RecuperarScreen> createState() => _RecuperarScreenState();
}

class _RecuperarScreenState extends State<RecuperarScreen> {
  final correo = TextEditingController(), token = TextEditingController(), nueva = TextEditingController();
  bool enviado = false, cargando = false;
  String? error, info;

  Future<void> _pedir() async {
    setState(() { cargando = true; error = null; });
    try {
      final r = await Api.post('/auth/solicitar-recuperacion', {'correo': correo.text.trim()});
      setState(() { enviado = true; info = r['mensaje']?.toString(); });
    } catch (e) { setState(() => error = '$e'); }
    if (mounted) setState(() => cargando = false);
  }

  Future<void> _restablecer() async {
    setState(() { cargando = true; error = null; });
    // acepta el enlace completo del correo o solo el código
    final texto = token.text.trim();
    final t = Uri.tryParse(texto)?.queryParameters['token'] ?? texto;
    try {
      await Api.post('/auth/restablecer-password', {'token': t, 'nuevaPassword': nueva.text});
      if (mounted) { aviso(context, 'Contraseña actualizada. Ya puedes iniciar sesión.'); Navigator.pop(context); }
    } catch (e) { setState(() => error = '$e'); }
    if (mounted) setState(() => cargando = false);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Recuperar contraseña')),
      body: ListView(padding: const EdgeInsets.all(20), children: [
        const Text('Escribe tu correo y te enviaremos un enlace para crear una nueva contraseña (vale 30 minutos).'),
        const SizedBox(height: 12),
        TextField(controller: correo, keyboardType: TextInputType.emailAddress, decoration: campo('Correo electrónico')),
        const SizedBox(height: 10),
        FilledButton(onPressed: cargando ? null : _pedir, child: const Text('Enviar enlace')),
        if (info != null) Padding(padding: const EdgeInsets.only(top: 10), child: Text(info!, style: const TextStyle(color: verde))),
        if (enviado) ...[
          const Divider(height: 34),
          const Text('Cuando recibas el correo, pega aquí el enlace (o el código) y elige tu nueva contraseña:'),
          const SizedBox(height: 10),
          TextField(controller: token, decoration: campo('Enlace o código del correo')),
          const SizedBox(height: 10),
          CampoPassword(nueva, etiqueta: 'Nueva contraseña (mínimo 8)'),
          const SizedBox(height: 10),
          FilledButton(onPressed: cargando ? null : _restablecer, child: const Text('Cambiar contraseña')),
        ],
        if (error != null) Padding(padding: const EdgeInsets.only(top: 10), child: Text(error!, style: const TextStyle(color: rojo))),
      ]),
    );
  }
}
