package com.lebarapp.exception;

import org.springframework.http.HttpStatus;

/**
 * Stable, machine-readable error codes returned to API clients, each bound to an
 * HTTP status and a default French user-facing message. The code strings are
 * part of the public API contract consumed by the future Vue frontend; internal
 * Java identifiers remain in English.
 */
public enum ApiErrorCode {

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST,
            "Les données de la requête sont invalides."),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST,
            "La requête est mal formée ou illisible."),
    INVALID_IDENTIFIER(HttpStatus.BAD_REQUEST,
            "L'identifiant fourni est invalide."),
    COCKTAIL_NOT_FOUND(HttpStatus.NOT_FOUND,
            "Le cocktail demandé est introuvable."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND,
            "La commande demandée est introuvable."),
    ORDER_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND,
            "L'élément de commande demandé est introuvable."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND,
            "La ressource demandée est introuvable."),
    INVALID_PREPARATION_TRANSITION(HttpStatus.CONFLICT,
            "La transition de préparation demandée est invalide."),
    COCKTAIL_UNAVAILABLE(HttpStatus.CONFLICT,
            "Le cocktail demandé n'est pas disponible."),
    SIZE_UNAVAILABLE(HttpStatus.CONFLICT,
            "La taille demandée n'est pas disponible pour ce cocktail."),
    PRICE_UNAVAILABLE(HttpStatus.CONFLICT,
            "Le tarif demandé n'est pas disponible pour ce cocktail."),
    PUBLIC_CODE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,
            "Une erreur est survenue lors de la création de la commande. Veuillez réessayer."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED,
            "Identifiants incorrects."),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED,
            "Une authentification est requise pour accéder à cette ressource."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED,
            "Le jeton d'authentification est invalide."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED,
            "Le jeton d'authentification a expiré."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN,
            "Vous n'avez pas l'autorisation d'accéder à cette ressource."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,
            "Une erreur interne est survenue. Veuillez réessayer plus tard.");

    private final HttpStatus status;
    private final String defaultMessage;

    ApiErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
