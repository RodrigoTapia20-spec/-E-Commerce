/**
 * Carrito en el navegador (localStorage). Cada línea recuerda la modalidad elegida
 * (NORMAL o PROMOCION) y el esquema de mensajería, para calcular el total con envío.
 */
const Carrito = {
  CLAVE: "jjm_carrito_v2",
  obtener() { try { return JSON.parse(localStorage.getItem(this.CLAVE) || "[]"); } catch (e) { return []; } },
  guardar(items) { localStorage.setItem(this.CLAVE, JSON.stringify(items)); this.actualizarBadge(); },

  /** opciones: { modalidad, paqueteria (objeto de la API o null), cantidad } */
  agregar(p, { modalidad, paqueteria, cantidad }) {
    const items = this.obtener().filter((i) => i.idProducto !== p.id);   // una línea por producto
    items.push({
      idProducto: p.id, nombre: p.nombre, imagen: JJM.imagenPrincipal(p), tipo: p.tipo,
      precio: p.precio, pesoKg: p.pesoKg, existencia: p.existencia,
      promo: p.promocion ? { id: p.promocion.id, titulo: p.promocion.titulo, pct: p.promocion.porcentajeDescuento,
        precio: p.promocion.precioPromocional, esCausa: p.promocion.esCausaSocial,
        fundacion: p.promocion.fundacionBeneficiaria, temporada: p.promocion.tipoTemporada } : null,
      modalidad, paqueteria: paqueteria || null, cantidad,
      vendedor: p.aliado ? p.aliado.nombreComercial : "",
    });
    this.guardar(items);
  },
  quitar(id) { this.guardar(this.obtener().filter((i) => i.idProducto !== id)); },
  cambiarCantidad(id, c) {
    const max = (i) => (i.tipo === "PRODUCTO" ? i.existencia : 99);
    this.guardar(this.obtener().map((i) => (i.idProducto === id ? { ...i, cantidad: Math.max(1, Math.min(c, max(i))) } : i)));
  },
  vaciar() { this.guardar([]); },

  precioUnit: (i) => (i.modalidad === "PROMOCION" && i.promo ? i.promo.precio : i.precio),
  importe(i) { return this.precioUnit(i) * i.cantidad; },
  envio(i) {
    if (i.tipo !== "PRODUCTO" || !i.paqueteria) return 0;
    return Number((i.paqueteria.costoBase + i.paqueteria.costoPorKg * (i.pesoKg || 0.5) * i.cantidad).toFixed(2));
  },
  subtotal() { return this.obtener().reduce((a, i) => a + this.importe(i), 0); },
  envioTotal() { return this.obtener().reduce((a, i) => a + this.envio(i), 0); },
  total() { return this.subtotal() + this.envioTotal(); },
  hayFisicos() { return this.obtener().some((i) => i.tipo === "PRODUCTO"); },
  totalItems() { return this.obtener().reduce((a, i) => a + i.cantidad, 0); },
  actualizarBadge() { const b = document.querySelector(".carrito-badge"); if (b) b.textContent = this.totalItems(); },
};
