package com.lebarapp.enums;

/**
 * Authenticated staff roles. Clients remain anonymous; only bar staff sign in.
 *
 * <ul>
 *   <li>{@code BARMAKER} — operates the barmaker workspace (orders, catalogue).</li>
 *   <li>{@code MANAGER} — an elevated barmaker who additionally manages staff
 *       accounts. A manager keeps every barmaker capability.</li>
 * </ul>
 *
 * <p>These two values are not a Spring Security role hierarchy: route
 * authorization lists the exact roles allowed on each namespace, so a manager is
 * explicitly granted access to the barmaker routes as well as the manager-only
 * ones.</p>
 */
public enum UserRole {
    BARMAKER,
    MANAGER
}
