package com.starklabs.moneytracker.ui.wallets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ArrowBack
import androidx.compose.material.icons.sharp.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.starklabs.moneytracker.data.Account
import com.starklabs.moneytracker.data.MoneyRepository
import com.starklabs.moneytracker.ui.components.NeonButton
import com.starklabs.moneytracker.ui.components.NeonTextField
import com.starklabs.moneytracker.ui.components.NeonText
import com.starklabs.moneytracker.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AddAccountScreen(
    navController: NavController,
    repository: MoneyRepository
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("BANK") }
    var last4 by remember { mutableStateOf("") }
    var colorHex by remember { mutableStateOf("#00B0FF") }
    
    val accountTypes = listOf("BANK", "CREDIT_CARD", "CASH", "UPI")

    Box(modifier = Modifier.fillMaxSize().background(StarkBlack)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Sharp.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                }
                NeonText("NEW ACCOUNT", style = MaterialTheme.typography.headlineMedium)
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            NeonTextField(
                value = name,
                onValueChange = { name = it },
                label = "Account Name (e.g. HDFC)",
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            NeonTextField(
                value = balance,
                onValueChange = { balance = it },
                label = "Initial Balance",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Type Selection (Simple Row for now)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                accountTypes.forEach { t ->
                     FilterChip(
                         selected = type == t,
                         onClick = { type = t },
                         label = { Text(t, color = if(type==t) StarkBlack else NeonCyan) },
                         colors = FilterChipDefaults.filterChipColors(
                             selectedContainerColor = NeonCyan,
                             containerColor = Color.Transparent,
                             labelColor = NeonCyan
                         )
                     )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            NeonTextField(
                value = last4,
                onValueChange = { if (it.length <= 4) last4 = it },
                label = "Last 4 Digits (for SMS)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            NeonButton(
                text = "INITIALIZE ACCOUNT",
                onClick = {
                    if (name.isNotEmpty() && balance.isNotEmpty()) {
                         scope.launch {
                             val account = Account(
                                 name = name,
                                 type = type,
                                 balance = balance.toDoubleOrNull() ?: 0.0,
                                 maskedNumber = if (last4.isNotBlank()) last4 else null,
                                 colorHex = when(type) {
                                     "BANK" -> "#FFD700" // Gold
                                     "CREDIT_CARD" -> "#B3001B" // Red
                                     "CASH" -> "#00B0FF" // Cyan
                                     else -> "#00E6FF"
                                 }
                             )
                             repository.addAccount(account)
                             navController.popBackStack()
                         }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
