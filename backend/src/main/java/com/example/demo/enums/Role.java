package com.example.demo.enums;


public enum Role {
    PLAYER,
    ADMIN;

    public String asAuthority() {
        return "ROLE_" + name();
    }
}
