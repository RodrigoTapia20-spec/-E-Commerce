package com.jjm.ecommerce.repository;

import com.jjm.ecommerce.model.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FacturaRepository extends JpaRepository<Factura, Integer> {
    Optional<Factura> findByPedido_Id(Integer idPedido);
    List<Factura> findByUsuario_Id(Integer idUsuario);
}
