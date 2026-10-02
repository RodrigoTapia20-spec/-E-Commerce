import 'package:flutter/foundation.dart';

class CartItem {
  final Map<String, dynamic> p;      // producto tal como lo devuelve la API
  String modalidad;                  // NORMAL | PROMOCION
  Map<String, dynamic>? paq;         // paquetería elegida (solo productos físicos)
  int cantidad;
  CartItem(this.p, this.modalidad, this.paq, this.cantidad);

  bool get fisico => p['tipo'] == 'PRODUCTO';
  num get precioUnit => (modalidad == 'PROMOCION' && p['promocion'] != null)
      ? p['promocion']['precioPromocional'] as num
      : p['precio'] as num;
  num get importe => precioUnit * cantidad;
  num get envio {
    if (!fisico || paq == null) return 0;
    final peso = (p['pesoKg'] as num?) ?? 0.5;
    return double.parse(((paq!['costoBase'] as num) + (paq!['costoPorKg'] as num) * peso * cantidad).toStringAsFixed(2));
  }
}

/// Carrito global. Cada producto guarda su modalidad de precio y su paquetería.
class Cart extends ChangeNotifier {
  static final Cart i = Cart._();
  Cart._();
  final List<CartItem> items = [];

  void agregar(Map<String, dynamic> p, String modalidad, Map<String, dynamic>? paq, int cant) {
    items.removeWhere((x) => x.p['id'] == p['id']);
    items.add(CartItem(p, modalidad, paq, cant));
    notifyListeners();
  }

  void quitar(CartItem x) { items.remove(x); notifyListeners(); }
  void cambiarCantidad(CartItem x, int c) {
    final max = x.fisico ? (x.p['existencia'] as int) : 99;
    x.cantidad = c.clamp(1, max < 1 ? 1 : max);
    notifyListeners();
  }
  void vaciar() { items.clear(); notifyListeners(); }

  int get totalItems => items.fold(0, (a, x) => a + x.cantidad);
  num get subtotal => items.fold<num>(0, (a, x) => a + x.importe);
  num get envio => items.fold<num>(0, (a, x) => a + x.envio);
  num get total => subtotal + envio;
  bool get hayFisicos => items.any((x) => x.fisico);
}
