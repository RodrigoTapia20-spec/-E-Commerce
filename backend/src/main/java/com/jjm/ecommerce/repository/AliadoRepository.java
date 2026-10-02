package com.jjm.ecommerce.repository;

import com.jjm.ecommerce.model.Aliado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AliadoRepository extends JpaRepository<Aliado, Integer> {
    Optional<Aliado> findByUsuario_Id(Integer idUsuario);
    List<Aliado> findByEstatusVerificacion(String estatusVerificacion);
}
