package com.jjm.ecommerce.repository;

import com.jjm.ecommerce.model.Promocion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PromocionRepository extends JpaRepository<Promocion, Integer> {
    List<Promocion> findByAliado_IdOrderByFechaFinDesc(Integer idAliado);

    @Query("SELECT DISTINCT p FROM Promocion p WHERE p.estatus = 'ACTIVA' " +
           "AND p.fechaInicio <= :hoy AND p.fechaFin >= :hoy ORDER BY p.porcentajeDescuento DESC")
    List<Promocion> findVigentes(@Param("hoy") LocalDate hoy);

    /** Promociones vigentes que incluyen un producto (la de mayor descuento primero). */
    @Query("SELECT DISTINCT p FROM Promocion p JOIN p.productos pr WHERE pr.id = :idProducto " +
           "AND p.estatus = 'ACTIVA' AND p.fechaInicio <= :hoy AND p.fechaFin >= :hoy " +
           "ORDER BY p.porcentajeDescuento DESC")
    List<Promocion> findVigentesPorProducto(@Param("idProducto") Integer idProducto, @Param("hoy") LocalDate hoy);
}
