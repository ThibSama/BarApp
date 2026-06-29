package com.lebarapp.dto;

/**
 * Successful login response: a signed access token plus the authenticated
 * user's public profile. The token must be sent as
 * {@code Authorization: Bearer &lt;accessToken&gt;} on subsequent requests.
 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        AuthUserDto user) {
}
