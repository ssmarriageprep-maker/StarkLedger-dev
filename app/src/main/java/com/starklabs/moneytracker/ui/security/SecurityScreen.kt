package com.starklabs.moneytracker.ui.security

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.starklabs.moneytracker.ui.Screen
import com.starklabs.moneytracker.ui.theme.*

@Composable
fun SecurityScreen(navController: NavController, viewModel: SecurityViewModel) {
    val isPinSet by viewModel.isPinSet.collectAsState()
    val scope = rememberCoroutineScope()
    
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

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        if (isPinSet == null) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = PrimaryContainer
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
                    color = if (error) Error else OnSurface,
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
                                    if (pin.length > i) PrimaryContainer else SurfaceVariant,
                                    androidx.compose.foundation.shape.CircleShape
                                )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(64.dp))

                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    SecurityKeypad(
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
                                        scope.launch {
                                            if (viewModel.verifyPin(pin)) {
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
                            }
                        },
                        onBackspace = {
                            if (pin.isNotEmpty()) pin = pin.dropLast(1)
                            error = false
                        }
                    )
                }
            }
            
            Text(
                text = "Your data stays on device",
                style = StarkTypography.labelSmall,
                color = OnSurfaceVariant,
                modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp)
            )
        }
    }
}

@Composable
fun SecurityKeypad(onDigit: (String) -> Unit, onBackspace: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "backspace")
        )

        keys.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = key.isNotEmpty()) {
                                when (key) {
                                    "backspace" -> onBackspace()
                                    else -> if (key.isNotEmpty()) onDigit(key)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (key == "backspace") {
                            Icon(Icons.Sharp.Backspace, contentDescription = null, tint = OnSurface)
                        } else if (key.isNotEmpty()) {
                            Text(
                                text = key,
                                style = StarkTypography.headlineLarge.copy(fontWeight = FontWeight.Medium),
                                color = OnSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
