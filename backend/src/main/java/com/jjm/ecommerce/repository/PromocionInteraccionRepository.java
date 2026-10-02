package com.jjm.ecommerce.repository;

import com.jjm.ecommerce.model.PromocionInteraccion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PromocionInteraccionRepository extends JpaRepository<PromocionInteraccion, Long> {
    List<PromocionInteraccion> findByPromocion_Id(Integer idPromocion);
}
