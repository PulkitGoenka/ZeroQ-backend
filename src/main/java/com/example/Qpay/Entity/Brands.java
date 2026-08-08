package com.example.Qpay.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "brands")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Brands{

     @Id
     @GeneratedValue(strategy = GenerationType.UUID)
     private UUID id;

     @Column(nullable = false, unique = true, length = 100)
     private String name;

     @Column(nullable = false, unique = true, length = 100)
     private String slug;

     private String description;

     @Column(name = "logo_url")
     private String logoUrl;

     @Column(name = "is_active", nullable = false)
     @Builder.Default
     private Boolean isActive = true;

     @CreationTimestamp
     private OffsetDateTime createdAt;

     @OneToMany(mappedBy = "brand", fetch = FetchType.LAZY)
     private List<Stores> stores;
}