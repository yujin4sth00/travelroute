package com.travelroute.backend.route;

import com.travelroute.backend.place.Place;
import com.travelroute.backend.route.dto.DayRouteResponse;
import com.travelroute.backend.route.dto.RouteSegmentResponse;
import com.travelroute.backend.trip.TripDay;
import com.travelroute.backend.trip.TripDayNotFoundException;
import com.travelroute.backend.trip.TripDayPlaceRepository;
import com.travelroute.backend.trip.TripDayRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DayRouteService {

    private final TripDayRepository tripDayRepository;
    private final TripDayPlaceRepository tripDayPlaceRepository;
    private final RouteCacheService routeCacheService;

    @Transactional
    public DayRouteResponse getDayRoute(Long tripId, Long dayId) {
        TripDay day = tripDayRepository.findByIdAndTripId(dayId, tripId)
                .orElseThrow(() -> new TripDayNotFoundException(tripId, dayId));

        Place startPlace = day.getStartPlace();
        Place endPlace = day.getEndPlace();
        if (startPlace == null || endPlace == null) {
            throw new MissingRoutePlacesException(tripId, dayId);
        }

        List<Place> fullSequence = new ArrayList<>();
        fullSequence.add(startPlace);
        tripDayPlaceRepository.findByTripDayIdOrderByVisitOrderAsc(dayId)
                .forEach(tripDayPlace -> fullSequence.add(tripDayPlace.getPlace()));
        fullSequence.add(endPlace);

        List<RouteSegmentResponse> segments = new ArrayList<>();
        for (int i = 0; i < fullSequence.size() - 1; i++) {
            segments.add(routeCacheService.getOrFetchSegment(fullSequence.get(i), fullSequence.get(i + 1)));
        }

        int totalDistance = segments.stream().mapToInt(RouteSegmentResponse::distanceM).sum();
        int totalDuration = segments.stream().mapToInt(RouteSegmentResponse::durationSec).sum();

        return new DayRouteResponse(totalDistance, totalDuration, segments);
    }
}
