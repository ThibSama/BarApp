package com.lebarapp.enums;

/**
 * Preparation lifecycle of a single order item. The preparation workflow is out
 * of scope for this stage; the enum is provided for schema-coherent JPA mapping.
 */
public enum PreparationStatus {
    PREPARATION_INGREDIENTS,
    ASSEMBLY,
    DRESSING,
    COMPLETED
}
