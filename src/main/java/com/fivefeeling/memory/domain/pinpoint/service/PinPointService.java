package com.fivefeeling.memory.domain.pinpoint.service;

import com.fivefeeling.memory.domain.pinpoint.model.PinPoint;
import com.fivefeeling.memory.domain.pinpoint.repository.PinPointRepository;
import com.fivefeeling.memory.domain.trip.model.Trip;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PinPointService {

  private final PinPointRepository pinPointRepository;

  private static final double EARTH_RADIUS_KM = 6371.01; // 지구 반경

  public PinPoint findOrCreatePinPoint(Trip trip, Double latitude, Double longitude) {
    List<PinPoint> existingPinPoints = pinPointRepository.findByTripTripId(trip.getTripId());

    for (PinPoint pinPoint : existingPinPoints) {
      double distance = calculateDistance(pinPoint.getLatitude(), pinPoint.getLongitude(), latitude, longitude);
      if (distance <= 1) { // 1km
        return pinPoint;
      }
    }

    // 2km 이내에 있는 PinPoint가 없다면 새로운 PinPoint 생성
    PinPoint newPinPoint = new PinPoint();
    newPinPoint.setTrip(trip);
    newPinPoint.setLatitude(latitude);
    newPinPoint.setLongitude(longitude);
    return pinPointRepository.save(newPinPoint);
  }

  private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
    double latDistance = Math.toRadians(lat2 - lat1);
    double lonDistance = Math.toRadians(lon2 - lon1);

    double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
        + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return EARTH_RADIUS_KM * c;
  }
}
