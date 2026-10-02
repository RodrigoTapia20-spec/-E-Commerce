/**
 * Cliente de la API REST (Java / Spring Boot). La web y la app móvil usan la MISMA API.
 * Si el backend corre en otra dirección, define antes de cargar este archivo:
 *   <script>window.JJM_API_BASE_URL = "http://192.168.1.50:8080/api";</script>
 */
const API_BASE_URL = window.JJM_API_BASE_URL || "http://localhost:8080/api";

const ApiClient = {
  token: () => localStorage.getItem("jjm_token"),
  rol: () => localStorage.getItem("jjm_rol"),
  nombre: () => localStorage.getItem("jjm_nombre"),
  estaAutenticado() { return !!this.token(); },

  guardarSesion({ token, rol, idUsuario, nombre }) {
    if (token) localStorage.setItem("jjm_token", token);
    if (rol) localStorage.setItem("jjm_rol", rol);
    if (idUsuario) localStorage.setItem("jjm_id_usuario", idUsuario);
    if (nombre) localStorage.setItem("jjm_nombre", nombre);
  },
  cerrarSesion() {
    ["jjm_token", "jjm_rol", "jjm_id_usuario", "jjm_nombre"].forEach((k) => localStorage.removeItem(k));
  },

  /** Petición genérica. body → JSON; form → FormData (subida de archivos). */
  async request(path, { method = "GET", body, form } = {}) {
    const headers = {};
    if (this.token()) headers["Authorization"] = `Bearer ${this.token()}`;
    if (body !== undefined) headers["Content-Type"] = "application/json";

    let resp;
    try {
      resp = await fetch(`${API_BASE_URL}${path}`, {
        method, headers, body: form ? form : body !== undefined ? JSON.stringify(body) : undefined,
      });
    } catch (e) {
      throw new Error("No hay conexión con el servidor. Verifica que el backend esté corriendo.");
    }
    let data = null;
    try { data = await resp.json(); } catch (e) { /* sin cuerpo */ }

    if (!resp.ok) {
      if (resp.status === 401 && this.token() && !path.startsWith("/auth/")) {
        this.cerrarSesion();
        sessionStorage.setItem("jjm_aviso", "Tu sesión expiró. Inicia sesión de nuevo.");
        window.location.href = "login.html";
      }
      throw new Error((data && (data.mensaje || data.error)) || `Error ${resp.status}`);
    }
    return data;
  },
  get: (p) => ApiClient.request(p),
  post: (p, body) => ApiClient.request(p, { method: "POST", body }),
  patch: (p, body) => ApiClient.request(p, { method: "PATCH", body }),
  subir(archivo) {
    const f = new FormData();
    f.append("archivo", archivo);
    return this.request("/aliados/media", { method: "POST", form: f });
  },
};
