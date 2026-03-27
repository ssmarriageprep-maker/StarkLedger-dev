package com.starklabs.moneytracker.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.starklabs.moneytracker.data.Category
import com.starklabs.moneytracker.data.MoneyRepository
import com.starklabs.moneytracker.data.Transaction
import com.starklabs.moneytracker.ui.components.*
import com.starklabs.moneytracker.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    navController: NavController,
    repository: MoneyRepository
) {
    val scope = rememberCoroutineScope()
    var amountStr by remember { mutableStateOf("142.00") }
    var selectedType by remember { mutableStateOf("DEBIT") } // DEBIT, CREDIT
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    
    val categories = remember {
        listOf(
            Category(id = 1, name = "Food", iconName = "restaurant", colorHex = "#00E6FF"),
            Category(id = 2, name = "Travel", iconName = "commute", colorHex = "#BAC9CC"),
            Category(id = 3, name = "Shop", iconName = "shopping_bag", colorHex = "#BAC9CC"),
            Category(id = 4, name = "Groc.", iconName = "local_grocery_store", colorHex = "#BAC9CC"),
            Category(id = 5, name = "Fun", iconName = "health_connect", colorHex = "#BAC9CC"),
            Category(id = 6, name = "Health", iconName = "health_and_safety", colorHex = "#BAC9CC"),
            Category(id = 7, name = "Bills", iconName = "home", colorHex = "#BAC9CC"),
            Category(id = 8, name = "Other", iconName = "more_horiz", colorHex = "#BAC9CC")
        )
    }

    Scaffold(
        containerColor = SurfaceContainerLowest,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(SurfaceContainerLowest)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Sharp.Close, contentDescription = "Close", tint = PrimaryContainer)
                }
                Text(
                    text = "Quick Expense",
                    style = StarkTypography.headlineLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
                    color = PrimaryContainer
                )
                Spacer(modifier = Modifier.width(48.dp)) // Spacer for centering
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Amount Input Section
            Column(
                modifier = Modifier.padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ENTER AMOUNT",
                    style = StarkTypography.labelSmall,
                    color = OnSurfaceVariant,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "₹",
                        style = StarkTypography.headlineLarge.copy(fontSize = 32.sp, fontWeight = FontWeight.Light),
                        color = PrimaryContainer,
                        modifier = Modifier.padding(bottom = 8.dp, end = 4.dp)
                    )
                    Text(
                        text = amountStr,
                        style = StarkTypography.displayLarge.copy(fontSize = 64.sp, fontWeight = FontWeight.Bold),
                        color = OnSurface,
                        letterSpacing = (-2).sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.size(width = 48.dp, height = 4.dp).clip(CircleShape).background(PrimaryContainer.copy(alpha = 0.5f)))
            }

            // Category Selector Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().height(200.dp)
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory?.id == category.id
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) PrimaryContainer.copy(alpha = 0.1f) else SurfaceContainerLow)
                            .border(1.dp, if (isSelected) PrimaryContainer.copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable { selectedCategory = category }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CategoryIcon(category.name, tint = if (isSelected) PrimaryContainer else OnSurfaceVariant, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = category.name.uppercase(),
                            style = StarkTypography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                            color = if (isSelected) PrimaryContainer else OnSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Numeric Keypad
            Column(modifier = Modifier.weight(1f)) {
                val keypadRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf(".", "0", "backspace")
                )

                keypadRows.forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.forEach { key ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        when (key) {
                                            "backspace" -> if (amountStr.isNotEmpty()) amountStr = amountStr.dropLast(1)
                                            else -> {
                                                if (amountStr == "142.00") amountStr = "" // Clear initial placeholder
                                                if (amountStr.length < 10) amountStr += key
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (key == "backspace") {
                                    Icon(Icons.Sharp.Backspace, contentDescription = null, tint = OnSurface)
                                } else {
                                    Text(
                                        text = key,
                                        style = StarkTypography.headlineMedium.copy(fontWeight = FontWeight.Medium),
                                        color = OnSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Secondary Fields
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Icon(Icons.Sharp.CalendarToday, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Today, 14 Mar", style = StarkTypography.bodyLarge, color = OnSurface)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Icon(Icons.Sharp.Notes, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Add note...", style = StarkTypography.bodyLarge, color = OnSurfaceVariant.copy(alpha = 0.4f))
                }
            }

            // Save Button
            Button(
                onClick = {
                    if (amountStr.isNotEmpty()) {
                        val amount = amountStr.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            scope.launch {
                                val defaultAccount = repository.getDefaultAccount()
                                val accountId = defaultAccount?.id ?: 1
                                val transaction = Transaction(
                                    amount = amount,
                                    merchant = selectedCategory?.name ?: "Expense",
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
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(bottom = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Sharp.CheckCircle, contentDescription = null, tint = OnPrimary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("SAVE EXPENSE", style = StarkTypography.headlineSmall.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = OnPrimary)
            }
        }
    }
}
