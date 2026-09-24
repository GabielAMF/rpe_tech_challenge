package com.rpe.clientmanager.service;

import com.rpe.clientmanager.domain.AppUser;

/** Issues access tokens. AuthService depends on this interface, not on the JWT library (see JwtTokenService). */
public interface TokenService {

    IssuedToken issue(AppUser user);
}
