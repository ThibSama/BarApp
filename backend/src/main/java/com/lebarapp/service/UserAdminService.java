package com.lebarapp.service;

import com.lebarapp.dto.CreateBarmakerRequest;
import com.lebarapp.dto.UserAdminResponse;
import com.lebarapp.entity.AppUser;
import com.lebarapp.exception.UsernameAlreadyExistsException;
import com.lebarapp.repository.AppUserRepository;
import jakarta.persistence.EntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Manager-only staff administration. Lists staff accounts and creates new
 * barmakers. Every created account is fixed to {@code BARMAKER} and active
 * (enforced by {@link AppUser#createBarmaker}); this service never accepts a
 * role or active flag from the caller.
 *
 * <p>The plaintext password is BCrypt-hashed with the project's configured
 * {@link PasswordEncoder} and is never logged, stored in clear, or echoed back.
 * Username uniqueness is case-insensitive and enforced twice: a best-effort
 * pre-check and the database functional unique index (final authority against a
 * concurrent race).</p>
 */
@Service
public class UserAdminService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    public UserAdminService(AppUserRepository appUserRepository,
                            PasswordEncoder passwordEncoder,
                            EntityManager entityManager) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.entityManager = entityManager;
    }

    /** Deterministic listing of every staff account (barmakers and managers). */
    @Transactional(readOnly = true)
    public List<UserAdminResponse> list() {
        return appUserRepository.findAllByOrderByDisplayNameAscUsernameAscIdAsc().stream()
                .map(UserAdminService::toResponse)
                .toList();
    }

    /**
     * Creates a new active barmaker account.
     *
     * @throws UsernameAlreadyExistsException if the username already exists
     *         (case-insensitive), whether caught by the pre-check or by the
     *         database unique constraint under concurrency
     */
    @Transactional
    public UserAdminResponse createBarmaker(CreateBarmakerRequest request) {
        String displayName = request.displayName().trim();
        String username = request.username().trim();

        if (appUserRepository.existsByUsernameIgnoreCase(username)) {
            throw new UsernameAlreadyExistsException();
        }

        // The password is hashed verbatim (never trimmed); only the hash is stored.
        String passwordHash = passwordEncoder.encode(request.password());
        AppUser user = AppUser.createBarmaker(username, passwordHash, displayName);

        try {
            AppUser saved = appUserRepository.saveAndFlush(user);
            // createdAt is assigned by the database default; refresh so the
            // response carries the persisted timestamp rather than null.
            entityManager.refresh(saved);
            return toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            // Concurrent insert won the race on the case-insensitive unique index.
            throw new UsernameAlreadyExistsException();
        }
    }

    private static UserAdminResponse toResponse(AppUser user) {
        return new UserAdminResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt());
    }
}
