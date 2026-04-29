package com.triptyche.backend.domain.user.model;

public enum UserRole {
    USER,
    GUEST;

    public String authority() {
        return "ROLE_" + this.name();
    }
}