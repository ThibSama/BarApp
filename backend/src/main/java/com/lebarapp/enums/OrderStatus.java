package com.lebarapp.enums;

/**
 * Lifecycle of a customer order. Order creation/workflow is out of scope for
 * this stage; the enum is provided for schema-coherent JPA mapping only.
 */
public enum OrderStatus {
    ORDERED,
    IN_PROGRESS,
    COMPLETED
}
