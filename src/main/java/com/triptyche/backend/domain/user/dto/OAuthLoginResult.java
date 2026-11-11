package com.triptyche.backend.domain.user.dto;

import com.triptyche.backend.domain.user.model.User;

public record OAuthLoginResult(User user, boolean newUser) {

}
