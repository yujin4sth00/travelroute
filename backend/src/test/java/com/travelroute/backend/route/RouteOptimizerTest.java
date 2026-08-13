package com.travelroute.backend.route;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RouteOptimizerTest {

    private final RouteOptimizer optimizer = new RouteOptimizer();

    @Test
    void optimize_returnsEmptyList_forNoWaypoints() {
        List<RoutePoint> result = optimizer.optimize(
                new RoutePoint(0L, 0, 0), new RoutePoint(1L, 10, 10), List.of());

        assertThat(result).isEmpty();
    }

    @Test
    void optimize_returnsSingleWaypoint_whenOnlyOneGiven() {
        RoutePoint only = new RoutePoint(5L, 1, 1);

        List<RoutePoint> result = optimizer.optimize(
                new RoutePoint(0L, 0, 0), new RoutePoint(1L, 10, 10), List.of(only));

        assertThat(result).containsExactly(only);
    }

    @Test
    void optimize_ordersWaypointsAlongStraightLine_regardlessOfInputOrder() {
        RoutePoint start = new RoutePoint(0L, 0, 0);
        RoutePoint end = new RoutePoint(99L, 0, 6);
        RoutePoint p1 = new RoutePoint(1L, 0, 1);
        RoutePoint p2 = new RoutePoint(2L, 0, 2);
        RoutePoint p3 = new RoutePoint(3L, 0, 3);
        RoutePoint p4 = new RoutePoint(4L, 0, 4);
        RoutePoint p5 = new RoutePoint(5L, 0, 5);

        List<RoutePoint> shuffled = List.of(p3, p5, p1, p4, p2);

        List<RoutePoint> result = optimizer.optimize(start, end, shuffled);

        assertThat(result).containsExactly(p1, p2, p3, p4, p5);
    }

    @Test
    void optimize_producesPathNoLongerThanNaiveInputOrder() {
        RoutePoint start = new RoutePoint(0L, 37.50, 127.00);
        RoutePoint end = new RoutePoint(9L, 37.54, 127.06);

        // 지그재그로 섞인 더미 좌표: 입력 순서 그대로 방문하면 비효율적인 경로가 되도록 구성
        RoutePoint a = new RoutePoint(1L, 37.53, 127.01);
        RoutePoint b = new RoutePoint(2L, 37.505, 127.02);
        RoutePoint c = new RoutePoint(3L, 37.52, 127.05);
        RoutePoint d = new RoutePoint(4L, 37.515, 127.015);
        List<RoutePoint> naiveOrder = List.of(a, b, c, d);

        List<RoutePoint> result = optimizer.optimize(start, end, naiveOrder);

        double naiveDistance = totalDistance(start, naiveOrder, end);
        double optimizedDistance = totalDistance(start, result, end);

        assertThat(optimizedDistance).isLessThanOrEqualTo(naiveDistance);
        assertThat(result).containsExactlyInAnyOrderElementsOf(naiveOrder);
    }

    private double totalDistance(RoutePoint start, List<RoutePoint> waypoints, RoutePoint end) {
        List<RoutePoint> path = new ArrayList<>();
        path.add(start);
        path.addAll(waypoints);
        path.add(end);

        double total = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            RoutePoint p1 = path.get(i);
            RoutePoint p2 = path.get(i + 1);
            total += HaversineCalculator.distanceMeters(p1.lat(), p1.lng(), p2.lat(), p2.lng());
        }
        return total;
    }
}
