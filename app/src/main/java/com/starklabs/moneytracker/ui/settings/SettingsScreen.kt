package com.starklabs.moneytracker.ui.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.*
import androidx.compose.material3.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Scaffold(
        containerColor = SurfaceContainerLowest,
        topBar = {
            StarkHeader(
                title = "StarkLedger",
                onSettingsClick = { /* Already on Settings */ }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Screen Title Section
            Column {
                Text(
                    text = "Settings",
                    style = StarkTypography.headlineLarge.copy(fontSize = 32.sp, fontWeight = FontWeight.Medium),
                    color = Primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Configure your financial workspace security and preferences.",
                    style = StarkTypography.labelLarge.copy(fontSize = 14.sp),
                    color = OnSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Security Status Banner
            Surface(
                color = Color(0x33201F1F),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = PrimaryContainer.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp))
                    .border(width = 2.dp, color = SecondaryContainer, shape = RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        color = SecondaryContainer.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Sharp.VerifiedUser, contentDescription = null, tint = SecondaryContainer, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Security Insight", style = StarkTypography.headlineMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium), color = OnSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your ledger is currently protected by standard encryption. Enable Biometric Lock for an extra layer of structural integrity.",
                            style = StarkTypography.bodyMedium,
                            color = OnSurfaceVariant,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { }) {
                            Text("Upgrade Protection", style = StarkTypography.headlineMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium), color = PrimaryContainer)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Sharp.ArrowForward, contentDescription = null, tint = PrimaryContainer, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Settings List
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingsItem(
                    icon = Icons.Sharp.Fingerprint,
                    title = "Biometric Lock",
                    subtitle = "ENHANCED SECURITY",
                    trailing = {
                         Box(modifier = Modifier.size(width = 40.dp, height = 20.dp).clip(RoundedCornerShape(10.dp)).background(OutlineVariant.copy(alpha = 0.3f))) {
                            Box(modifier = Modifier.padding(2.dp).size(16.dp).clip(RoundedCornerShape(8.dp)).background(Outline))
                         }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsItem(
                    icon = Icons.Sharp.Lock,
                    title = "Data & Privacy",
                    subtitle = "ENCRYPTION & EXPORTS",
                    onClick = { /* Export Logic */ }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsItem(
                    icon = Icons.Sharp.QrCodeScanner,
                    title = "SMS Parsing Settings",
                    subtitle = "AUTOMATED TRACKING",
                    trailing = {
                        Surface(color = PrimaryContainer.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                            Text("Active", style = StarkTypography.labelLarge.copy(color = PrimaryFixedDim, fontSize = 12.sp), modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsItem(
                    icon = Icons.Sharp.Info,
                    title = "About StarkLabs",
                    subtitle = "V${com.starklabs.moneytracker.BuildConfig.VERSION_NAME} • STARKLABS"
                )
            }

            // Logout Action
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = {
                    navController.navigate(com.starklabs.moneytracker.ui.Screen.Security.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, Error.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Icon(Icons.Sharp.Logout, contentDescription = null, tint = Error)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Sign Out of Ledger", style = StarkTypography.headlineMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium), color = Error)
            }

            // Footer
            Spacer(modifier = Modifier.height(80.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, PrimaryContainer, Color.Transparent))))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "LOCAL ENCRYPTION • OFFLINE FIRST",
                style = StarkTypography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 2.sp),
                color = OnSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    Surface(
        color = SurfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = SurfaceContainerHigh,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = PrimaryContainer, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = StarkTypography.headlineMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium), color = OnSurface)
                Text(subtitle, style = StarkTypography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.sp), color = OnSurfaceVariant)
            }
            if (trailing != null) {
                trailing()
            }
            Spacer(modifier = Modifier.width(12.dp))
            Icon(Icons.Sharp.ChevronRight, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}
