package com.example.homeworkout.ui.core.settings

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.data.backup.DriveBackupManager
import com.example.homeworkout.ui.components.AppCard
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.ConfirmDialog
import com.example.homeworkout.ui.theme.SettingsBlue
import com.example.homeworkout.ui.theme.SettingsGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Settings-tab "Data & Cloud Backup": manual Google Drive backup/restore of the local database. */
@Composable
fun CloudBackupScreen(
    viewModel: CloudBackupViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    val snackbarHostState = remember { SnackbarHostState() }
    var showRestoreConfirm by remember { mutableStateOf(false) }

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> viewModel.onSignInResult(result.data) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.messageShown()
        }
    }

    // The restore just swapped the on-disk DB file out from under every already-open connection —
    // a full process relaunch is the only reliable way to make the whole app see it (see
    // DriveBackupManager.restartApp's KDoc).
    LaunchedEffect(uiState.restoreCompleted) {
        if (uiState.restoreCompleted && activity != null) {
            snackbarHostState.showSnackbar("Restore complete. Restarting…")
            DriveBackupManager.restartApp(activity)
        }
    }

    Scaffold(
        topBar = { BackTopBar(title = "Data & Cloud Backup", onNavigateBack = onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Google Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (uiState.accountEmail != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CloudStatusRow(icon = Icons.Default.CloudDone, tint = SettingsGreen, text = "Connected: ${uiState.accountEmail}")
                            TextButton(onClick = viewModel::signOut) { Text("Sign out") }
                        }
                    } else {
                        Text(
                            "Sign in to back up your workout and form check history to Google Drive.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                        )
                        Button(onClick = { signInLauncher.launch(viewModel.signInIntent()) }) {
                            Text("Sign in with Google")
                        }
                    }
                }
            }

            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Data & Cloud Sync", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Last backed up: ${formatLastBackup(uiState.lastBackupTimeMillis)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val enabled = uiState.accountEmail != null && !uiState.isBackingUp && !uiState.isRestoring

                    Button(
                        onClick = viewModel::backup,
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isBackingUp) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Text("Back Up to Drive", modifier = Modifier.padding(start = 8.dp))
                    }

                    OutlinedButton(
                        onClick = { showRestoreConfirm = true },
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isRestoring) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = SettingsBlue, modifier = Modifier.size(18.dp))
                        }
                        Text("Restore from Drive", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }

    if (showRestoreConfirm) {
        ConfirmDialog(
            title = "Restore Database?",
            message = "This will overwrite current local workout and form check history with the cloud backup. Do you wish to proceed?",
            confirmLabel = "Overwrite & Restore",
            onConfirm = viewModel::restore,
            onDismiss = { showRestoreConfirm = false }
        )
    }
}

@Composable
private fun CloudStatusRow(icon: ImageVector, tint: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatLastBackup(timeMillis: Long?): String =
    if (timeMillis == null) "Never" else SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(timeMillis))
