package com.travelroute.backend.trip;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripDayPlaceRepository extends JpaRepository<TripDayPlace, Long> {

    List<TripDayPlace> findByTripDayIdOrderByVisitOrderAsc(Long tripDayId);

    Optional<TripDayPlace> findByIdAndTripDayId(Long id, Long tripDayId);

    @Query("SELECT COALESCE(MAX(tdp.visitOrder), 0) FROM TripDayPlace tdp WHERE tdp.tripDay.id = :tripDayId")
    Integer findMaxVisitOrder(@Param("tripDayId") Long tripDayId);
}
