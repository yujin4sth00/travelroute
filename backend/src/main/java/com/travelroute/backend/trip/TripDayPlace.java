package com.travelroute.backend.trip;

import com.travelroute.backend.place.Place;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "trip_day_places")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripDayPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_day_id", nullable = false)
    private TripDay tripDay;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(name = "visit_order", nullable = false)
    private Integer visitOrder;

    @Column(name = "is_locked", nullable = false)
    private boolean locked;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public TripDayPlace(TripDay tripDay, Place place, Integer visitOrder, boolean locked) {
        this.tripDay = tripDay;
        this.place = place;
        this.visitOrder = visitOrder;
        this.locked = locked;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public void updateOrder(int visitOrder, boolean locked) {
        this.visitOrder = visitOrder;
        this.locked = locked;
    }
}
