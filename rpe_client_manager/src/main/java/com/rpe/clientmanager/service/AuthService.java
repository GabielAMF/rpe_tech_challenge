package com.rpe.clientmanager.service;

import com.rpe.clientmanager.domain.AppUser;
import com.rpe.clientmanager.domain.UserRole;
import com.rpe.clientmanager.domain.Username;

/**
 * Authentication use cases: logging in and managing the accounts allowed to call this API.
 */
public interface AuthService {

    /** @throws com.rpe.clientmanager.exception.InvalidCredentialsException on an unknown user or wrong password */
    IssuedToken login(Username username, String rawPassword);

    /** @throws com.rpe.clientmanager.exception.UsernameAlreadyExistsException if the username is taken */
    AppUser createUser(Username username, String rawPassword, UserRole role);
}
