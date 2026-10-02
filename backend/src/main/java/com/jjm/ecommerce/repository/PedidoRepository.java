package com.jjm.ecommerce.repository;

import com.jjm.ecommerce.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    List<Pedido> findByUsuario_IdOrderByFechaPedidoDesc(Integer idUsuario);
    boolean existsByUsuario_Id(Integer idUsuario);
}
