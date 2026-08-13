package com.travelroute.backend.route;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "route_cache",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_route",
                columnNames = {"origin_place_id", "destination_place_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RouteCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "origin_place_id", nullable = false)
    private Long originPlaceId;

    @Column(name = "destination_place_id", nullable = false)
    private Long destinationPlaceId;

    @Column(name = "distance_m")
    private Integer distanceM;

    @Column(name = "duration_sec")
    private Integer durationSec;

    @Lob
    @Column(name = "path_json", columnDefinition = "JSON")
    private String pathJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private RouteSource source;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public RouteCache(Long originPlaceId, Long destinationPlaceId, Integer distanceM,
                       Integer durationSec, String pathJson, RouteSource source) {
        this.originPlaceId = originPlaceId;
        this.destinationPlaceId = destinationPlaceId;
        this.distanceM = distanceM;
        this.durationSec = durationSec;
        this.pathJson = pathJson;
        this.source = source;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
