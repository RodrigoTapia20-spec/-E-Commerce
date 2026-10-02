package com.jjm.ecommerce.repository;

import com.jjm.ecommerce.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByCorreo(String correo);
    boolean existsByCorreo(String correo);
    Optional<Usuario> findByResetPasswordToken(String resetPasswordToken);
    boolean existsByRol_Nombre(String nombreRol);
    List<Usuario> findAllByOrderByFechaRegistroDesc();
}
