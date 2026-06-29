package com.lebarapp.security;

import com.lebarapp.repository.AppUserRepository;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads barmaker accounts from the {@code app_user} table using a
 * case-insensitive username lookup against the functional unique index. Used by
 * {@link ActiveUserJwtAuthenticationConverter} on every authenticated API
 * request to build the {@link SecurityUserPrincipal} from the live database
 * state.
 *
 * <p>If the user is not found, {@link UsernameNotFoundException} is thrown; the
 * converter's caller (Spring Security's {@code BearerTokenAuthenticationFilter})
 * wraps it so {@link JsonAuthenticationEntryPoint} returns a 401
 * {@code INVALID_TOKEN} — the token references a user that no longer exists.</p>
 */
@Service
public class BarmakerUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public BarmakerUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        return appUserRepository.findByUsernameIgnoreCase(username)
                .map(SecurityUserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
