package com.starklabs.moneytracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starklabs.moneytracker.data.Account
import com.starklabs.moneytracker.data.Category
import com.starklabs.moneytracker.domain.*
import com.starklabs.moneytracker.ui.theme.*

/**
 * Reusable filter bottom sheet shared by History and Analytics. Edits a local
 * draft of [TransactionFilter] and only commits via [onApply], so dragging the
 * amount fields or typing a merchant query doesn't recompute the underlying
 * lists on every keystroke.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFilterSheet(
    filter: TransactionFilter,
    categories: List<Category>,
    accounts: List<Account>,
    onApply: (TransactionFilter) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember(filter) { mutableStateOf(filter) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainer,
        dragHandle = { BottomSheetDefaults.DragHandle(color = OutlineVariant) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            Text("FILTER TRANSACTIONS", style = StarkTypography.labelSmall.copy(letterSpacing = 2.sp, color = PrimaryContainer))
            Spacer(modifier = Modifier.height(20.dp))

            FilterSection(title = "Date Range") {
                val presets = remember {
                    listOf(
                        DateRangePresets.last7Days(),
                        DateRangePresets.last30Days(),
                        DateRangePresets.thisMonth(),
                        DateRangePresets.thisYear()
                    )
                }
                ChipRow {
                    SelectableChip(
                        label = "All Time",
                        selected = draft.dateRange == null,
                        onClick = { draft = draft.copy(dateRange = null) }
                    )
                    presets.forEach { preset ->
                        SelectableChip(
                            label = preset.label,
                            selected = draft.dateRange?.label == preset.label,
                            onClick = { draft = draft.copy(dateRange = preset) }
                        )
                    }
                }
            }

            FilterSection(title = "Transaction Type") {
                ChipRow {
                    TransactionTypeFilter.entries.forEach { type ->
                        SelectableChip(
                            label = when (type) {
                                TransactionTypeFilter.ALL -> "All"
                                TransactionTypeFilter.INCOME -> "Income"
                                TransactionTypeFilter.EXPENSE -> "Expense"
                            },
                            selected = draft.transactionType == type,
                            onClick = { draft = draft.copy(transactionType = type) }
                        )
                    }
                }
            }

            FilterSection(title = "Category") {
                ChipRow {
                    SelectableChip(
                        label = "Any",
                        selected = draft.categoryId == null,
                        onClick = { draft = draft.copy(categoryId = null) }
                    )
                    categories.forEach { category ->
                        SelectableChip(
                            label = category.name,
                            selected = draft.categoryId == category.id,
                            onClick = { draft = draft.copy(categoryId = category.id) }
                        )
                    }
                }
            }

            FilterSection(title = "Account") {
                ChipRow {
                    SelectableChip(
                        label = "Any",
                        selected = draft.accountId == null,
                        onClick = { draft = draft.copy(accountId = null) }
                    )
                    accounts.forEach { account ->
                        SelectableChip(
                            label = account.name,
                            selected = draft.accountId == account.id,
                            onClick = { draft = draft.copy(accountId = account.id) }
                        )
                    }
                }
            }

            FilterSection(title = "Merchant") {
                FilterTextField(
                    value = draft.merchantQuery,
                    onValueChange = { draft = draft.copy(merchantQuery = it) },
                    placeholder = "e.g. Amazon, Swiggy",
                    keyboardType = KeyboardType.Text,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            FilterSection(title = "Amount Range") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FilterTextField(
                        value = draft.minAmount?.let { formatAmountInput(it) } ?: "",
                        onValueChange = { draft = draft.copy(minAmount = it.toDoubleOrNull()) },
                        placeholder = "Min ₹",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                    FilterTextField(
                        value = draft.maxAmount?.let { formatAmountInput(it) } ?: "",
                        onValueChange = { draft = draft.copy(maxAmount = it.toDoubleOrNull()) },
                        placeholder = "Max ₹",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { draft = TransactionFilter() },
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerHighest),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("RESET", color = OnSurfaceVariant)
                }
                Button(
                    onClick = { onApply(draft) },
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainer, contentColor = OnPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("APPLY", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Active filter chip row shown above History/Analytics lists, e.g.
 * [ HDFC ] [ Food ] [ Last 30 Days ] [ Clear All ].
 */
@Composable
fun ActiveFilterChipsRow(
    filter: TransactionFilter,
    categories: List<Category>,
    accounts: List<Account>,
    onClearDimension: (FilterDimension) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chips = remember(filter, categories, accounts) {
        TransactionFilterEngine.activeChips(filter, categories, accounts)
    }
    if (chips.isEmpty()) return

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        chips.forEach { chip ->
            DismissibleChip(label = chip.label, onDismiss = { onClearDimension(chip.dimension) })
        }
        DismissibleChip(label = "Clear All", emphasised = true, onDismiss = onClearAll)
    }
}

@Composable
private fun FilterSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(bottom = 20.dp)) {
        Text(title.uppercase(), style = StarkTypography.labelSmall.copy(letterSpacing = 1.sp, color = OnSurfaceVariant))
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

@Composable
private fun SelectableChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = PrimaryContainer.copy(alpha = 0.2f),
            selectedLabelColor = Primary,
            containerColor = SurfaceContainerLow,
            labelColor = OnSurfaceVariant
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = OutlineVariant.copy(alpha = 0.3f),
            selectedBorderColor = Primary
        )
    )
}

@Composable
private fun DismissibleChip(label: String, onDismiss: () -> Unit, emphasised: Boolean = false) {
    AssistChip(
        onClick = onDismiss,
        label = { Text(label, style = StarkTypography.labelLarge) },
        trailingIcon = {
            Icon(
                imageVector = Icons.Sharp.Close,
                contentDescription = "Remove filter",
                modifier = Modifier.size(16.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (emphasised) ErrorContainer.copy(alpha = 0.15f) else PrimaryContainer.copy(alpha = 0.12f),
            labelColor = if (emphasised) Error else Primary,
            trailingIconContentColor = if (emphasised) Error else Primary
        ),
        border = null
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLow)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = StarkTypography.bodyLarge.copy(color = OnSurface),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(placeholder, color = OnSurfaceVariant, style = StarkTypography.bodyLarge)
                }
                inner()
            }
        )
    }
}

private fun formatAmountInput(amount: Double): String =
    if (amount == amount.toLong().toDouble()) amount.toLong().toString() else amount.toString()
