package com.example.demo.domain;

public enum Role {
    PLAYER,
    ADMIN;

    public String asAuthority() {
        return "ROLE_" + name();
    }
}
