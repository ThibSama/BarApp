package com.lebarapp.enums;

/**
 * Preparation lifecycle of a single order item. The values are declared in the
 * exact order a barmaker progresses through them, so the sequential transition
 * is simply "the next declared value": ingredients -> assembly -> dressing ->
 * completed. {@link #COMPLETED} is terminal.
 */
public enum PreparationStatus {
    PREPARATION_INGREDIENTS,
    ASSEMBLY,
    DRESSING,
    COMPLETED;

    /** Whether this is the terminal state (the drink is fully prepared). */
    public boolean isCompleted() {
        return this == COMPLETED;
    }

    /**
     * The single next preparation step in the sequence.
     *
     * @throws IllegalStateException if invoked on the terminal {@link #COMPLETED}
     *         state (callers must guard with {@link #isCompleted()} and surface a
     *         controlled business error instead of reaching this).
     */
    public PreparationStatus next() {
        if (isCompleted()) {
            throw new IllegalStateException("COMPLETED is terminal and has no next step");
        }
        return values()[ordinal() + 1];
    }
}
