package com.travelroute.backend.trip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.travelroute.backend.auth.CurrentUserProvider;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TripAccessGuardTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private TripAccessGuard tripAccessGuard;

    @Test
    void requireOwnedTrip_returnsTrip_whenOwnedByCurrentUser() {
        Trip trip = Trip.builder().userId(1L).title("여행").build();
        ReflectionTestUtils.setField(trip, "id", 10L);

        given(tripRepository.findById(10L)).willReturn(Optional.of(trip));
        given(currentUserProvider.getUserId()).willReturn(1L);

        assertThat(tripAccessGuard.requireOwnedTrip(10L)).isSameAs(trip);
    }

    @Test
    void requireOwnedTrip_throwsTripNotFoundException_whenTripBelongsToAnotherUser() {
        Trip trip = Trip.builder().userId(2L).title("다른 사람 여행").build();
        ReflectionTestUtils.setField(trip, "id", 10L);

        given(tripRepository.findById(10L)).willReturn(Optional.of(trip));
        given(currentUserProvider.getUserId()).willReturn(1L);

        assertThatThrownBy(() -> tripAccessGuard.requireOwnedTrip(10L))
                .isInstanceOf(TripNotFoundException.class);
    }

    @Test
    void requireOwnedTrip_throwsTripNotFoundException_whenTripDoesNotExist() {
        given(tripRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> tripAccessGuard.requireOwnedTrip(999L))
                .isInstanceOf(TripNotFoundException.class);
    }
}
