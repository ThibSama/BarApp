package com.lebarapp.service;

import com.lebarapp.dto.LoginResponse;
import com.lebarapp.dto.LoginRequest;
import com.lebarapp.entity.AppUser;
import com.lebarapp.exception.ApiErrorCode;
import com.lebarapp.exception.BusinessException;
import com.lebarapp.repository.AppUserRepository;
import com.lebarapp.security.JwtTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Barmaker login. Authentication uses BCrypt against the {@code app_user} table;
 * all invalid-credential cases (unknown user, wrong password, inactive account)
 * produce the same generic {@code INVALID_CREDENTIALS} error so that no
 * side-channel information leaks.
 *
 * <p>Post-login user verification (is the user still active?) is handled by
 * {@link com.lebarapp.security.ActiveUserJwtAuthenticationConverter} on every
 * authenticated request, so this service does not need a separate
 * {@code getCurrentUser} method.</p>
 */
@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(AppUserRepository appUserRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    /**
     * Authenticates a barmaker and issues a signed access token.
     *
     * @throws BusinessException with {@link ApiErrorCode#INVALID_CREDENTIALS}
     *         for unknown user, wrong password or inactive account
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        AppUser user = appUserRepository.findByUsernameIgnoreCase(request.username())
                .orElseThrow(() -> new BusinessException(ApiErrorCode.INVALID_CREDENTIALS));

        // BCrypt comparison against the stored hash; the supplied password is
        // never trimmed or modified. Inactive accounts cannot authenticate.
        if (!user.isActive()
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ApiErrorCode.INVALID_CREDENTIALS);
        }

        JwtTokenService.TokenResult token = jwtTokenService.createToken(user);
        return new LoginResponse(token.accessToken(), token.tokenType(),
                token.expirationSeconds(), toDto(user));
    }

    private static com.lebarapp.dto.AuthUserDto toDto(AppUser user) {
        return new com.lebarapp.dto.AuthUserDto(user.getId(), user.getUsername(),
                user.getDisplayName(), user.getRole());
    }
}
