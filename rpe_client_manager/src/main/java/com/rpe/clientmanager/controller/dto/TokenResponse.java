package com.rpe.clientmanager.controller.dto;

/** OAuth2-style token response: send {@code accessToken} as {@code Authorization: Bearer <token>}. */
public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
