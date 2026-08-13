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
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "trip_days")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    @Column(name = "date")
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "start_place_id")
    private Place startPlace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "end_place_id")
    private Place endPlace;

    @Builder
    public TripDay(Trip trip, Integer dayNumber, LocalDate date) {
        this.trip = trip;
        this.dayNumber = dayNumber;
        this.date = date;
    }

    public void assignPlaces(Place startPlace, Place endPlace) {
        this.startPlace = startPlace;
        this.endPlace = endPlace;
    }
}
