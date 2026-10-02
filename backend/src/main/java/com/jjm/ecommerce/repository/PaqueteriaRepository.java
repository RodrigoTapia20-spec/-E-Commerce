package com.jjm.ecommerce.repository;

import com.jjm.ecommerce.model.Paqueteria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaqueteriaRepository extends JpaRepository<Paqueteria, Integer> {
    List<Paqueteria> findByActivaTrue();
}
