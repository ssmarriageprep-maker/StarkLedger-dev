package com.starklabs.moneytracker.ui.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.starklabs.moneytracker.ui.Screen
import com.starklabs.moneytracker.ui.add.Keypad
import com.starklabs.moneytracker.ui.components.ArcReactor
import com.starklabs.moneytracker.ui.components.NeonText
import com.starklabs.moneytracker.ui.theme.*

@Composable
fun SecurityScreen(navController: NavController) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(StarkBlack)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ArcReactor(percentage = 1f, modifier = Modifier.size(100.dp), color = if (error) MetallicRed else NeonCyan)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            NeonText(
                text = if (error) "ACCESS DENIED" else "ENTER PASSCODE",
                color = if (error) MetallicRed else NeonCyan,
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Circles for PIN
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(4) { i ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                if (pin.length > i) NeonCyan else StarkSurface,
                                androidx.compose.foundation.shape.CircleShape
                            )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            Keypad(
                onDigit = { 
                    if (pin.length < 4) {
                        pin += it
                        if (pin.length == 4) {
                            if (pin == "1234") { // Mock PIN
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Security.route) { inclusive = true }
                                }
                            } else {
                                error = true
                                pin = ""
                            }
                        }
                    }
                },
                onBackspace = {
                    if (pin.isNotEmpty()) pin = pin.dropLast(1)
                    error = false
                },
                onDone = {} // Auto submits
            )
        }
    }
}
