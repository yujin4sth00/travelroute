package com.travelroute.backend.trip;

import com.travelroute.backend.route.DayRouteService;
import com.travelroute.backend.route.RouteOptimizationService;
import com.travelroute.backend.route.dto.DayRouteResponse;
import com.travelroute.backend.trip.dto.ReorderRequest;
import com.travelroute.backend.trip.dto.TripCreateRequest;
import com.travelroute.backend.trip.dto.TripDayPlaceCreateRequest;
import com.travelroute.backend.trip.dto.TripDayPlaceResponse;
import com.travelroute.backend.trip.dto.TripDayResponse;
import com.travelroute.backend.trip.dto.TripDayUpdateRequest;
import com.travelroute.backend.trip.dto.TripDetailResponse;
import com.travelroute.backend.trip.dto.TripResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;
    private final RouteOptimizationService routeOptimizationService;
    private final DayRouteService dayRouteService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TripResponse create(@Valid @RequestBody TripCreateRequest request) {
        return tripService.createTrip(request);
    }

    @GetMapping("/{tripId}")
    public TripDetailResponse getDetail(@PathVariable Long tripId) {
        return tripService.getTripDetail(tripId);
    }

    @PatchMapping("/{tripId}/days/{dayId}")
    public TripDayResponse updateDay(@PathVariable Long tripId, @PathVariable Long dayId,
                                      @RequestBody TripDayUpdateRequest request) {
        return tripService.updateTripDay(tripId, dayId, request);
    }

    @PostMapping("/{tripId}/days/{dayId}/places")
    @ResponseStatus(HttpStatus.CREATED)
    public TripDayPlaceResponse addPlace(@PathVariable Long tripId, @PathVariable Long dayId,
                                          @Valid @RequestBody TripDayPlaceCreateRequest request) {
        return tripService.addPlaceToDay(tripId, dayId, request);
    }

    @DeleteMapping("/{tripId}/days/{dayId}/places/{tripDayPlaceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removePlace(@PathVariable Long tripId, @PathVariable Long dayId,
                             @PathVariable Long tripDayPlaceId) {
        tripService.removePlaceFromDay(tripId, dayId, tripDayPlaceId);
    }

    @PatchMapping("/{tripId}/days/{dayId}/places/reorder")
    public List<TripDayPlaceResponse> reorder(@PathVariable Long tripId, @PathVariable Long dayId,
                                               @Valid @RequestBody ReorderRequest request) {
        return tripService.reorderPlaces(tripId, dayId, request);
    }

    @PostMapping("/{tripId}/days/{dayId}/optimize")
    public List<TripDayPlaceResponse> optimize(@PathVariable Long tripId, @PathVariable Long dayId) {
        return routeOptimizationService.optimizeDay(tripId, dayId);
    }

    @GetMapping("/{tripId}/days/{dayId}/route")
    public DayRouteResponse getRoute(@PathVariable Long tripId, @PathVariable Long dayId) {
        return dayRouteService.getDayRoute(tripId, dayId);
    }
}
