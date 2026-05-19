package com.example.potholereport

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.potholereport.data.AuthAccountRepository
import com.example.potholereport.data.RecentReportsRepository
import com.example.potholereport.data.UserProfileRepository
import com.example.potholereport.navigation.AuthNavHost
import com.example.potholereport.ui.theme.PotholeReportTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AuthAccountRepository.init(applicationContext)
        RecentReportsRepository.init(applicationContext)
        UserProfileRepository.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            PotholeReportTheme {
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                ) { innerPadding ->
                    AuthNavHost(
                        navController = navController,
                        snackbarHostState = snackbarHostState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    )
                }
            }
        }
    }
}
