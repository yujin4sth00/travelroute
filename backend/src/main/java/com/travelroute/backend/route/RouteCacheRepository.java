package com.travelroute.backend.route;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteCacheRepository extends JpaRepository<RouteCache, Long> {

    Optional<RouteCache> findByOriginPlaceIdAndDestinationPlaceId(Long originPlaceId, Long destinationPlaceId);
}
