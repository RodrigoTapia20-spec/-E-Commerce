/**
 * Asistente virtual SIEMPRE presente: burbuja flotante en todas las páginas, con mensaje
 * proactivo según la pantalla y respuestas rápidas distintas para Cliente y Vendedor.
 * Cada página define <body data-pagina="..."> para dar contexto.
 */
(function () {
  const rol = ApiClient.rol();
  const contexto = rol === "ALIADO" ? "VENDEDOR" : rol === "DISTRIBUIDOR" ? "DISTRIBUIDOR" : rol === "ADMIN" ? "ADMIN" : "CLIENTE";
  const pagina = document.body.dataset.pagina || "inicio";
  let idConv = Number(sessionStorage.getItem("jjm_chat_id")) || null;

  const TIPS = {
    inicio: "👋 ¿Buscas algo? Te ayudo a encontrar productos con causa y promociones de temporada.",
    producto: "💡 Elige entre costo normal o promoción, y escoge tu paquetería: verás el costo y los días de entrega.",
    carrito: "🧾 Revisa tu envío y método de pago. Si necesitas factura, la pides después en \"Mis pedidos\".",
    pedidos: "📦 Aquí ves tus guías y tiempos de entrega. También puedes facturar y calificar a tu vendedor.",
    factura: "🧾 Necesito tu RFC, razón social, régimen fiscal y C.P. fiscal. Te guío si tienes dudas.",
    "vendedor-publicar": "📸 Te acompaño a publicar: cada producto lleva de 5 a 7 archivos (imágenes y videos de máx. 15 s).",
    "vendedor-campanas": "🎄 ¿Lanzamos una campaña de temporada? Te explico cómo poner 20%, 30% o 40% y apoyar a una fundación.",
    vendedor: "🤝 Soy tu asistente de ventas: publicar, campañas, paqueterías y ranking. ¿Empezamos?",
    distribuidor: "🚚 Aquí gestionas las guías: pasa cada envío de PENDIENTE a EN_TRANSITO y luego a ENTREGADO.",
    admin: "👑 Desde aquí aprobas vendedores, creas distribuidores y ves las ventas y donaciones.",
    login: "🔐 ¿Olvidaste tu contraseña? Toca \"¿Olvidaste tu contraseña?\" y te enviamos un enlace por correo.",
  };
  const SUGERENCIAS = {
    CLIENTE: ["¿Cómo funcionan las promociones?", "¿Cómo se calcula el envío?", "¿Cómo facturo?", "¿Cómo califico a un vendedor?"],
    VENDEDOR: ["¿Cómo publico un producto?", "¿Cómo lanzo una campaña?", "¿Cómo subo mi nivel de ranking?", "¿Qué paqueterías puedo usar?"],
    DISTRIBUIDOR: ["¿Cómo actualizo un envío?"], ADMIN: ["¿Qué tipos de cuenta existen?"],
  };
  const BIENVENIDA = contexto === "VENDEDOR"
    ? "¡Hola! Soy tu asistente 🤝. Te acompaño a publicar productos, lanzar campañas y mejorar tu ranking. ¿Con qué empezamos?"
    : "¡Hola! Soy tu asistente 🤝. Te acompaño en tu compra: promociones, envíos, pagos, factura y seguimiento de tu pedido. ¿En qué te ayudo?";

  function agregar(emisor, texto) {
    const c = document.getElementById("chat-msgs");
    const d = document.createElement("div");
    d.className = emisor === "BOT" ? "m-bot" : "m-user";
    d.textContent = texto;
    c.appendChild(d); c.scrollTop = c.scrollHeight;
  }
  async function enviar(texto) {
    agregar("USUARIO", texto);
    try {
      const r = await ApiClient.request("/chatbot/mensaje", { method: "POST",
        body: { idConversacion: idConv, mensaje: texto, canal: "WEB", contexto, pagina } });
      idConv = r.id; sessionStorage.setItem("jjm_chat_id", idConv);
      agregar("BOT", r.mensajes[r.mensajes.length - 1].mensaje);
    } catch (e) {
      agregar("BOT", "No pude responder ahora: " + e.message);
    }
  }
  function abrir() {
    const v = document.getElementById("chat-ventana");
    v.classList.add("abierto");
    document.getElementById("chat-tip").style.display = "none";
    document.getElementById("chat-burbuja").classList.remove("pulso");
    if (!v.dataset.init) {
      v.dataset.init = "1";
      agregar("BOT", BIENVENIDA);
      if (TIPS[pagina]) agregar("BOT", TIPS[pagina]);
      const s = document.getElementById("chat-sugs");
      (SUGERENCIAS[contexto] || []).forEach((t) => {
        const b = document.createElement("button"); b.textContent = t;
        b.onclick = () => enviar(t); s.appendChild(b);
      });
    }
    document.getElementById("chat-input").focus();
  }

  document.addEventListener("DOMContentLoaded", () => {
    document.body.insertAdjacentHTML("beforeend", `
      <div id="chat-tip"><button aria-label="Cerrar">✕</button><span></span></div>
      <div id="chat-burbuja" class="pulso" title="Asistente virtual" role="button" aria-label="Abrir asistente virtual">💬</div>
      <div id="chat-ventana" role="dialog" aria-label="Asistente virtual">
        <header><span>Asistente virtual JJM</span><span id="chat-cerrar" style="cursor:pointer">✕</span></header>
        <div id="chat-msgs"></div><div id="chat-sugs"></div>
        <form id="chat-form"><input id="chat-input" placeholder="Escribe tu pregunta..." autocomplete="off"/><button type="submit">➤</button></form>
      </div>`);
    document.getElementById("chat-burbuja").onclick = () => {
      const v = document.getElementById("chat-ventana");
      v.classList.contains("abierto") ? v.classList.remove("abierto") : abrir();
    };
    document.getElementById("chat-cerrar").onclick = () => document.getElementById("chat-ventana").classList.remove("abierto");
    document.getElementById("chat-form").onsubmit = (e) => {
      e.preventDefault();
      const i = document.getElementById("chat-input"); const t = i.value.trim();
      if (t) { i.value = ""; enviar(t); }
    };
    // Mensaje proactivo: acompaña al usuario en cada pantalla
    const tip = document.getElementById("chat-tip");
    if (TIPS[pagina] && !sessionStorage.getItem("jjm_tip_" + pagina)) {
      setTimeout(() => {
        if (document.getElementById("chat-ventana").classList.contains("abierto")) return;
        tip.querySelector("span").textContent = TIPS[pagina]; tip.style.display = "block";
        sessionStorage.setItem("jjm_tip_" + pagina, "1");
      }, 2500);
    }
    tip.querySelector("button").onclick = () => (tip.style.display = "none");
    tip.querySelector("span").onclick = abrir;
  });
})();
