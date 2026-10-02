package com.jjm.ecommerce.repository;

import com.jjm.ecommerce.model.Resena;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ResenaRepository extends JpaRepository<Resena, Integer> {
    List<Resena> findByProducto_IdOrderByFechaDesc(Integer idProducto);
    boolean existsByProducto_IdAndUsuario_Id(Integer idProducto, Integer idUsuario);
    List<Resena> findByUsuario_Id(Integer idUsuario);
}
