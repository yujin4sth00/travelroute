package com.travelroute.backend.place;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "places")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String address;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    @Column(length = 50)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(name = "kakao_place_id", length = 50)
    private String kakaoPlaceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Place(Long userId, String name, String address, Double lat, Double lng,
                 String category, String memo, String kakaoPlaceId) {
        this.userId = userId;
        this.name = name;
        this.address = address;
        this.lat = lat;
        this.lng = lng;
        this.category = category;
        this.memo = memo;
        this.kakaoPlaceId = kakaoPlaceId;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
