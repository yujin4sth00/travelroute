package com.travelroute.backend.trip;

import com.travelroute.backend.auth.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 로그인한 사용자가 해당 여행의 소유자인지 확인한다.
 * 다른 사용자의 여행이면 존재 여부를 노출하지 않기 위해 404(TripNotFoundException)로 응답한다.
 */
@Component
@RequiredArgsConstructor
public class TripAccessGuard {

    private final TripRepository tripRepository;
    private final CurrentUserProvider currentUserProvider;

    public Trip requireOwnedTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));

        if (!trip.getUserId().equals(currentUserProvider.getUserId())) {
            throw new TripNotFoundException(tripId);
        }
        return trip;
    }
}
