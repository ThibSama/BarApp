package com.lebarapp.controller;

import com.lebarapp.dto.CreateBarmakerRequest;
import com.lebarapp.dto.UserAdminResponse;
import com.lebarapp.service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Manager-only staff administration API. Every route lives under
 * {@code /api/bar/users/**} and therefore requires {@code ROLE_MANAGER}
 * (enforced by the security filter chain, which lists this matcher before the
 * broader {@code /api/bar/**} barmaker namespace). A regular barmaker receives a
 * JSON 403; an anonymous caller receives a JSON 401.
 *
 * <p>Accounts created here are always {@code BARMAKER} and active — the request
 * carries no role or active flag. Password hashes and internal fields are never
 * exposed; only {@link UserAdminResponse} crosses this boundary.</p>
 */
@RestController
@RequestMapping("/api/bar/users")
public class BarUserController {

    private final UserAdminService userAdminService;

    public BarUserController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    /** Lists every staff account (barmakers and managers), deterministically ordered. */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<UserAdminResponse> list() {
        return userAdminService.list();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserAdminResponse> create(@Valid @RequestBody CreateBarmakerRequest request,
                                                    UriComponentsBuilder uriBuilder) {
        UserAdminResponse created = userAdminService.createBarmaker(request);
        URI location = uriBuilder.path("/api/bar/users/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }
}
