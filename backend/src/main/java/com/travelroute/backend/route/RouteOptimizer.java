package com.travelroute.backend.route;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 출발지/도착지가 고정된 상태에서 중간 지점들을 방문하는 순서를 계산한다.
 * 외부 API를 호출하지 않고 Haversine 직선거리만으로 근사치를 구한다 (Nearest Neighbor + 2-opt).
 */
@Component
public class RouteOptimizer {

    public List<RoutePoint> optimize(RoutePoint start, RoutePoint end, List<RoutePoint> waypoints) {
        if (waypoints.isEmpty()) {
            return List.of();
        }

        List<RoutePoint> initialRoute = nearestNeighbor(start, waypoints);
        return twoOpt(start, end, initialRoute);
    }

    private List<RoutePoint> nearestNeighbor(RoutePoint start, List<RoutePoint> waypoints) {
        List<RoutePoint> remaining = new ArrayList<>(waypoints);
        List<RoutePoint> route = new ArrayList<>();

        RoutePoint current = start;
        while (!remaining.isEmpty()) {
            RoutePoint nearest = remaining.get(0);
            double nearestDistance = distance(current, nearest);
            for (RoutePoint candidate : remaining) {
                double candidateDistance = distance(current, candidate);
                if (candidateDistance < nearestDistance) {
                    nearest = candidate;
                    nearestDistance = candidateDistance;
                }
            }
            route.add(nearest);
            remaining.remove(nearest);
            current = nearest;
        }
        return route;
    }

    private List<RoutePoint> twoOpt(RoutePoint start, RoutePoint end, List<RoutePoint> initialRoute) {
        List<RoutePoint> path = new ArrayList<>();
        path.add(start);
        path.addAll(initialRoute);
        path.add(end);

        int n = path.size();
        boolean improved = true;
        while (improved) {
            improved = false;
            for (int i = 1; i < n - 2; i++) {
                for (int j = i + 1; j < n - 1; j++) {
                    if (swapDelta(path, i, j) < -1e-9) {
                        Collections.reverse(path.subList(i, j + 1));
                        improved = true;
                    }
                }
            }
        }

        return path.subList(1, n - 1);
    }

    private double swapDelta(List<RoutePoint> path, int i, int j) {
        RoutePoint a = path.get(i - 1);
        RoutePoint b = path.get(i);
        RoutePoint c = path.get(j);
        RoutePoint d = path.get(j + 1);

        double before = distance(a, b) + distance(c, d);
        double after = distance(a, c) + distance(b, d);
        return after - before;
    }

    private double distance(RoutePoint p1, RoutePoint p2) {
        return HaversineCalculator.distanceMeters(p1.lat(), p1.lng(), p2.lat(), p2.lng());
    }
}
