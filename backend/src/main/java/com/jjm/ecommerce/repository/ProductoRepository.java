package com.jjm.ecommerce.repository;

import com.jjm.ecommerce.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {
    List<Producto> findByAliado_Id(Integer idAliado);
    List<Producto> findByCategoria_IdAndEstatus(Integer idCategoria, String estatus);
    List<Producto> findByEstatus(String estatus);
    List<Producto> findByEstatusNot(String estatus);

    @Query("SELECT p FROM Producto p WHERE p.estatus = 'ACTIVO' AND " +
           "(LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%')) OR " +
           " LOWER(p.descripcion) LIKE LOWER(CONCAT('%', :texto, '%')))")
    List<Producto> buscarPorTexto(@Param("texto") String texto);
}
