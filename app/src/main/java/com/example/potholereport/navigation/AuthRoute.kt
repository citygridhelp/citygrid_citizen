package com.example.potholereport.navigation

import android.net.Uri

sealed class AuthRoute(val route: String) {
    data object Home : AuthRoute("home")
    data object Emergency : AuthRoute("emergency")
    data object Login : AuthRoute("login")
    data object Signup : AuthRoute("signup")
    data object ForgotPassword : AuthRoute("forgot_password")
    data object NewReport : AuthRoute("new_report/{city}") {
        fun createRoute(city: String): String {
            val key = city.trim().ifBlank { "BENGALURU" }
            return "new_report/${Uri.encode(key)}"
        }
    }
}
