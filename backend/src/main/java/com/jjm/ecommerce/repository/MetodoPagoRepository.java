package com.jjm.ecommerce.repository;

import com.jjm.ecommerce.model.MetodoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MetodoPagoRepository extends JpaRepository<MetodoPago, Integer> {
    Optional<MetodoPago> findByNombre(String nombre);
}
