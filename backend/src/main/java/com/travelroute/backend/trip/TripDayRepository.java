package com.travelroute.backend.trip;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripDayRepository extends JpaRepository<TripDay, Long> {

    List<TripDay> findByTripIdOrderByDayNumberAsc(Long tripId);

    Optional<TripDay> findByIdAndTripId(Long id, Long tripId);
}
