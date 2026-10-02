package com.jjm.ecommerce.repository;

import com.jjm.ecommerce.model.QuejaSugerencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuejaSugerenciaRepository extends JpaRepository<QuejaSugerencia, Integer> {
    List<QuejaSugerencia> findByEstatus(String estatus);
    List<QuejaSugerencia> findByUsuario_Id(Integer idUsuario);
}
