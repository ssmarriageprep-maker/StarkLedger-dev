package com.starklabs.moneytracker.ui.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.starklabs.moneytracker.data.MoneyRepository
import com.starklabs.moneytracker.ui.components.GlassCard
import com.starklabs.moneytracker.ui.components.HudButton
import com.starklabs.moneytracker.ui.components.NeonText
import com.starklabs.moneytracker.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter

@Composable
fun SettingsScreen(repository: MoneyRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // SAF Launcher for saving file
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
                        android.widget.Toast.makeText(context, "Export Saved Successfully", android.widget.Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                         android.widget.Toast.makeText(context, "Export Failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    )

    Box(modifier = Modifier.fillMaxSize().background(StarkBlack)) {
        Column(modifier = Modifier.padding(16.dp)) {
            NeonText(text = "SYSTEM CONFIG", style = MaterialTheme.typography.headlineMedium)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                NeonText(text = "DATA MANAGEMENT", color = JarvisGold)
                Spacer(modifier = Modifier.height(16.dp))
                
                HudButton(
                    text = "SAVE CSV LOCALLY", 
                    onClick = {
                        val fileName = "starkledger_export_${System.currentTimeMillis()}.csv"
                        saveLauncher.launch(fileName)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = NeonCyan
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                HudButton(
                    text = "SHARE CSV", 
                    onClick = {
                        scope.launch {
                            exportData(context, repository)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = NeonCyanDim
                )
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
    context.startActivity(Intent.createChooser(intent, "Export Data"))
}
