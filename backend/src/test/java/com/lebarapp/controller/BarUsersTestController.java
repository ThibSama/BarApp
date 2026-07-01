package com.lebarapp.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Test-only controller used to verify that {@code /api/bar/users/**} requires
 * {@code ROLE_MANAGER} while the broader {@code /api/bar/**} namespace accepts
 * any staff role. This controller exists ONLY in test sources and is never
 * packaged into the production JAR.
 */
@RestController
@RequestMapping("/api/bar/users/test")
public class BarUsersTestController {

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> ping() {
        return Map.of("status", "ok");
    }
}
