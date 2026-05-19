package com.example.potholereport.navigation

sealed class AuthRoute(val route: String) {
    data object Home : AuthRoute("home")
    data object Emergency : AuthRoute("emergency")
    data object Login : AuthRoute("login")
    data object Signup : AuthRoute("signup")
    data object ForgotPassword : AuthRoute("forgot_password")
    data object NewReport : AuthRoute("new_report/{city}") {
        fun createRoute(city: String): String = "new_report/$city"
    }
}
