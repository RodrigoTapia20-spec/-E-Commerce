package com.jjm.ecommerce.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Sirve las imágenes y videos subidos por los vendedores en /api/uploads/** (con soporte de rangos para video). */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.uploads.directorio}")
    private String directorio;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path dir = Paths.get(directorio).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear la carpeta de archivos: " + dir, e);
        }
        registry.addResourceHandler("/uploads/**").addResourceLocations(dir.toUri().toString());
    }
}
