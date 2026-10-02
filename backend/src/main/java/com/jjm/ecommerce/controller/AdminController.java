package com.jjm.ecommerce.controller;

import com.jjm.ecommerce.dto.AdminCrearUsuarioRequest;
import com.jjm.ecommerce.dto.Vistas.AliadoAdminView;
import com.jjm.ecommerce.dto.Vistas.ProductoView;
import com.jjm.ecommerce.dto.Vistas.UsuarioView;
import com.jjm.ecommerce.security.CurrentUserProvider;
import com.jjm.ecommerce.service.AdminService;
import com.jjm.ecommerce.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Solo el Administrador (dueño). */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final ProductoService productoService;
    private final CurrentUserProvider currentUser;

    public AdminController(AdminService adminService, ProductoService productoService, CurrentUserProvider currentUser) {
        this.adminService = adminService;
        this.productoService = productoService;
        this.currentUser = currentUser;
    }

    /** "Eliminar Cuenta" — borra o anonimiza (ver AdminService) a cualquier usuario menos a sí mismo. */
    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Integer id) {
        adminService.eliminarCuenta(id, currentUser.obtenerId());
        return ResponseEntity.noContent().build();
    }

    /** Todos los productos/servicios de cualquier vendedor (para el botón "Eliminar" del admin). */
    @GetMapping("/productos")
    public List<ProductoView> productos() {
        return productoService.todosParaAdmin();
    }

    @DeleteMapping("/productos/{id}")
    public ResponseEntity<Void> eliminarProducto(@PathVariable Integer id) {
        productoService.eliminar(currentUser.obtener(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/usuarios")
    public List<UsuarioView> usuarios() {
        return adminService.listarUsuarios();
    }

    @PostMapping("/usuarios")
    public UsuarioView crearUsuarioInterno(@Valid @RequestBody AdminCrearUsuarioRequest req) {
        return adminService.crearUsuarioInterno(req);
    }

    @PatchMapping("/usuarios/{id}/estatus")
    public UsuarioView cambiarEstatus(@PathVariable Integer id, @RequestParam String estatus) {
        return adminService.cambiarEstatus(id, currentUser.obtenerId(), estatus);
    }

    @GetMapping("/vendedores/pendientes")
    public List<AliadoAdminView> pendientes() {
        return adminService.vendedoresPorVerificar();
    }

    @PatchMapping("/vendedores/{id}/verificacion")
    public ResponseEntity<Void> verificar(@PathVariable Integer id, @RequestParam boolean aprobar) {
        adminService.verificarVendedor(id, aprobar);
        return ResponseEntity.noContent().build();
    }
}
