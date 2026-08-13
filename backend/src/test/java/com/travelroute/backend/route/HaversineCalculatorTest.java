package com.travelroute.backend.route;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class HaversineCalculatorTest {

    @Test
    void distanceMeters_returnsZero_forSamePoint() {
        double distance = HaversineCalculator.distanceMeters(37.5665, 126.9780, 37.5665, 126.9780);

        assertThat(distance).isEqualTo(0.0);
    }

    @Test
    void distanceMeters_matchesKnownDistance_seoulToBusan() {
        // 서울 시청 -> 부산 시청, 실제 직선거리는 약 325km
        double distance = HaversineCalculator.distanceMeters(37.5665, 126.9780, 35.1796, 129.0756);

        assertThat(distance).isCloseTo(325_000, within(5_000.0));
    }

    @Test
    void distanceMeters_isSymmetric() {
        double d1 = HaversineCalculator.distanceMeters(37.5665, 126.9780, 35.1796, 129.0756);
        double d2 = HaversineCalculator.distanceMeters(35.1796, 129.0756, 37.5665, 126.9780);

        assertThat(d1).isEqualTo(d2);
    }
}
