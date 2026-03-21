package com.starklabs.moneytracker.ui.wallets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ArrowBack
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
import com.starklabs.moneytracker.ui.components.StarkButton
import com.starklabs.moneytracker.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    var colorHex by remember { mutableStateOf("#0A84FF") }
    
    val accountTypes = listOf("BANK", "CREDIT_CARD", "CASH", "UPI")

    Scaffold(
        containerColor = StarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("New Account", style = StarkTypography.titleLarge, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Sharp.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StarkBackground)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            val textFieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentSecondary,
                unfocusedBorderColor = StarkBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = StarkSurface,
                unfocusedContainerColor = StarkSurface
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Account Name (e.g. HDFC)", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = balance,
                onValueChange = { balance = it },
                label = { Text("Initial Balance", color = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Type Selection
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                accountTypes.forEach { t ->
                     FilterChip(
                         selected = type == t,
                         onClick = { type = t },
                         label = { Text(t, color = if(type==t) StarkBlack else TextPrimary) },
                         colors = FilterChipDefaults.filterChipColors(
                             selectedContainerColor = AccentPrimary,
                             containerColor = StarkSurfaceVariant,
                             labelColor = TextPrimary
                         )
                     )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = last4,
                onValueChange = { if (it.length <= 4) last4 = it },
                label = { Text("Last 4 Digits (for SMS tracking)", color = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            StarkButton(
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
                                     "BANK" -> "#FFD60A"
                                     "CREDIT_CARD" -> "#FF453A"
                                     "CASH" -> "#32D74B"
                                     else -> "#0A84FF"
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
