package com.sheyingtong.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "gear")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Gear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String brand;

    @Column(nullable = false, length = 200)
    private String model;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private GearCategory category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 500)
    private String description;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    @Column(length = 50)
    private String pixel;

    @Column(length = 50)
    private String continuousShooting;

    @Column(length = 50)
    private String video;

    @Column(length = 50)
    private String focus;

    @Column(columnDefinition = "TEXT")
    private String specifications;

    @Builder.Default
    @Column(nullable = false)
    private Integer rating = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer reviewCount = 0;

    @Column(columnDefinition = "TEXT")
    private String aiReview;

    @Builder.Default
    @Column(nullable = false)
    private Integer aiScore = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
