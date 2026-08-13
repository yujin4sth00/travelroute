package com.travelroute.backend.route;

import com.travelroute.backend.place.Place;
import com.travelroute.backend.trip.TripAccessGuard;
import com.travelroute.backend.trip.TripDay;
import com.travelroute.backend.trip.TripDayPlace;
import com.travelroute.backend.trip.TripDayPlaceRepository;
import com.travelroute.backend.trip.TripDayRepository;
import com.travelroute.backend.trip.TripService;
import com.travelroute.backend.trip.dto.TripDetailResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 여행에 배치된 장소 전체(모든 날짜)를 여행 일수만큼 지리적으로 그룹핑해서 날짜별로 재배치한다.
 * is_locked=true인 장소는 현재 날짜와 순서를 그대로 유지하고, 잠기지 않은 장소만 대상으로 삼는다.
 * 좌표 연산(K-means)만 사용하며 외부 API를 호출하지 않는다.
 */
@Service
@RequiredArgsConstructor
public class AutoAssignService {

    private final TripDayRepository tripDayRepository;
    private final TripDayPlaceRepository tripDayPlaceRepository;
    private final GeoClusterer geoClusterer;
    private final RouteOptimizer routeOptimizer;
    private final TripService tripService;
    private final TripAccessGuard tripAccessGuard;

    @Transactional
    public TripDetailResponse autoAssign(Long tripId) {
        tripAccessGuard.requireOwnedTrip(tripId);

        List<TripDay> days = tripDayRepository.findByTripIdOrderByDayNumberAsc(tripId);
        List<TripDayPlace> allEntries = tripDayPlaceRepository.findAllByTripIdOrderByDayAndOrder(tripId);
        List<TripDayPlace> unlocked = allEntries.stream().filter(tdp -> !tdp.isLocked()).toList();

        if (unlocked.isEmpty()) {
            return tripService.getTripDetail(tripId);
        }

        List<RoutePoint> unlockedPoints = unlocked.stream()
                .map(tdp -> new RoutePoint(tdp.getId(), tdp.getPlace().getLat(), tdp.getPlace().getLng()))
                .toList();

        List<List<RoutePoint>> clusters = geoClusterer.cluster(unlockedPoints, days.size());

        RoutePoint reference = days.get(0).getStartPlace() != null
                ? toRoutePoint(days.get(0).getStartPlace())
                : centroidOf(unlockedPoints);
        List<List<RoutePoint>> orderedClusters = orderClustersForDays(clusters, reference);

        Map<Long, TripDayPlace> unlockedById = unlocked.stream()
                .collect(Collectors.toMap(TripDayPlace::getId, tdp -> tdp));

        for (int i = 0; i < days.size(); i++) {
            TripDay day = days.get(i);
            List<RoutePoint> clusterPoints = i < orderedClusters.size() ? orderedClusters.get(i) : List.of();
            List<RoutePoint> orderedClusterPoints = reorderWithinDay(day, clusterPoints);

            List<TripDayPlace> lockedInDay = allEntries.stream()
                    .filter(tdp -> tdp.isLocked() && tdp.getTripDay().getId().equals(day.getId()))
                    .toList();

            int position = 0;
            for (TripDayPlace locked : lockedInDay) {
                position++;
                locked.updateOrder(position, true);
            }
            for (RoutePoint point : orderedClusterPoints) {
                TripDayPlace tdp = unlockedById.get(point.id());
                tdp.reassignDay(day);
                position++;
                tdp.updateOrder(position, false);
            }
        }

        return tripService.getTripDetail(tripId);
    }

    private List<RoutePoint> reorderWithinDay(TripDay day, List<RoutePoint> clusterPoints) {
        if (clusterPoints.size() < 2 || day.getStartPlace() == null || day.getEndPlace() == null) {
            return clusterPoints;
        }
        return routeOptimizer.optimize(toRoutePoint(day.getStartPlace()), toRoutePoint(day.getEndPlace()), clusterPoints);
    }

    private List<List<RoutePoint>> orderClustersForDays(List<List<RoutePoint>> clusters, RoutePoint reference) {
        List<List<RoutePoint>> remaining = new ArrayList<>(clusters);
        List<List<RoutePoint>> ordered = new ArrayList<>();
        RoutePoint current = reference;

        while (!remaining.isEmpty()) {
            int nearestIndex = 0;
            double nearestDistance = Double.MAX_VALUE;
            for (int i = 0; i < remaining.size(); i++) {
                RoutePoint centroid = centroidOf(remaining.get(i));
                double distance = HaversineCalculator.distanceMeters(
                        current.lat(), current.lng(), centroid.lat(), centroid.lng());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestIndex = i;
                }
            }
            List<RoutePoint> chosen = remaining.remove(nearestIndex);
            ordered.add(chosen);
            current = centroidOf(chosen);
        }
        return ordered;
    }

    private RoutePoint centroidOf(List<RoutePoint> points) {
        double avgLat = points.stream().mapToDouble(RoutePoint::lat).average().orElse(0);
        double avgLng = points.stream().mapToDouble(RoutePoint::lng).average().orElse(0);
        return new RoutePoint(null, avgLat, avgLng);
    }

    private RoutePoint toRoutePoint(Place place) {
        return new RoutePoint(place.getId(), place.getLat(), place.getLng());
    }
}
