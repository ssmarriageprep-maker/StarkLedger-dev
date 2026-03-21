package com.starklabs.moneytracker.ui.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.starklabs.moneytracker.ui.Screen
import com.starklabs.moneytracker.ui.add.CleanKeypad
import com.starklabs.moneytracker.ui.theme.*

@Composable
fun SecurityScreen(navController: NavController, viewModel: SecurityViewModel) {
    val isPinSet by viewModel.isPinSet.collectAsState()
    val storedPin by viewModel.storedPin.collectAsState()
    
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    
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
                         // Fallback to PIN
                     }
                 )
             }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(StarkBackground)) {
        if (isPinSet == null) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = AccentPrimary
            )
        } else {
            val isSetupMode = isPinSet == false
            
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = if (error) "INCORRECT PIN" else if (isSetupMode) "CREATE PASSCODE" else "ENTER PASSCODE",
                    color = if (error) ExpenseRed else TextPrimary,
                    style = StarkTypography.headlineMedium
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Circles for PIN
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    repeat(4) { i ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    if (pin.length > i) AccentPrimary else StarkSurfaceVariant,
                                    androidx.compose.foundation.shape.CircleShape
                                )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(64.dp))

                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    CleanKeypad(
                        onDigit = { 
                            if (pin.length < 4) {
                                pin += it
                                if (pin.length == 4) {
                                    if (isSetupMode) {
                                        viewModel.savePin(pin)
                                        navController.navigate(Screen.Dashboard.route) {
                                            popUpTo(Screen.Security.route) { inclusive = true }
                                        }
                                    } else {
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
            
            Text(
                text = "Your data stays on device",
                style = StarkTypography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp)
            )
        }
    }
}
