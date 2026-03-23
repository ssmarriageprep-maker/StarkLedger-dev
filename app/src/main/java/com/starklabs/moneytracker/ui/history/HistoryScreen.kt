package com.starklabs.moneytracker.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ArrowBack
import androidx.compose.material.icons.sharp.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import com.starklabs.moneytracker.ui.components.*
import com.starklabs.moneytracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var transactionToEdit by remember { mutableStateOf<com.starklabs.moneytracker.data.Transaction?>(null) }
    
    if (transactionToEdit != null) {
        AlertDialog(
            onDismissRequest = { transactionToEdit = null },
            title = { Text("Edit Category for ${transactionToEdit?.merchant}") },
            text = {
                LazyColumn {
                    items(categories) { cat ->
                        Text(
                            text = cat.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    transactionToEdit?.let {
                                        viewModel.updateTransactionCategory(it.id, cat.id, it.merchant)
                                    }
                                    transactionToEdit = null
                                }
                                .padding(16.dp),
                            color = TextPrimary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { transactionToEdit = null }) {
                    Text("Cancel", color = AccentSecondary)
                }
            },
            containerColor = StarkSurface
        )
    }

    Scaffold(
        containerColor = StarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Transactions", style = StarkTypography.titleLarge, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Sharp.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StarkBackground,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Clean Search Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search logs...", color = TextSecondary, style = StarkTypography.bodyMedium) },
                leadingIcon = { Icon(Icons.Sharp.Search, contentDescription = null, tint = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentSecondary,
                    unfocusedBorderColor = StarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentSecondary,
                    focusedContainerColor = StarkSurface,
                    unfocusedContainerColor = StarkSurface
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (state.groupedTransactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transactions found", color = TextSecondary, style = StarkTypography.bodyMedium)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    state.groupedTransactions.forEach { (month, transactions) ->
                        item {
                            MonthHeader(month)
                        }
                        items(transactions) { transaction ->
                            TransactionRow(transaction = transaction, onClick = { transactionToEdit = transaction })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthHeader(month: String) {
    Text(
        text = month.uppercase(),
        style = StarkTypography.labelLarge,
        color = TextSecondary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}
