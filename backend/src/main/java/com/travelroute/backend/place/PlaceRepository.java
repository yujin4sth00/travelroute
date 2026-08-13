package com.travelroute.backend.place;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    List<Place> findByUserId(Long userId);

    Optional<Place> findByIdAndUserId(Long id, Long userId);
}
