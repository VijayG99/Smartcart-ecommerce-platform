package com.smartcart.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "inventory")
@Check(constraints = "quantity >= 0")   // last line of defence: the database itself refuses negative stock
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Inventory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @UpdateTimestamp
    private Instant updatedAt;
}
