package com.travelroute.backend.route;

import com.travelroute.backend.place.Place;
import com.travelroute.backend.trip.TripAccessGuard;
import com.travelroute.backend.trip.TripDay;
import com.travelroute.backend.trip.TripDayNotFoundException;
import com.travelroute.backend.trip.TripDayPlace;
import com.travelroute.backend.trip.TripDayPlaceRepository;
import com.travelroute.backend.trip.TripDayRepository;
import com.travelroute.backend.trip.dto.TripDayPlaceResponse;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RouteOptimizationService {

    private final TripDayRepository tripDayRepository;
    private final TripDayPlaceRepository tripDayPlaceRepository;
    private final RouteOptimizer routeOptimizer;
    private final TripAccessGuard tripAccessGuard;

    @Transactional
    public List<TripDayPlaceResponse> optimizeDay(Long tripId, Long dayId) {
        tripAccessGuard.requireOwnedTrip(tripId);
        TripDay day = tripDayRepository.findByIdAndTripId(dayId, tripId)
                .orElseThrow(() -> new TripDayNotFoundException(tripId, dayId));

        Place startPlace = day.getStartPlace();
        Place endPlace = day.getEndPlace();
        if (startPlace == null || endPlace == null) {
            throw new MissingRoutePlacesException(tripId, dayId);
        }

        List<TripDayPlace> currentOrder = tripDayPlaceRepository.findByTripDayIdOrderByVisitOrderAsc(dayId);
        List<TripDayPlace> unlocked = currentOrder.stream().filter(tdp -> !tdp.isLocked()).toList();

        if (!unlocked.isEmpty()) {
            RoutePoint start = new RoutePoint(startPlace.getId(), startPlace.getLat(), startPlace.getLng());
            RoutePoint end = new RoutePoint(endPlace.getId(), endPlace.getLat(), endPlace.getLng());
            List<RoutePoint> waypoints = unlocked.stream()
                    .map(tdp -> new RoutePoint(tdp.getId(), tdp.getPlace().getLat(), tdp.getPlace().getLng()))
                    .toList();

            List<RoutePoint> optimizedOrder = routeOptimizer.optimize(start, end, waypoints);

            Map<Long, TripDayPlace> unlockedById = unlocked.stream()
                    .collect(Collectors.toMap(TripDayPlace::getId, tdp -> tdp));

            Iterator<RoutePoint> orderIterator = optimizedOrder.iterator();
            int position = 1;
            for (TripDayPlace tdp : currentOrder) {
                if (tdp.isLocked()) {
                    tdp.updateOrder(position, true);
                } else {
                    TripDayPlace next = unlockedById.get(orderIterator.next().id());
                    next.updateOrder(position, false);
                }
                position++;
            }
        }

        return currentOrder.stream()
                .sorted(Comparator.comparing(TripDayPlace::getVisitOrder))
                .map(TripDayPlaceResponse::from)
                .toList();
    }
}
