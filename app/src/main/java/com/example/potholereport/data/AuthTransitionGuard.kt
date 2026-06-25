package com.example.potholereport.data

/**
 * Blocks GPS prompts, Settings intents, and heavy location re-bootstrap during sign-in UI transition.
 */
object AuthTransitionGuard {

    @Volatile
    private var activeUntilMs: Long = 0L

    fun begin(durationMs: Long = 3_000L) {
        activeUntilMs = System.currentTimeMillis() + durationMs
    }

    fun end() {
        activeUntilMs = 0L
    }

    fun isActive(): Boolean = System.currentTimeMillis() < activeUntilMs
}
