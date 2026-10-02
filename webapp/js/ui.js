/** Utilidades compartidas + encabezado/pie (con logo) que se inyectan en todas las páginas. */
const JJM = {
  esc(t) {   // evita inyección de HTML en textos escritos por usuarios
    return String(t ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
  },
  money: (n) => "$" + Number(n || 0).toLocaleString("es-MX", { minimumFractionDigits: 2, maximumFractionDigits: 2 }),
  fecha: (f) => (f ? new Date(f).toLocaleDateString("es-MX", { day: "2-digit", month: "short", year: "numeric" }) : ""),
  estrellas(prom, total) {
    if (prom == null) return `<span class="estrellas"><small>Sin calificaciones</small></span>`;
    const llenas = Math.round(prom);
    return `<span class="estrellas">${"★".repeat(llenas)}${"☆".repeat(5 - llenas)} <small>${Number(prom).toFixed(1)} (${total})</small></span>`;
  },
  temporada: (t) => ({ NAVIDAD: "🎄 Navidad", DIA_DE_MUERTOS: "💀 Día de Muertos", FIESTAS_PATRIAS: "🇲🇽 Fiestas Patrias", BUEN_FIN: "🛍️ Buen Fin",
    DIA_DE_LAS_MADRES: "💐 Día de las Madres", SAN_VALENTIN: "❤️ San Valentín", OTRA: "🎁 Campaña", GENERAL: "🎁 Promoción" }[t] || "🎁 Promoción"),
  imagenPrincipal(p) {
    const m = (p.media || []).find((x) => x.tipo === "IMAGEN");
    return m ? m.url : "data:image/svg+xml;utf8," + encodeURIComponent(
      "<svg xmlns='http://www.w3.org/2000/svg' width='300' height='200'><rect width='100%' height='100%' fill='#eee'/><text x='50%' y='50%' text-anchor='middle' fill='#999' font-family='Arial' font-size='16'>Sin imagen</text></svg>");
  },
  toast(msg) {
    let t = document.getElementById("toast");
    if (!t) { t = document.createElement("div"); t.id = "toast"; document.body.appendChild(t); }
    t.textContent = msg; t.style.display = "block";
    clearTimeout(t._h); t._h = setTimeout(() => (t.style.display = "none"), 2600);
  },
  msg(el, tipo, texto) {   // tipo: error | ok | info
    el.innerHTML = texto ? `<div class="msg-${tipo}">${JJM.esc(texto)}</div>` : "";
  },
  /** Exige sesión (y opcionalmente uno de los roles). Devuelve false si redirige. */
  requiereRol(...roles) {
    if (!ApiClient.estaAutenticado()) { window.location.href = "login.html"; return false; }
    if (roles.length && !roles.includes(ApiClient.rol())) {
      document.querySelector("main").innerHTML =
        `<div class="contenedor"><div class="msg-error">No tienes permiso para ver esta sección (requiere: ${roles.join(" / ")}).</div></div>`;
      return false;
    }
    return true;
  },
  // Ícono de ojo cerrado (mismo trazo que el ojo abierto, con el párpado cerrado) — sustituye al mono 🙈.
  OJO_CERRADO: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:middle"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7Z" opacity=".35"/><path d="M2 12s3.5 7 10 7 10-7 10-7"/><line x1="2" y1="19" x2="22" y2="5"/></svg>',
  alternarPassword(idInput, btn) {
    const i = document.getElementById(idInput);
    const oculto = i.type === "password";
    i.type = oculto ? "text" : "password";
    btn.innerHTML = oculto ? JJM.OJO_CERRADO : "👁";
    btn.setAttribute("aria-label", oculto ? "Ocultar contraseña" : "Mostrar contraseña");
  },
};

/** Encabezado + pie. Uso: <div id="jjm-header"></div> ... <div id="jjm-footer"></div> */
document.addEventListener("DOMContentLoaded", () => {
  // favicon con el logo (si existe img/logo.png)
  const ico = document.createElement("link");
  ico.rel = "icon"; ico.type = "image/png"; ico.href = "img/logo.png";
  document.head.appendChild(ico);

  const rol = ApiClient.rol();
  const nombre = ApiClient.nombre();
  const logueado = ApiClient.estaAutenticado();
  const enlaces = [];
  if (logueado && (rol === "CLIENTE" || rol === "ALIADO" || rol === "ADMIN")) enlaces.push(`<a href="pedidos.html" class="item"><small>Mis</small><strong>Pedidos</strong></a>`);
  if (rol === "ALIADO") enlaces.push(`<a href="aliado-dashboard.html" class="item"><small>Panel de</small><strong>Vendedor</strong></a>`);
  if (rol === "DISTRIBUIDOR" || rol === "ADMIN") enlaces.push(`<a href="distribuidor-dashboard.html" class="item"><small>Panel de</small><strong>Distribuidor</strong></a>`);
  if (rol === "ADMIN") enlaces.push(`<a href="admin-dashboard.html" class="item"><small>Panel de</small><strong>Administrador</strong></a>`);

  const sesion = logueado
    ? `<span class="item"><small>Hola,</small><strong>${JJM.esc(nombre)}</strong></span>
       <button class="btn btn-primario btn-chico" id="btn-sesion" type="button">Cerrar Sesión</button>`
    : `<button class="btn btn-secundario btn-chico" id="btn-registrar" type="button">Registrar</button>
       <button class="btn btn-primario btn-chico" id="btn-sesion" type="button">Iniciar Sesión</button>`;

  const h = document.getElementById("jjm-header");
  if (h) {
    h.innerHTML = `
    <header>
      <div class="header-top">
        <a href="index.html" class="logo" aria-label="Inicio">
          <img src="img/logo.png" alt="JJM" class="logo-img" id="logo-img"
               onerror="this.style.display='none';document.getElementById('logo-texto').style.display='inline'"/>
          <span id="logo-texto" style="display:none">JJM</span>
          <span class="tag">con causa</span>
        </a>
        <form class="buscador" id="form-buscar">
          <input type="text" id="input-buscar" placeholder="Buscar productos y servicios con causa..." />
          <button type="submit">🔎</button>
        </form>
        <nav class="header-links">
          ${enlaces.join("")}
          ${sesion}
          ${rol === "DISTRIBUIDOR" ? "" : `<a href="carrito.html" class="item carrito-icono" aria-label="Carrito">🛒<span class="carrito-badge">0</span></a>`}
        </nav>
      </div>
      <nav class="header-nav">
        <a href="index.html">Inicio</a><a href="index.html#promos">Promociones</a>
        <a href="quejas.html">Quejas y Sugerencias</a>
      </nav>
    </header>`;
    document.getElementById("form-buscar").onsubmit = (e) => {
      e.preventDefault();
      window.location.href = "index.html?buscar=" + encodeURIComponent(document.getElementById("input-buscar").value);
    };
    document.getElementById("btn-sesion").onclick = () => {
      if (logueado) { if (confirm("¿Cerrar sesión?")) { ApiClient.cerrarSesion(); window.location.href = "index.html"; } }
      else window.location.href = "login.html";
    };
    const reg = document.getElementById("btn-registrar");
    if (reg) reg.onclick = () => (window.location.href = "registro.html");
  }
  const f = document.getElementById("jjm-footer");
  if (f) f.outerHTML = `<footer class="pie">© 2026 JJM Tecnologías Innovadoras, S.A. de C.V. — E-Commerce con Causa Social</footer>`;
  if (typeof Carrito !== "undefined") Carrito.actualizarBadge();
});

/**
 * La sesión se guarda en localStorage, que el navegador comparte entre TODAS las pestañas
 * del mismo sitio (no es exclusiva de cada ventana). Si inicias sesión o cierras sesión en
 * una pestaña, las demás pestañas abiertas se enteran de inmediato con este evento — y para
 * que no se queden mostrando el panel de un usuario que ya no es el de la sesión activa
 * (lo que antes causaba error 403 al hacer clic en algo), las recargamos automáticamente.
 */
window.addEventListener("storage", (e) => {
  if (e.key === "jjm_token" || e.key === "jjm_rol") window.location.reload();
});
