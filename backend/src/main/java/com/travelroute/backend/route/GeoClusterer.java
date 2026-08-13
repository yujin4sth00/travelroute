package com.travelroute.backend.route;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 좌표 목록을 k개의 지리적 그룹으로 묶는다 (Haversine 기반 K-means).
 * 외부 API를 호출하지 않고 순수 좌표 연산으로만 동작한다.
 */
@Component
public class GeoClusterer {

    private static final int MAX_ITERATIONS = 50;
    private static final double CONVERGENCE_THRESHOLD_METERS = 1.0;

    public List<List<RoutePoint>> cluster(List<RoutePoint> points, int k) {
        if (points.isEmpty() || k <= 0) {
            return List.of();
        }

        int effectiveK = Math.min(k, points.size());
        List<RoutePoint> centroids = seedCentroids(points, effectiveK);
        List<List<RoutePoint>> clusters = assignToClusters(points, centroids);

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            List<RoutePoint> newCentroids = recomputeCentroids(clusters, centroids);
            boolean converged = centroidsConverged(centroids, newCentroids);
            centroids = newCentroids;
            clusters = assignToClusters(points, centroids);
            if (converged) {
                break;
            }
        }

        return clusters;
    }

    private List<RoutePoint> seedCentroids(List<RoutePoint> points, int k) {
        List<RoutePoint> sorted = points.stream()
                .sorted(Comparator.comparingDouble(RoutePoint::lat).thenComparingDouble(RoutePoint::lng))
                .toList();

        List<RoutePoint> centroids = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            int index = Math.min((int) ((i + 0.5) * sorted.size() / k), sorted.size() - 1);
            centroids.add(sorted.get(index));
        }
        return centroids;
    }

    private List<List<RoutePoint>> assignToClusters(List<RoutePoint> points, List<RoutePoint> centroids) {
        List<List<RoutePoint>> clusters = new ArrayList<>();
        for (int i = 0; i < centroids.size(); i++) {
            clusters.add(new ArrayList<>());
        }

        for (RoutePoint point : points) {
            int nearestIndex = 0;
            double nearestDistance = Double.MAX_VALUE;
            for (int i = 0; i < centroids.size(); i++) {
                double distance = distance(point, centroids.get(i));
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestIndex = i;
                }
            }
            clusters.get(nearestIndex).add(point);
        }
        return clusters;
    }

    private List<RoutePoint> recomputeCentroids(List<List<RoutePoint>> clusters, List<RoutePoint> previousCentroids) {
        List<RoutePoint> newCentroids = new ArrayList<>();
        for (int i = 0; i < clusters.size(); i++) {
            List<RoutePoint> cluster = clusters.get(i);
            if (cluster.isEmpty()) {
                newCentroids.add(previousCentroids.get(i));
                continue;
            }
            double avgLat = cluster.stream().mapToDouble(RoutePoint::lat).average().orElseThrow();
            double avgLng = cluster.stream().mapToDouble(RoutePoint::lng).average().orElseThrow();
            newCentroids.add(new RoutePoint(null, avgLat, avgLng));
        }
        return newCentroids;
    }

    private boolean centroidsConverged(List<RoutePoint> previous, List<RoutePoint> current) {
        for (int i = 0; i < previous.size(); i++) {
            if (distance(previous.get(i), current.get(i)) > CONVERGENCE_THRESHOLD_METERS) {
                return false;
            }
        }
        return true;
    }

    private double distance(RoutePoint a, RoutePoint b) {
        return HaversineCalculator.distanceMeters(a.lat(), a.lng(), b.lat(), b.lng());
    }
}
