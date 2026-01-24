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
                        android.widget.Toast.makeText(context, "EXPORT PROTOCOL: SUCCESS", android.widget.Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                         android.widget.Toast.makeText(context, "EXPORT ERROR: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    )

    Box(modifier = Modifier.fillMaxSize().background(StarkBackground)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Sharp.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                }
                HudHeader(title = "SYSTEM CONFIG", subtitle = "MANAGING DATA PROTOCOLS")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                NeonText(text = "CORE DATA ENGINE", color = JarvisGold, style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(16.dp))
                
                NeonText(
                    text = "EXPORT ALL FINANCIAL LOGS TO CSV FORMAT FOR EXTERNAL AUDIT.",
                    color = TextGrey,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                HudButton(
                    text = "SAVE ARCHIVE LOCALLY",
                    onClick = {
                        val fileName = "starkledger_archive_${System.currentTimeMillis()}.csv"
                        saveLauncher.launch(fileName)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = NeonCyan
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                HudButton(
                    text = "TRANSMIT ARCHIVE (SHARE)",
                    onClick = {
                        scope.launch {
                            exportData(context, repository)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = NeonCyan.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                NeonText(text = "DASHBOARD PREFERENCES", color = JarvisGold, style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(16.dp))

                NeonText(
                    text = "LOG HISTORY LIMIT: $dashboardLogCount",
                    color = TextWhite,
                    style = MaterialTheme.typography.labelSmall
                )

                Slider(
                    value = dashboardLogCount.toFloat(),
                    onValueChange = { viewModel.setDashboardLogCount(it.toInt()) },
                    valueRange = 5f..50f,
                    steps = 9, // 5, 10, 15, ..., 50
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = StarkSurface
                    )
                )

                NeonText(
                    text = "CONFIGURES THE NUMBER OF RECENT TRANSACTIONS VISIBLE ON THE PRIMARY HUD.",
                    color = TextGrey,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                NeonText(text = "FIRMWARE INFORMATION", color = JarvisGold, style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(12.dp))
                NeonText(text = "VERSION: 1.2.0-STARK", style = MaterialTheme.typography.labelSmall, color = TextWhite)
                NeonText(text = "OS: JARVIS-HUD-KOTLIN", style = MaterialTheme.typography.labelSmall, color = TextWhite)
                NeonText(text = "ENCRYPTION: AES-256 (SIMULATED)", style = MaterialTheme.typography.labelSmall, color = TextGrey)
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
            NeonText(text = "PROPERTY OF STARK INDUSTRIES", color = TextGrey.copy(alpha = 0.3f), style = MaterialTheme.typography.labelSmall)
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
