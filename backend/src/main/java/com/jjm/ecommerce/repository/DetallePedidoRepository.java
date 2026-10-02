package com.jjm.ecommerce.repository;

import com.jjm.ecommerce.model.DetallePedido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Integer> {
    List<DetallePedido> findByAliado_Id(Integer idAliado);
    List<DetallePedido> findByProducto_IdAndPedido_Usuario_Id(Integer idProducto, Integer idUsuario);
    boolean existsByProducto_Id(Integer idProducto);
    boolean existsByAliado_Id(Integer idAliado);
}
