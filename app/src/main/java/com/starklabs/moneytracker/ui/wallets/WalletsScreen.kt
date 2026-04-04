package com.starklabs.moneytracker.ui.wallets

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.navigation.NavController
import com.starklabs.moneytracker.data.MoneyRepository
import com.starklabs.moneytracker.ui.components.*
import com.starklabs.moneytracker.ui.theme.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class WalletsViewModel(private val repository: MoneyRepository) : ViewModel() {
    val accounts = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateCategoryBudget(categoryId: Int, newLimit: Double) {
        viewModelScope.launch {
            val category = categories.value.find { it.id == categoryId }
            category?.let {
                repository.updateCategory(it.copy(budgetLimit = newLimit))
            }
        }
    }

    fun updateAccount(account: com.starklabs.moneytracker.data.Account) {
        viewModelScope.launch {
            repository.updateAccount(account)
        }
    }

    fun mergeAccounts(sourceId: Int, targetId: Int) {
        viewModelScope.launch {
            repository.mergeAccounts(sourceId, targetId)
        }
    }
}

class WalletsViewModelFactory(private val repository: MoneyRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WalletsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WalletsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletsScreen(
    navController: NavController,
    viewModel: WalletsViewModel
) {
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var showAdjustBudgetDialog by remember { mutableStateOf(false) }
    var categoryToAdjust by remember { mutableStateOf<com.starklabs.moneytracker.data.Category?>(null) }
    var optimizationApplied by remember { mutableStateOf(false) }
    var isOptimizing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    if (showAdjustBudgetDialog && categoryToAdjust != null) {
        var newLimit by remember { mutableStateOf(categoryToAdjust!!.budgetLimit.toString()) }
        AlertDialog(
            onDismissRequest = { showAdjustBudgetDialog = false },
            title = { Text("Adjust Budget: ${categoryToAdjust!!.name}", color = Primary) },
            text = {
                Column {
                    Text("Enter new monthly limit for this category:", color = OnSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newLimit,
                        onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) newLimit = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Limit (₹)") },
                        textStyle = StarkTypography.bodyLarge.copy(color = OnSurface),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = OutlineVariant,
                            cursorColor = Primary
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val limitValue = newLimit.toDoubleOrNull() ?: categoryToAdjust!!.budgetLimit
                        viewModel.updateCategoryBudget(categoryToAdjust!!.id, limitValue)
                        showAdjustBudgetDialog = false
                    }
                ) {
                    Text("UPDATE", color = Primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdjustBudgetDialog = false }) {
                    Text("CANCEL", color = OnSurfaceVariant)
                }
            },
            containerColor = SurfaceContainer
        )
    }

    Scaffold(
        containerColor = SurfaceContainerLowest,
        topBar = {
            StarkHeader(
                title = "StarkLedger",
                onSettingsClick = { navController.navigate(com.starklabs.moneytracker.ui.Screen.Settings.route) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        var showMergeDialog by remember { mutableStateOf(false) }
        var mergeSourceAccount by remember { mutableStateOf<com.starklabs.moneytracker.data.Account?>(null) }
        var mergeTargetAccount by remember { mutableStateOf<com.starklabs.moneytracker.data.Account?>(null) }

        if (showMergeDialog && mergeSourceAccount != null) {
            AlertDialog(
                onDismissRequest = { showMergeDialog = false },
                title = { Text("Merge Account: ${mergeSourceAccount!!.name}", color = Primary) },
                text = {
                    Column {
                        Text("Select target account to merge into. All transactions will be reassigned.", color = OnSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        accounts.filter { it.id != mergeSourceAccount!!.id }.forEach { acc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { mergeTargetAccount = acc }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = mergeTargetAccount?.id == acc.id, onClick = { mergeTargetAccount = acc })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(acc.name, color = OnSurface)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            mergeTargetAccount?.let {
                                viewModel.mergeAccounts(mergeSourceAccount!!.id, it.id)
                                showMergeDialog = false
                            }
                        },
                        enabled = mergeTargetAccount != null
                    ) {
                        Text("MERGE", color = Primary, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showMergeDialog = false }) {
                        Text("CANCEL", color = OnSurfaceVariant)
                    }
                },
                containerColor = SurfaceContainer
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Header Section: Total Budget Overview
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(800)) + slideInVertically(tween(800), initialOffsetY = { it / 2 })
            ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Text("CURRENT PERIOD STRATEGY", style = StarkTypography.labelSmall.copy(letterSpacing = 2.sp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Budget Analysis",
                            style = StarkTypography.headlineLarge.copy(fontSize = 40.sp, fontWeight = FontWeight.Bold),
                            color = Primary,
                            lineHeight = 44.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("₹12,450.00", style = StarkTypography.headlineMedium.copy(fontSize = 28.sp, fontWeight = FontWeight.Medium), color = OnSurface)
                        Text("TOTAL ALLOCATED", style = StarkTypography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Global Progress Bar
                Surface(
                    color = SurfaceContainerLow,
                    shape = RoundedCornerShape(100.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .background(Brush.horizontalGradient(listOf(PrimaryContainer.copy(alpha = 0.05f), Color.Transparent)))
                        )
                        Column(modifier = Modifier.padding(horizontal = 32.dp, vertical = 20.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Monthly Utilization", style = StarkTypography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = OnSurfaceVariant)
                                Text("68%", style = StarkTypography.headlineMedium.copy(color = PrimaryContainer, fontWeight = FontWeight.Bold))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape).background(SurfaceContainerHighest)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.68f)
                                        .fillMaxHeight()
                                        .background(Brush.horizontalGradient(listOf(PrimaryFixedDim, PrimaryContainer)))
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("₹8,466 SPENT", style = StarkTypography.labelSmall.copy(fontSize = 10.sp, color = OnSurfaceVariant.copy(alpha = 0.6f)))
                                Text("₹3,984 REMAINING", style = StarkTypography.labelSmall.copy(fontSize = 10.sp, color = OnSurfaceVariant.copy(alpha = 0.6f)))
                            }
                        }
                    }
                }
            }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Accounts Management Section
            Text("FINANCIAL SOURCES", style = StarkTypography.labelSmall.copy(letterSpacing = 2.sp))
            Spacer(modifier = Modifier.height(16.dp))
            accounts.forEach { account ->
                StarkClickableCard(
                    onClick = { navController.navigate(com.starklabs.moneytracker.ui.Screen.AccountDetail.createRoute(account.id)) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(SurfaceContainerHighest),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when(account.type) {
                                        "BANK" -> Icons.Sharp.AccountBalance
                                        "CARD" -> Icons.Sharp.CreditCard
                                        "WALLET" -> Icons.Sharp.AccountBalanceWallet
                                        else -> Icons.Sharp.Payments
                                    },
                                    contentDescription = null,
                                    tint = Primary
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(account.name, style = StarkTypography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                Text(if (account.isActive) "Active" else "Hidden", style = StarkTypography.labelSmall, color = if (account.isActive) TertiaryContainer else OnSurfaceVariant)
                            }
                        }
                        Row {
                            IconButton(onClick = {
                                mergeSourceAccount = account
                                mergeTargetAccount = null
                                showMergeDialog = true
                            }) {
                                Icon(Icons.Sharp.Merge, contentDescription = "Merge", tint = OnSurfaceVariant)
                            }
                            IconButton(onClick = {
                                viewModel.updateAccount(account.copy(isActive = !account.isActive))
                            }) {
                                Icon(if (account.isActive) Icons.Sharp.Visibility else Icons.Sharp.VisibilityOff, contentDescription = "Toggle Visibility", tint = OnSurfaceVariant)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            StarkButton(
                text = "ADD ACCOUNT",
                onClick = { navController.navigate(com.starklabs.moneytracker.ui.Screen.AddAccount.route) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Warning Indicator Banner
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1000)) + slideInVertically(tween(1000), initialOffsetY = { it / 2 })
            ) {
            Surface(
                color = Color(0x33201F1F),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 4.dp, color = SecondaryContainer, shape = RoundedCornerShape(topStart = 12.dp, topEnd = 0.dp, bottomStart = 12.dp, bottomEnd = 0.dp))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(color = SecondaryContainer.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp), modifier = Modifier.size(40.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Sharp.Warning, contentDescription = null, tint = SecondaryContainer, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Action Required", style = StarkTypography.headlineMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = OnSurface)
                        Text(
                            text = "Food budget will exceed in 3 days based on current spending velocity.",
                            style = StarkTypography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                            // Find Food category for this mock warning
                            categoryToAdjust = categories.find { it.name.contains("Food", ignoreCase = true) }
                            if (categoryToAdjust != null) showAdjustBudgetDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerHighest),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("ADJUST", style = StarkTypography.labelSmall.copy(color = OnSurface, fontWeight = FontWeight.Bold))
                    }
                }
            }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Category Grid
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1200)) + slideInVertically(tween(1200), initialOffsetY = { it / 2 })
            ) {
            val mockCategories = listOf(
                Pair("Food & Dining", Pair(SecondaryContainer, 0.92f)),
                Pair("Transport", Pair(PrimaryContainer, 0.45f)),
                Pair("Leisure", Pair(TertiaryContainer, 0.12f)),
                Pair("Utilities", Pair(Outline, 0.60f)),
                Pair("Shopping", Pair(PrimaryContainer, 0.38f)),
                Pair("Wellness", Pair(TertiaryContainer, 0.75f))
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                val rows = mockCategories.chunked(3)
                rows.forEach { rowItems ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        rowItems.forEachIndexed { index, (name, props) ->
                            val (color, progress) = props
                            StarkCard(
                                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                                cornerRadius = 16.dp,
                                contentPadding = PaddingValues(12.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp), modifier = Modifier.size(32.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            CategoryIcon(name, tint = color, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Text("${(progress * 100).toInt()}%", style = StarkTypography.headlineSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold), color = if (progress >= 0.9f) Error else color)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = name,
                                    style = StarkTypography.headlineMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                                    color = OnSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (progress >= 0.9f) "Critical" else if (progress >= 0.7f) "Projected" else "Healthy",
                                    style = StarkTypography.labelLarge.copy(fontSize = 9.sp),
                                    color = OnSurfaceVariant,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(SurfaceContainerHighest)) {
                                    Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(color))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("₹${String.format("%.0f", 1200 * progress)}", style = StarkTypography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold))
                                    Text("of ₹1,200", style = StarkTypography.labelSmall.copy(fontSize = 8.sp, color = OnSurfaceVariant))
                                }
                            }
                            if (index < rowItems.size - 1) {
                                Spacer(modifier = Modifier.width(24.dp))
                            }
                        }
                        // Handle incomplete row
                        if (rowItems.size < 3) {
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.width(24.dp))
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // AI Strategy Insight Card
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1400)) + slideInVertically(tween(1400), initialOffsetY = { it / 2 })
            ) {
            Surface(
                color = Color(0x33201F1F),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 2.dp, color = SecondaryContainer, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 0.dp, bottomEnd = 0.dp))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Surface(
                        color = SurfaceContainerHigh,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.background(Brush.linearGradient(listOf(Color(0xFF001F24), Color(0xFF00363D))))) {
                            Icon(Icons.Sharp.AutoAwesome, contentDescription = null, tint = SecondaryContainer.copy(alpha = 0.2f), modifier = Modifier.size(100.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Sharp.AutoAwesome, contentDescription = null, tint = SecondaryContainer, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI STRATEGY INSIGHT", style = StarkTypography.headlineSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp), color = SecondaryContainer)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Optimize Subscription Recurrence", style = StarkTypography.headlineLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold), color = OnSurface)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Our engine detected 4 overlapping streaming services. Consolidating these to a single family plan could reclaim ₹42.00/month, effectively covering your transport overages.",
                            style = StarkTypography.bodyMedium,
                            color = OnSurfaceVariant,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                if (!optimizationApplied) {
                                    isOptimizing = true
                                    scope.launch {
                                        kotlinx.coroutines.delay(2000L) // Artificial pulse for UX
                                        // "Apply" by finding Transport category and adjusting it for the user
                                        val transport = categories.find { it.name.contains("Transport", ignoreCase = true) }
                                        transport?.let {
                                            viewModel.updateCategoryBudget(it.id, it.budgetLimit + 42.0)
                                        }
                                        isOptimizing = false
                                        optimizationApplied = true
                                        snackbarHostState.showSnackbar("Optimization Applied: Transport budget optimized by ₹42.00")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (optimizationApplied) TertiaryContainer else PrimaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            enabled = !isOptimizing
                        ) {
                            if (isOptimizing) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = OnPrimary, strokeWidth = 2.dp)
                            } else {
                                Text(
                                    text = if (optimizationApplied) "Optimization Applied" else "Apply Optimization",
                                    style = StarkTypography.labelLarge.copy(color = OnPrimary, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
