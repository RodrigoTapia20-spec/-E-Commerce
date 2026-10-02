package com.jjm.ecommerce.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "metodos_pago")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MetodoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_metodo")
    private Integer id;

    @Column(nullable = false, length = 50)
    private String nombre; // TARJETA_CREDITO, TARJETA_DEBITO, TRANSFERENCIA, OXXO, SEVEN, VALE_DESPENSA, BILLETERA_DIGITAL
}
