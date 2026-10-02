package com.jjm.ecommerce.repository;

import com.jjm.ecommerce.model.CuentaCobroAliado;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CuentaCobroAliadoRepository extends JpaRepository<CuentaCobroAliado, Integer> {
    Optional<CuentaCobroAliado> findByAliado_Id(Integer idAliado);
}
