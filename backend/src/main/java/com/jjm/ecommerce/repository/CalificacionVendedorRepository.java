package com.jjm.ecommerce.repository;

import com.jjm.ecommerce.model.CalificacionVendedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CalificacionVendedorRepository extends JpaRepository<CalificacionVendedor, Integer> {
    List<CalificacionVendedor> findByAliado_IdOrderByFechaDesc(Integer idAliado);
    boolean existsByAliado_IdAndUsuario_IdAndPedido_Id(Integer idAliado, Integer idUsuario, Integer idPedido);
    List<CalificacionVendedor> findByUsuario_Id(Integer idUsuario);

    /** [promedio, total] de un aliado (una sola fila). */
    @Query("SELECT AVG(c.calificacion), COUNT(c) FROM CalificacionVendedor c WHERE c.aliado.id = :idAliado")
    List<Object[]> resumenDeAliado(@Param("idAliado") Integer idAliado);
}
