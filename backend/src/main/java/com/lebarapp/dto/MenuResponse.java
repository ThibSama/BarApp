package com.lebarapp.dto;

import java.util.List;

/**
 * Root payload of {@code GET /api/menu}. An empty catalog yields an empty
 * {@code categories} list (never {@code null}).
 */
public record MenuResponse(List<MenuCategoryDto> categories) {
}
