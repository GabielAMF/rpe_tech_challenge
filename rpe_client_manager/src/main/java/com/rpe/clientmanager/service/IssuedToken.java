package com.rpe.clientmanager.service;

import java.time.Instant;

/** A signed JWT and the moment it stops being accepted. */
public record IssuedToken(String value, Instant expiresAt) {
}
