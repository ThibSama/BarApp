package com.lebarapp.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Test-only controller used to verify that {@code /api/bar/**} requires
 * {@code ROLE_BARMAKER}. This controller exists ONLY in test sources and is
 * never packaged into the production JAR.
 */
@RestController
@RequestMapping("/api/bar/test")
public class BarTestController {

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> ping() {
        return Map.of("status", "ok");
    }
}
