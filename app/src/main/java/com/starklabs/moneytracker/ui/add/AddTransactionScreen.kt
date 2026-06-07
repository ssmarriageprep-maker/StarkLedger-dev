package com.starklabs.moneytracker.ui.add

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    navController: NavController,
    repository: MoneyRepository
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var amountStr by remember { mutableStateOf("0") }
    var selectedType by remember { mutableStateOf("DEBIT") } // DEBIT, CREDIT
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var note by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    
    val categories by repository.allCategories.collectAsState(initial = emptyList())

    var showDatePicker by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDate = datePickerState.selectedDateMillis ?: selectedDate
                    showDatePicker = false
                }) { Text("SELECT", color = Primary) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("CANCEL", color = OnSurfaceVariant) }
            },
            colors = DatePickerDefaults.colors(containerColor = SurfaceContainer)
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showNoteDialog) {
        var tempNote by remember { mutableStateOf(note) }
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text("Add Transaction Note", color = Primary) },
            text = {
                OutlinedTextField(
                    value = tempNote,
                    onValueChange = { tempNote = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Note") },
                    textStyle = StarkTypography.bodyLarge.copy(color = OnSurface),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = OutlineVariant,
                        cursorColor = Primary
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    note = tempNote
                    showNoteDialog = false
                }) { Text("DONE", color = Primary, fontWeight = FontWeight.Bold) }
            },
            containerColor = SurfaceContainer
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
                modifier = Modifier.fillMaxWidth().height(160.dp)
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory?.id == category.id
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) PrimaryContainer.copy(alpha = 0.1f) else SurfaceContainerLow)
                            .border(1.dp, if (isSelected) PrimaryContainer.copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedCategory = category
                            }
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
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        when (key) {
                                            "backspace" -> {
                                                if (amountStr.length > 1) {
                                                    amountStr = amountStr.dropLast(1)
                                                } else {
                                                    amountStr = "0"
                                                }
                                            }
                                            "." -> {
                                                if (!amountStr.contains(".")) {
                                                    amountStr += "."
                                                }
                                            }
                                            else -> {
                                                if (amountStr == "0") {
                                                    amountStr = key
                                                } else if (amountStr.length < 10) {
                                                    // Handle 2 decimal places limit
                                                    if (!amountStr.contains(".") || amountStr.substringAfter(".").length < 2) {
                                                        amountStr += key
                                                    }
                                                }
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
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showDatePicker = true }
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Icon(Icons.Sharp.CalendarToday, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    val sdf = java.text.SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
                    Text(sdf.format(Date(selectedDate)), style = StarkTypography.bodyLarge, color = OnSurface)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showNoteDialog = true }
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Icon(Icons.Sharp.Notes, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        if (note.isEmpty()) "Add note..." else note,
                        style = StarkTypography.bodyLarge,
                        color = if (note.isEmpty()) OnSurfaceVariant.copy(alpha = 0.4f) else OnSurface,
                        maxLines = 1
                    )
                }
            }

            // Save Button
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch {
                            val defaultAccount = repository.getDefaultAccount()
                            val accountId = defaultAccount?.id ?: 1
                            val transaction = Transaction(
                                amount = amount,
                                merchant = selectedCategory?.name ?: "Manual Expense",
                                date = selectedDate,
                                type = selectedType,
                                accountId = accountId,
                                categoryId = selectedCategory?.id,
                                notes = if (note.isEmpty()) null else note
                            )
                            repository.addTransaction(transaction)
                            navController.popBackStack()
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
