package com.jjm.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * JJM Tecnologías Innovadoras, S.A. de C.V.
 * API REST central para la plataforma de E-Commerce con Causa Social.
 * Esta misma API es consumida tanto por la aplicación web (webapp/)
 * como por la aplicación móvil Android (android-app/), garantizando
 * que ambos frentes trabajen sobre la misma lógica de negocio y base
 * de datos (MySQL).
 */
@SpringBootApplication
@EnableScheduling
public class EcommerceBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(EcommerceBackendApplication.class, args);
    }
}
