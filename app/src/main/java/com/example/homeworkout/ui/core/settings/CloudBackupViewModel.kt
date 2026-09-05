package com.example.homeworkout.ui.core.settings

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.data.backup.DriveBackupManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.FileNotFoundException

data class CloudBackupUiState(
    val accountEmail: String? = null,
    val lastBackupTimeMillis: Long? = null,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val message: String? = null,
    /** True once a restore has finished writing the new DB file — the screen reacts by restarting the app. */
    val restoreCompleted: Boolean = false
)

/** Backs the Settings tab's "Data & Cloud Backup" screen — see DriveBackupManager for the actual Drive I/O. */
class CloudBackupViewModel(
    private val driveBackupManager: DriveBackupManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CloudBackupUiState())
    val uiState: StateFlow<CloudBackupUiState> = _uiState.asStateFlow()

    init {
        refreshAccountState()
    }

    fun signInIntent(): Intent = driveBackupManager.signInClient().signInIntent

    private fun refreshAccountState() {
        _uiState.update {
            it.copy(
                accountEmail = driveBackupManager.signedInAccount()?.email,
                lastBackupTimeMillis = driveBackupManager.lastBackupTimeMillis()
            )
        }
    }

    /** Feed the [Intent] from the sign-in launcher's activity result here. */
    fun onSignInResult(data: Intent?) {
        try {
            GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
            refreshAccountState()
        } catch (e: ApiException) {
            // The user backing out of the account picker is not an error — return to idle quietly.
            if (e.statusCode != GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                val statusName = GoogleSignInStatusCodes.getStatusCodeString(e.statusCode)
                Log.e("CloudBackupViewModel", "Sign-in failed: $statusName (${e.statusCode})", e)
                _uiState.update { it.copy(message = "Sign-in failed: $statusName") }
            }
        }
    }

    fun signOut() {
        driveBackupManager.signOut { refreshAccountState() }
    }

    fun backup() {
        val account = driveBackupManager.signedInAccount() ?: return
        if (!driveBackupManager.isNetworkAvailable()) {
            _uiState.update { it.copy(message = "No internet connection available.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBackingUp = true) }
            try {
                driveBackupManager.backupToDrive(account)
                _uiState.update {
                    it.copy(
                        isBackingUp = false,
                        lastBackupTimeMillis = driveBackupManager.lastBackupTimeMillis(),
                        message = "Backup successfully uploaded."
                    )
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isBackingUp = false, message = "Backup failed: ${t.message ?: "unknown error"}") }
            }
        }
    }

    fun restore() {
        val account = driveBackupManager.signedInAccount() ?: return
        if (!driveBackupManager.isNetworkAvailable()) {
            _uiState.update { it.copy(message = "No internet connection available.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true) }
            try {
                driveBackupManager.restoreFromDrive(account)
                _uiState.update { it.copy(isRestoring = false, restoreCompleted = true) }
            } catch (e: FileNotFoundException) {
                _uiState.update { it.copy(isRestoring = false, message = "No remote backup found.") }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(isRestoring = false, message = "Restore failed: ${t.message ?: "the backup may be corrupt."}")
                }
            }
        }
    }

    fun messageShown() = _uiState.update { it.copy(message = null) }
}
