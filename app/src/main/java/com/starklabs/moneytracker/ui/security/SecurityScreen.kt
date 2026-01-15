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
import com.starklabs.moneytracker.ui.theme.NeonCyan
import com.starklabs.moneytracker.ui.theme.MetallicRed
import com.starklabs.moneytracker.ui.theme.StarkBlack
import com.starklabs.moneytracker.ui.theme.StarkSurface

@Composable
fun SecurityScreen(navController: NavController, viewModel: SecurityViewModel) {
    val isPinSet by viewModel.isPinSet.collectAsState()
    val storedPin by viewModel.storedPin.collectAsState()
    
    // Local state for input
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Trigger biometric if available and not setup mode
    LaunchedEffect(isPinSet) {
        pin = ""
        error = false
        
        if (isPinSet == true) {
             val activity = context as? androidx.fragment.app.FragmentActivity
             if (activity != null && BiometricHelper.isBiometricAvailable(context)) {
                 BiometricHelper.authenticate(activity, 
                     onSuccess = {
                         navController.navigate(Screen.Dashboard.route) {
                             popUpTo(Screen.Security.route) { inclusive = true }
                         }
                     },
                     onError = {
                         // Fallback to PIN, maybe show toast
                     }
                 )
             }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(StarkBlack)) {
        if (isPinSet == null) {
            // Loading state
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = NeonCyan
            )
        } else {
            val isSetupMode = isPinSet == false
            
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // ... (Rest of UI similar, maybe add biometric icon if available)
                ArcReactor(percentage = 1f, modifier = Modifier.size(100.dp), color = if (error) MetallicRed else NeonCyan)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                NeonText(
                    text = if (error) "ACCESS DENIED" else if (isSetupMode) "CREATE PASSCODE" else "ENTER PASSCODE",
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
                                if (isSetupMode) {
                                    // In setup mode, save the PIN and navigate
                                    viewModel.savePin(pin)
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(Screen.Security.route) { inclusive = true }
                                    }
                                } else {
                                    // In login mode, verify against stored PIN
                                    if (pin == storedPin) {
                                        navController.navigate(Screen.Dashboard.route) {
                                            popUpTo(Screen.Security.route) { inclusive = true }
                                        }
                                    } else {
                                        error = true
                                        pin = ""
                                    }
                                }
                            }
                        }
                    },
                    onBackspace = {
                        if (pin.isNotEmpty()) pin = pin.dropLast(1)
                        error = false
                    },
                    onDone = {} 
                )
            }
        }
    }
}
