package com.jjm.ecommerce.config;

import com.jjm.ecommerce.model.Rol;
import com.jjm.ecommerce.model.Usuario;
import com.jjm.ecommerce.repository.RolRepository;
import com.jjm.ecommerce.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea la cuenta del Administrador (dueño) la primera vez que arranca el sistema, si todavía no
 * existe ninguna. CAMBIA la contraseña por defecto (variable ADMIN_PASSWORD o app.admin.password).
 */
@Component
public class AdminInicial implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.correo}")
    private String correo;
    @Value("${app.admin.password}")
    private String password;

    public AdminInicial(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                        PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.existsByRol_Nombre("ADMIN")) return;
        Rol rol = rolRepository.findByNombre("ADMIN")
                .orElseGet(() -> rolRepository.save(Rol.builder().nombre("ADMIN").build()));
        usuarioRepository.save(Usuario.builder()
                .nombre("Administrador").apellidos("JJM")
                .correo(correo).passwordHash(passwordEncoder.encode(password))
                .rol(rol).verificadoIa(true).build());
        System.out.println("=====================================================================");
        System.out.println(" Cuenta de ADMINISTRADOR creada:  " + correo);
        System.out.println(" Contraseña inicial: la definida en app.admin.password (cámbiala).");
        System.out.println("=====================================================================");
    }
}
