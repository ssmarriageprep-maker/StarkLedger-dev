package com.starklabs.moneytracker.ui.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.starklabs.moneytracker.data.MoneyRepository
import com.starklabs.moneytracker.ui.components.*
import com.starklabs.moneytracker.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import com.starklabs.moneytracker.data.AppSettingsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    repository: MoneyRepository,
    appSettingsRepository: AppSettingsRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val factory = AppSettingsViewModelFactory(appSettingsRepository)
    val viewModel: AppSettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
    val dashboardLogCount by viewModel.dashboardLogCount.collectAsState()

    val saveLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    try {
                        val csvData = repository.getExportDataCsv()
                        context.contentResolver.openOutputStream(it)?.use { stream ->
                            stream.write(csvData.toByteArray())
                        }
                        android.widget.Toast.makeText(context, "Export Success", android.widget.Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                         android.widget.Toast.makeText(context, "Export Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    )

    Scaffold(
        containerColor = StarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = StarkTypography.titleLarge, color = TextPrimary) },
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
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            StarkCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Data Management", style = StarkTypography.titleMedium, color = TextPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Export all financial logs to CSV format for external audit.",
                    color = TextSecondary,
                    style = StarkTypography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                StarkButton(
                    text = "SAVE LOCALLY",
                    onClick = {
                        val fileName = "starkledger_archive_${System.currentTimeMillis()}.csv"
                        saveLauncher.launch(fileName)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                StarkButton(
                    text = "SHARE EXPORT",
                    onClick = {
                        scope.launch {
                            exportData(context, repository)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    isEnabled = true
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            StarkCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Dashboard Preferences", style = StarkTypography.titleMedium, color = TextPrimary)
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Recent Logs Limit: $dashboardLogCount",
                    color = TextPrimary,
                    style = StarkTypography.bodyMedium
                )

                Slider(
                    value = dashboardLogCount.toFloat(),
                    onValueChange = { viewModel.setDashboardLogCount(it.toInt()) },
                    valueRange = 5f..50f,
                    steps = 9,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentSecondary,
                        activeTrackColor = AccentSecondary,
                        inactiveTrackColor = StarkSurfaceVariant
                    )
                )

                Text(
                    text = "Controls the number of transactions visible natively on the dashboard.",
                    color = TextSecondary,
                    style = StarkTypography.labelSmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            val allCategories by repository.allCategories.collectAsState(initial = emptyList())
            StarkCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Budget Customization", style = StarkTypography.titleMedium, color = TextPrimary)
                Spacer(modifier = Modifier.height(16.dp))
                
                if (allCategories.isEmpty()) {
                    Text("Loading categories...", color = TextSecondary)
                } else {
                    allCategories.forEach { category ->
                        var localBudget by remember(category.id) { mutableStateOf(category.budgetLimit.toInt().toString()) }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = category.name, style = StarkTypography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                            
                            OutlinedTextField(
                                value = localBudget,
                                onValueChange = { localBudget = it },
                                modifier = Modifier.weight(1f).height(50.dp),
                                textStyle = StarkTypography.bodyMedium.copy(color = TextPrimary),
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentPrimary,
                                    unfocusedBorderColor = StarkBorder
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SAVE",
                                style = StarkTypography.labelSmall,
                                color = AccentSecondary,
                                modifier = Modifier.clickable {
                                    val newBudget = localBudget.toDoubleOrNull()
                                    if (newBudget != null && newBudget >= 0) {
                                        scope.launch {
                                            // Need a dedicated CategoryDao method, but repository doesn't expose it directly.
                                            // Let's add updateCategory(category.copy(budgetLimit = newBudget)) to MoneyRepository.
                                            repository.updateCategory(category.copy(budgetLimit = newBudget))
                                            android.widget.Toast.makeText(context, "Budget Saved", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            StarkCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = "System Information", style = StarkTypography.titleMedium, color = TextPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                StarkStat("Version", "1.2.0")
                Spacer(modifier = Modifier.height(12.dp))
                StarkStat("Environment", "Production Mode")
                Spacer(modifier = Modifier.height(12.dp))
                StarkStat("Security", "Your data stays on device")
            }
        }
    }
}

suspend fun exportData(context: Context, repository: MoneyRepository) {
    val csvData = repository.getExportDataCsv()
    val fileName = "starkledger_export_${System.currentTimeMillis()}.csv"
    val file = File(context.cacheDir, fileName)
    
    FileWriter(file).use { it.write(csvData) }
    
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "EXPORT ARCHIVE"))
}
