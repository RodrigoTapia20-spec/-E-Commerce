package com.jjm.ecommerce.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/** Guarda las imágenes y videos que el vendedor sube desde la web o la app. */
@Service
public class ArchivoService {

    private static final Map<String, String> EXTENSIONES = Map.of(
            "image/jpeg", ".jpg", "image/png", ".png", "image/webp", ".webp",
            "video/mp4", ".mp4", "video/webm", ".webm", "video/quicktime", ".mov");

    private static final long MAX_IMAGEN = 8L * 1024 * 1024;   // 8 MB
    private static final long MAX_VIDEO = 30L * 1024 * 1024;   // 30 MB (≈15 s en buena calidad)

    @Value("${app.uploads.directorio}")
    private String directorio;

    public Map<String, Object> guardar(MultipartFile archivo) throws IOException {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío.");
        }
        String contentType = archivo.getContentType() == null ? "" : archivo.getContentType().toLowerCase();
        String extension = EXTENSIONES.get(contentType);
        if (extension == null) {
            throw new IllegalArgumentException("Formato no permitido. Usa JPG, PNG, WEBP (imágenes) o MP4, WEBM, MOV (videos).");
        }
        boolean esVideo = contentType.startsWith("video/");
        if (archivo.getSize() > (esVideo ? MAX_VIDEO : MAX_IMAGEN)) {
            throw new IllegalArgumentException(esVideo
                    ? "El video pesa demasiado (máximo 30 MB, ~15 segundos)."
                    : "La imagen pesa demasiado (máximo 8 MB).");
        }
        Path dir = Paths.get(directorio).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        String nombre = UUID.randomUUID() + extension;
        try (InputStream in = archivo.getInputStream()) {
            Files.copy(in, dir.resolve(nombre), StandardCopyOption.REPLACE_EXISTING);
        }
        return Map.of("url", "/uploads/" + nombre, "tipo", esVideo ? "VIDEO" : "IMAGEN");
    }
}
