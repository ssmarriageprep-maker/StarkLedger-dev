package com.starklabs.moneytracker.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.starklabs.moneytracker.data.Category
import com.starklabs.moneytracker.data.MoneyRepository
import com.starklabs.moneytracker.data.Transaction
import com.starklabs.moneytracker.ui.components.StarkButton
import com.starklabs.moneytracker.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    navController: NavController,
    repository: MoneyRepository
) {
    val scope = rememberCoroutineScope()
    var amountStr by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("DEBIT") } // DEBIT, CREDIT
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    
    val categories = remember {
        listOf(
            Category(id = 1, name = "Food", iconName = "restaurant", colorHex = "#FFD700"),
            Category(id = 2, name = "Rent", iconName = "home", colorHex = "#00B0FF"),
            Category(id = 3, name = "Travel", iconName = "commute", colorHex = "#FF6D00"),
            Category(id = 4, name = "Shopping", iconName = "shopping_bag", colorHex = "#B3001B"),
            Category(id = 5, name = "Bills", iconName = "receipt", colorHex = "#00E6FF")
        )
    }

    Scaffold(
        containerColor = StarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("New Transaction", style = StarkTypography.titleLarge, color = TextPrimary) },
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
            Spacer(modifier = Modifier.weight(1f))

            // Amount Display
            Text(
                text = "₹${if (amountStr.isEmpty()) "0" else amountStr}",
                style = StarkTypography.displayLarge.copy(fontSize = 56.sp),
                color = if (selectedType == "DEBIT") TextPrimary else IncomeGreen,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Type Toggle
            Row(
                modifier = Modifier
                    .background(StarkSurface, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                TypeSegmentButton("Expense", selectedType == "DEBIT") { selectedType = "DEBIT" }
                TypeSegmentButton("Income", selectedType == "CREDIT") { selectedType = "CREDIT" }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Minimalist Custom Keypad
            CleanKeypad(
                onDigit = { if (amountStr.length < 8) amountStr += it },
                onBackspace = { if (amountStr.isNotEmpty()) amountStr = amountStr.dropLast(1) },
                onDone = {
                    if (amountStr.isNotEmpty()) {
                        val amount = amountStr.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            scope.launch {
                                val defaultAccount = repository.getDefaultAccount()
                                val accountId = defaultAccount?.id ?: 1
                                val transaction = Transaction(
                                    amount = amount,
                                    merchant = if (selectedType == "DEBIT") (selectedCategory?.name ?: "Expense") else "Income",
                                    date = System.currentTimeMillis(),
                                    type = selectedType,
                                    accountId = accountId,
                                    categoryId = selectedCategory?.id
                                )
                                repository.addTransaction(transaction)
                                navController.popBackStack()
                            }
                        }
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun TypeSegmentButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(if (isSelected) StarkSurfaceVariant else Color.Transparent)
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = StarkTypography.labelLarge,
            color = if (isSelected) TextPrimary else TextSecondary
        )
    }
}

@Composable
fun CleanKeypad(onDigit: (String) -> Unit, onBackspace: () -> Unit, onDone: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf(".", "0", "⌫")
        )
        
        keys.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(2f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                when (key) {
                                    "⌫" -> onBackspace()
                                    else -> onDigit(key)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = key,
                            style = StarkTypography.headlineLarge,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
        
        // Huge Save Button
        StarkButton(
            text = "SAVE TRANSACTION",
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )
    }
}
