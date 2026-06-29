package com.lebarapp.security;

import com.lebarapp.entity.AppUser;
import com.lebarapp.enums.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security principal wrapping an {@link AppUser}. Only the minimum data
 * required for authorization ({@code userId}, {@code username}, {@code displayName},
 * {@code role}) is carried in memory. Built from the live database state by
 * {@link BarmakerUserDetailsService} on every authenticated request, so a
 * disabled or deleted user is immediately rejected.
 *
 * <p>The password hash is not stored here because this principal is built from
 * the JWT request flow (not the login flow); password verification happens only
 * in {@link com.lebarapp.service.AuthService#login}.</p>
 */
public class SecurityUserPrincipal implements UserDetails {

    private static final String ROLE_PREFIX = "ROLE_";

    private final Long userId;
    private final String username;
    private final String displayName;
    private final boolean active;
    private final UserRole role;

    public SecurityUserPrincipal(AppUser user) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.displayName = user.getDisplayName();
        this.active = user.isActive();
        this.role = user.getRole();
    }

    public Long getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UserRole getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(ROLE_PREFIX + role.name()));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
