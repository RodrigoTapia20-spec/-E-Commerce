package com.jjm.ecommerce.controller;

import com.jjm.ecommerce.dto.ProductoRequest;
import com.jjm.ecommerce.dto.Vistas.ProductoView;
import com.jjm.ecommerce.security.CurrentUserProvider;
import com.jjm.ecommerce.service.ArchivoService;
import com.jjm.ecommerce.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
public class ProductoController {

    private final ProductoService productoService;
    private final ArchivoService archivoService;
    private final CurrentUserProvider currentUser;

    public ProductoController(ProductoService productoService, ArchivoService archivoService,
                              CurrentUserProvider currentUser) {
        this.productoService = productoService;
        this.archivoService = archivoService;
        this.currentUser = currentUser;
    }

    // ---------- Público (catálogo)
    @GetMapping("/productos")
    public List<ProductoView> catalogo(@RequestParam(required = false) String buscar,
                                       @RequestParam(required = false) Integer categoria) {
        return productoService.catalogo(buscar, categoria);
    }

    @GetMapping("/productos/{id}")
    public ProductoView detalle(@PathVariable Integer id) {
        return productoService.detalle(id);
    }

    // ---------- Vendedor
    @PostMapping(value = "/aliados/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> subirArchivo(@RequestParam("archivo") MultipartFile archivo) throws IOException {
        return archivoService.guardar(archivo);
    }

    @PostMapping("/aliados/productos")
    public ProductoView publicar(@Valid @RequestBody ProductoRequest req) {
        return productoService.publicar(currentUser.obtenerId(), req);
    }

    @GetMapping("/aliados/productos")
    public List<ProductoView> misProductos() {
        return productoService.misProductos(currentUser.obtenerId());
    }

    @PatchMapping("/aliados/productos/{id}/estatus")
    public ProductoView cambiarEstatus(@PathVariable Integer id, @RequestParam String estatus) {
        return productoService.cambiarEstatus(currentUser.obtener(), id, estatus);
    }

    /** "Modificar Datos" — sólo el vendedor dueño puede editar su producto/servicio. */
    @PutMapping("/aliados/productos/{id}")
    public ProductoView actualizar(@PathVariable Integer id, @Valid @RequestBody ProductoRequest req) {
        return productoService.actualizar(currentUser.obtenerId(), id, req);
    }

    /** "Eliminar" — el vendedor sólo puede borrar sus propios productos. */
    @DeleteMapping("/aliados/productos/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        productoService.eliminar(currentUser.obtener(), id);
        return ResponseEntity.noContent().build();
    }
}
