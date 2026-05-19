package com.example.potholereport.ml

sealed class PhotoValidationResult {
    data class Accepted(
        val confidence: Float,
        val summary: String,
    ) : PhotoValidationResult()

    data class Rejected(
        val title: String,
        val message: String,
    ) : PhotoValidationResult()
}
