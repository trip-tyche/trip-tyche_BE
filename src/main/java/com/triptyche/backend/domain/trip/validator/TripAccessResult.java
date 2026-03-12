package com.triptyche.backend.domain.trip.validator;

import com.triptyche.backend.domain.trip.model.Trip;
import com.triptyche.backend.domain.user.model.User;

public record TripAccessResult(Trip trip, User user) {}