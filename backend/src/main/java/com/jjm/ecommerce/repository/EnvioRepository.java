package com.jjm.ecommerce.repository;

import com.jjm.ecommerce.model.Envio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnvioRepository extends JpaRepository<Envio, Integer> {
    List<Envio> findByDetalle_Pedido_Id(Integer idPedido);
    Optional<Envio> findByDetalle_Id(Integer idDetalle);
    List<Envio> findByEstatusOrderByFechaGeneracionDesc(String estatus);
    List<Envio> findAllByOrderByFechaGeneracionDesc();
}
