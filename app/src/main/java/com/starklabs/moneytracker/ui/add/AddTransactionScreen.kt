package com.starklabs.moneytracker.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.starklabs.moneytracker.data.Account
import com.starklabs.moneytracker.data.Category
import com.starklabs.moneytracker.data.MoneyRepository
import com.starklabs.moneytracker.data.Transaction
import com.starklabs.moneytracker.ui.components.GlassCard
import com.starklabs.moneytracker.ui.components.NeonText
import com.starklabs.moneytracker.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AddTransactionScreen(
    navController: NavController,
    repository: MoneyRepository
) {
    val scope = rememberCoroutineScope()
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("DEBIT") } // DEBIT, CREDIT
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    
    // In a real app these would come from ViewModel
    val categories = remember {
        listOf(
            Category(id = 1, name = "Food", iconName = "restaurant", colorHex = "#FFD700"),
            Category(id = 2, name = "Rent", iconName = "home", colorHex = "#00B0FF"),
            Category(id = 3, name = "Travel", iconName = "commute", colorHex = "#FF6D00"),
            Category(id = 4, name = "Shopping", iconName = "shopping_bag", colorHex = "#B3001B"),
            Category(id = 5, name = "Bills", iconName = "receipt", colorHex = "#00E6FF")
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(StarkBlack)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                }
                NeonText(text = "NEW ENTRY", style = MaterialTheme.typography.titleLarge)
            }

            // Display
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                NeonText(text = "AMOUNT", color = TextGrey)
                NeonText(
                    text = if (amountStr.isEmpty()) "0" else amountStr,
                    style = MaterialTheme.typography.displayLarge,
                    color = if (selectedType == "DEBIT") JarvisOrange else NeonCyan
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Type Toggle
                Row(
                    modifier = Modifier.background(StarkSurface, CircleShape).padding(4.dp)
                ) {
                    TypeButton("EXPENSE", selectedType == "DEBIT") { selectedType = "DEBIT" }
                    TypeButton("INCOME", selectedType == "CREDIT") { selectedType = "CREDIT" }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                NeonText(
                    text = selectedCategory?.name?.uppercase() ?: "SELECT CATEGORY", 
                    color = JarvisGold
                )
            }

            // Category Grid
            GlassCard(
                modifier = Modifier.fillMaxWidth().height(120.dp).padding(horizontal = 16.dp),
                borderColor = NeonCyanDim.copy(alpha = 0.5f)
            ) {
                 LazyVerticalGrid(columns = GridCells.Adaptive(60.dp)) {
                     items(categories) { cat ->
                         Column(
                             horizontalAlignment = Alignment.CenterHorizontally,
                             modifier = Modifier.clickable { selectedCategory = cat }
                         ) {
                             Box(
                                 modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        if (selectedCategory == cat) NeonCyan else StarkSurface,
                                        CircleShape
                                    ),
                                 contentAlignment = Alignment.Center
                             ) {
                                 // Placeholder Icon
                                 NeonText(text = cat.name.take(1), color = if (selectedCategory == cat) StarkBlack else TextWhite)
                             }
                             NeonText(text = cat.name, style = MaterialTheme.typography.labelSmall)
                         }
                     }
                 }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Keypad
            Keypad(
                onDigit = { 
                    if (amountStr.length < 9) amountStr += it 
                },
                onBackspace = {
                    if (amountStr.isNotEmpty()) amountStr = amountStr.dropLast(1)
                },
                onDone = {
                    if (amountStr.isNotEmpty()) {
                        val amount = amountStr.toDouble()
                        scope.launch {
                            // Fetch a valid account ID dynamically
                            val defaultAccount = repository.getDefaultAccount()
                            val accountId = defaultAccount?.id ?: 1 // Fallback to 1 only if absolutely nothing found (which implies DB empty)
                            
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
            )
        }
    }
}

@Composable
fun TypeButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(if (selected) NeonCyan.copy(alpha = 0.2f) else Color.Transparent, CircleShape)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        NeonText(text = text, color = if (selected) NeonCyan else TextGrey)
    }
}

@Composable
fun Keypad(onDigit: (String) -> Unit, onBackspace: () -> Unit, onDone: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(StarkSurface).padding(16.dp)) {
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf(".", "0", "DEL")
        )
        
        keys.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth().weight(1f, fill = false), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { key ->
                    TextButton(
                        onClick = {
                            when (key) {
                                "DEL" -> onBackspace()
                                else -> onDigit(key)
                            }
                        },
                        modifier = Modifier.weight(1f).height(60.dp)
                    ) {
                        NeonText(text = key, style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        }
        
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
        ) {
            Text("CONFIRM", color = StarkBlack, fontWeight = FontWeight.Bold)
        }
    }
}
