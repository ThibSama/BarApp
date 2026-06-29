package com.lebarapp.controller;

import com.lebarapp.dto.MenuResponse;
import com.lebarapp.service.MenuService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, read-only menu endpoint. Anonymous clients consume {@code GET /api/menu}.
 */
@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public MenuResponse getMenu() {
        return menuService.getMenu();
    }
}
