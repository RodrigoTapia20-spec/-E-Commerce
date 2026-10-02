package com.jjm.ecommerce.repository;

import com.jjm.ecommerce.model.Dispersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DispersionRepository extends JpaRepository<Dispersion, Integer> {
    List<Dispersion> findByAliado_IdAndEstatus(Integer idAliado, String estatus);
    List<Dispersion> findByAliado_IdOrderByFechaDispersionDesc(Integer idAliado);
}
