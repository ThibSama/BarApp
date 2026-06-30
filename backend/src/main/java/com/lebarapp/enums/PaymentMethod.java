package com.lebarapp.enums;

/**
 * Payment methods a client can select for an order. Persisted as the enum name
 * and constrained by a database CHECK constraint
 * ({@code ck_customer_order_payment_method}). These mirror the options offered
 * by the client payment selector in the frontend.
 */
public enum PaymentMethod {
    CASH_AT_COUNTER,
    CARD_AT_COUNTER,
    CARD_IN_APP,
    APPLE_PAY,
    GOOGLE_PAY
}
