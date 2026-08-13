package com.travelroute.backend.trip;

import com.travelroute.backend.place.Place;
import com.travelroute.backend.place.PlaceNotFoundException;
import com.travelroute.backend.place.PlaceRepository;
import com.travelroute.backend.trip.dto.ReorderRequest;
import com.travelroute.backend.trip.dto.TripCreateRequest;
import com.travelroute.backend.trip.dto.TripDayPlaceCreateRequest;
import com.travelroute.backend.trip.dto.TripDayPlaceResponse;
import com.travelroute.backend.trip.dto.TripDayResponse;
import com.travelroute.backend.trip.dto.TripDayUpdateRequest;
import com.travelroute.backend.trip.dto.TripDetailResponse;
import com.travelroute.backend.trip.dto.TripResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final TripDayRepository tripDayRepository;
    private final TripDayPlaceRepository tripDayPlaceRepository;
    private final PlaceRepository placeRepository;

    @Transactional
    public TripResponse createTrip(TripCreateRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new InvalidTripPeriodException("종료일은 시작일보다 빠를 수 없습니다.");
        }

        Trip trip = Trip.builder()
                .title(request.title())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .build();
        trip = tripRepository.save(trip);

        List<TripDay> days = new ArrayList<>();
        LocalDate date = request.startDate();
        int dayNumber = 1;
        while (!date.isAfter(request.endDate())) {
            days.add(TripDay.builder()
                    .trip(trip)
                    .dayNumber(dayNumber)
                    .date(date)
                    .build());
            date = date.plusDays(1);
            dayNumber++;
        }
        tripDayRepository.saveAll(days);

        return TripResponse.from(trip);
    }

    public TripDetailResponse getTripDetail(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));

        List<TripDay> days = tripDayRepository.findByTripIdOrderByDayNumberAsc(tripId);
        List<TripDayResponse> dayResponses = days.stream()
                .map(day -> {
                    List<TripDayPlaceResponse> places = tripDayPlaceRepository
                            .findByTripDayIdOrderByVisitOrderAsc(day.getId())
                            .stream()
                            .map(TripDayPlaceResponse::from)
                            .toList();
                    return TripDayResponse.from(day, places);
                })
                .toList();

        return TripDetailResponse.from(trip, dayResponses);
    }

    @Transactional
    public TripDayResponse updateTripDay(Long tripId, Long dayId, TripDayUpdateRequest request) {
        TripDay day = getOwnedTripDay(tripId, dayId);

        Place startPlace = request.startPlaceId() != null
                ? placeRepository.findById(request.startPlaceId())
                        .orElseThrow(() -> new PlaceNotFoundException(request.startPlaceId()))
                : null;
        Place endPlace = request.endPlaceId() != null
                ? placeRepository.findById(request.endPlaceId())
                        .orElseThrow(() -> new PlaceNotFoundException(request.endPlaceId()))
                : null;

        day.assignPlaces(startPlace, endPlace);

        List<TripDayPlaceResponse> places = tripDayPlaceRepository
                .findByTripDayIdOrderByVisitOrderAsc(day.getId())
                .stream()
                .map(TripDayPlaceResponse::from)
                .toList();
        return TripDayResponse.from(day, places);
    }

    @Transactional
    public TripDayPlaceResponse addPlaceToDay(Long tripId, Long dayId, TripDayPlaceCreateRequest request) {
        TripDay day = getOwnedTripDay(tripId, dayId);
        Place place = placeRepository.findById(request.placeId())
                .orElseThrow(() -> new PlaceNotFoundException(request.placeId()));

        int nextOrder = tripDayPlaceRepository.findMaxVisitOrder(dayId) + 1;

        TripDayPlace tripDayPlace = TripDayPlace.builder()
                .tripDay(day)
                .place(place)
                .visitOrder(nextOrder)
                .locked(false)
                .build();
        tripDayPlaceRepository.save(tripDayPlace);

        return TripDayPlaceResponse.from(tripDayPlace);
    }

    @Transactional
    public void removePlaceFromDay(Long tripId, Long dayId, Long tripDayPlaceId) {
        getOwnedTripDay(tripId, dayId);
        TripDayPlace tripDayPlace = tripDayPlaceRepository.findByIdAndTripDayId(tripDayPlaceId, dayId)
                .orElseThrow(() -> new TripDayPlaceNotFoundException(tripDayPlaceId, dayId));
        tripDayPlaceRepository.delete(tripDayPlace);
    }

    @Transactional
    public List<TripDayPlaceResponse> reorderPlaces(Long tripId, Long dayId, ReorderRequest request) {
        getOwnedTripDay(tripId, dayId);

        List<TripDayPlace> existing = tripDayPlaceRepository.findByTripDayIdOrderByVisitOrderAsc(dayId);
        Map<Long, TripDayPlace> byId = existing.stream()
                .collect(java.util.stream.Collectors.toMap(TripDayPlace::getId, tdp -> tdp));

        Set<Long> existingIds = new HashSet<>(byId.keySet());
        Set<Long> requestedIds = new HashSet<>(request.tripDayPlaceIds());
        if (!existingIds.equals(requestedIds)) {
            throw new InvalidReorderRequestException("요청한 장소 목록이 해당 일차에 배치된 장소 목록과 일치하지 않습니다.");
        }

        List<TripDayPlaceResponse> result = new ArrayList<>();
        int order = 1;
        for (Long tripDayPlaceId : request.tripDayPlaceIds()) {
            TripDayPlace tripDayPlace = byId.get(tripDayPlaceId);
            tripDayPlace.updateOrder(order, true);
            result.add(TripDayPlaceResponse.from(tripDayPlace));
            order++;
        }
        return result;
    }

    private TripDay getOwnedTripDay(Long tripId, Long dayId) {
        return tripDayRepository.findByIdAndTripId(dayId, tripId)
                .orElseThrow(() -> new TripDayNotFoundException(tripId, dayId));
    }
}
