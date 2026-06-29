package com.lebarapp.security;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Converts a validated JWT into an authenticated {@link UsernamePasswordAuthenticationToken}
 * whose principal is a {@link SecurityUserPrincipal} loaded live from PostgreSQL.
 * This ensures that every authenticated API request verifies:
 * <ul>
 *   <li>the user still exists;</li>
 *   <li>{@code active = true};</li>
 *   <li>the role matches the database (not just the JWT claim).</li>
 * </ul>
 *
 * <p>If the user is deleted or disabled, a {@link BadCredentialsException} is thrown;
 * Spring Security's {@code BearerTokenAuthenticationFilter} catches it and delegates
 * to {@link JsonAuthenticationEntryPoint}, which returns a 401 {@code INVALID_TOKEN}.</p>
 *
 * <p>A single database lookup is performed per request: the
 * {@link BarmakerUserDetailsService} loads the user case-insensitively and
 * builds the principal. The authorities are derived from the reloaded
 * database state, not from the JWT claims, so a token whose role claim was
 * tampered with has no effect.</p>
 */
@Component
public class ActiveUserJwtAuthenticationConverter implements Converter<Jwt, UsernamePasswordAuthenticationToken> {

    private final UserDetailsService userDetailsService;

    public ActiveUserJwtAuthenticationConverter(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    public UsernamePasswordAuthenticationToken convert(Jwt jwt) {
        String username = jwt.getSubject();
        UserDetails principal = userDetailsService.loadUserByUsername(username);

        if (!principal.isEnabled()) {
            throw new BadCredentialsException("User account is disabled");
        }

        return new UsernamePasswordAuthenticationToken(
                principal, jwt, List.copyOf(principal.getAuthorities()));
    }
}
