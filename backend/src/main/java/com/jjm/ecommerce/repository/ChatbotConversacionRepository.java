package com.jjm.ecommerce.repository;

import com.jjm.ecommerce.model.ChatbotConversacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatbotConversacionRepository extends JpaRepository<ChatbotConversacion, Long> {
    java.util.List<ChatbotConversacion> findByUsuario_Id(Integer idUsuario);
}
