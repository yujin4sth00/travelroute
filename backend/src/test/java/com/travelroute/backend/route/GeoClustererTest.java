package com.travelroute.backend.route;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class GeoClustererTest {

    private final GeoClusterer geoClusterer = new GeoClusterer();

    @Test
    void cluster_returnsEmpty_whenNoPoints() {
        List<List<RoutePoint>> result = geoClusterer.cluster(List.of(), 3);

        assertThat(result).isEmpty();
    }

    @Test
    void cluster_groupsClearlySeparatedPoints_intoCorrectClusters() {
        // 서울 그룹 (좁게 모여 있음)
        RoutePoint seoul1 = new RoutePoint(1L, 37.5665, 126.9780);
        RoutePoint seoul2 = new RoutePoint(2L, 37.5700, 126.9820);
        RoutePoint seoul3 = new RoutePoint(3L, 37.5630, 126.9750);

        // 부산 그룹 (서울과 멀리 떨어져 있음)
        RoutePoint busan1 = new RoutePoint(4L, 35.1796, 129.0756);
        RoutePoint busan2 = new RoutePoint(5L, 35.1830, 129.0800);
        RoutePoint busan3 = new RoutePoint(6L, 35.1760, 129.0720);

        List<RoutePoint> points = List.of(busan2, seoul1, busan1, seoul3, busan3, seoul2);

        List<List<RoutePoint>> clusters = geoClusterer.cluster(points, 2);

        assertThat(clusters).hasSize(2);

        Set<Long> seoulIds = Set.of(1L, 2L, 3L);
        Set<Long> busanIds = Set.of(4L, 5L, 6L);

        for (List<RoutePoint> cluster : clusters) {
            Set<Long> ids = cluster.stream().map(RoutePoint::id).collect(Collectors.toSet());
            assertThat(ids.equals(seoulIds) || ids.equals(busanIds)).isTrue();
        }
    }

    @Test
    void cluster_doesNotCrash_whenKExceedsPointCount() {
        List<RoutePoint> points = List.of(
                new RoutePoint(1L, 37.50, 127.00),
                new RoutePoint(2L, 37.51, 127.01));

        List<List<RoutePoint>> clusters = geoClusterer.cluster(points, 5);

        int totalAssigned = clusters.stream().mapToInt(List::size).sum();
        assertThat(totalAssigned).isEqualTo(2);
        assertThat(clusters.size()).isLessThanOrEqualTo(5);
    }
}
