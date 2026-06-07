package com.starklabs.moneytracker.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ArrowBack
import androidx.compose.material.icons.sharp.Add
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material.icons.sharp.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.starklabs.moneytracker.data.Category
import com.starklabs.moneytracker.data.MoneyRepository
import com.starklabs.moneytracker.domain.CategoryForm
import com.starklabs.moneytracker.ui.theme.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoriesViewModel(private val repository: MoneyRepository) : ViewModel() {

    val categories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(existing: Category?, v: CategoryForm.Validated) {
        viewModelScope.launch {
            val category = Category(
                id = existing?.id ?: 0,
                name = v.name,
                iconName = existing?.iconName ?: "label",
                budgetLimit = v.budget,
                colorHex = v.colorHex,
                keywords = v.keywords
            )
            if (existing == null) repository.addCategory(category) else repository.updateCategory(category)
        }
    }

    fun delete(category: Category) {
        viewModelScope.launch { repository.deleteCategory(category) }
    }
}

class CategoriesViewModelFactory(private val repository: MoneyRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return CategoriesViewModel(repository) as T
    }
}

private fun parseColorOrDefault(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    PrimaryContainer
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(navController: NavController, viewModel: CategoriesViewModel) {
    val categories by viewModel.categories.collectAsState()

    // null = dialog closed; Category(id=0,...sentinel) when adding; existing when editing.
    var editing by remember { mutableStateOf<CategoryEditState?>(null) }
    var pendingDelete by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        containerColor = StarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Categories", style = StarkTypography.titleLarge, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Sharp.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StarkBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editing = CategoryEditState(null) },
                containerColor = PrimaryContainer,
                contentColor = OnPrimary
            ) {
                Icon(Icons.Sharp.Add, contentDescription = "Add category")
            }
        }
    ) { padding ->
        if (categories.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No categories yet. Tap + to create one.", color = TextSecondary, style = StarkTypography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories, key = { it.id }) { category ->
                    CategoryRow(
                        category = category,
                        onEdit = { editing = CategoryEditState(category) },
                        onDelete = { pendingDelete = category }
                    )
                }
            }
        }
    }

    editing?.let { state ->
        CategoryEditDialog(
            existing = state.category,
            onDismiss = { editing = null },
            onSave = { validated ->
                viewModel.save(state.category, validated)
                editing = null
            }
        )
    }

    pendingDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete ${category.name}?", color = TextPrimary) },
            text = { Text("Transactions in this category will become uncategorized. This cannot be undone.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(category)
                    pendingDelete = null
                }) { Text("Delete", color = Error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = StarkSurface
        )
    }
}

private data class CategoryEditState(val category: Category?)

@Composable
private fun CategoryRow(category: Category, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(color = SurfaceContainerLow, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(28.dp).clip(CircleShape).background(parseColorOrDefault(category.colorHex)))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(category.name, style = StarkTypography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                val sub = buildString {
                    if (category.budgetLimit > 0) append("Budget ₹${category.budgetLimit.toLong()}")
                    if (!category.keywords.isNullOrBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append(category.keywords)
                    }
                    if (isEmpty()) append("No budget · no keywords")
                }
                Text(sub, style = StarkTypography.labelSmall, color = TextSecondary, maxLines = 2)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Sharp.Edit, contentDescription = "Edit", tint = PrimaryContainer) }
            IconButton(onClick = onDelete) { Icon(Icons.Sharp.Delete, contentDescription = "Delete", tint = Error) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryEditDialog(
    existing: Category?,
    onDismiss: () -> Unit,
    onSave: (CategoryForm.Validated) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var budget by remember { mutableStateOf(existing?.budgetLimit?.takeIf { it > 0 }?.toLong()?.toString() ?: "") }
    var keywords by remember { mutableStateOf(existing?.keywords ?: "") }
    var colorHex by remember { mutableStateOf(existing?.colorHex ?: CategoryForm.DEFAULT_COLOR) }
    var error by remember { mutableStateOf<String?>(null) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = AccentSecondary,
        unfocusedBorderColor = StarkBorder,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedContainerColor = StarkSurface,
        unfocusedContainerColor = StarkSurface
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StarkSurface,
        title = { Text(if (existing == null) "New Category" else "Edit Category", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name", color = TextSecondary) },
                    singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = budget, onValueChange = { budget = it },
                    label = { Text("Monthly budget (₹, optional)", color = TextSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = keywords, onValueChange = { keywords = it },
                    label = { Text("Keywords (comma separated)", color = TextSecondary) },
                    colors = fieldColors, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = colorHex, onValueChange = { colorHex = it },
                    label = { Text("Color hex (e.g. #00E6FF)", color = TextSecondary) },
                    singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(it, color = Error, style = StarkTypography.labelSmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when (val result = CategoryForm.validate(name, budget, keywords, colorHex)) {
                    is CategoryForm.Result.Ok -> onSave(result.value)
                    is CategoryForm.Result.Error -> error = result.message
                }
            }) { Text("Save", color = PrimaryContainer) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}
