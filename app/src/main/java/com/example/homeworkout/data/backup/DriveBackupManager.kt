package com.example.homeworkout.data.backup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.homeworkout.data.local.AppDatabase
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile

/**
 * Manual Google Drive backup/restore of the whole Room database file, stored as a single hidden
 * object in the signed-in user's `appDataFolder` (invisible in the user's regular Drive UI, and
 * only this app can read it back — see DriveScopes.DRIVE_APPDATA). No account/backend server is
 * involved: this is a straight device<->Drive file sync triggered from the Settings screen.
 */
class DriveBackupManager(
    private val context: Context,
    private val database: AppDatabase
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun signInClient(): GoogleSignInClient {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        return GoogleSignIn.getClient(context, options)
    }

    /** The account last granted appDataFolder access, if any — null when nobody is signed in yet. */
    fun signedInAccount(): GoogleSignInAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val hasScope = GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_APPDATA))
        return account.takeIf { hasScope }
    }

    fun lastBackupTimeMillis(): Long? = prefs.getLong(KEY_LAST_BACKUP, -1L).takeIf { it > 0 }

    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun driveService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(context, setOf(DriveScopes.DRIVE_APPDATA))
        credential.selectedAccount = account.account
        return Drive.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
            .setApplicationName("Finess Mobile")
            .build()
    }

    private fun findRemoteBackupId(service: Drive): String? = service.files().list()
        .setSpaces("appDataFolder")
        .setQ("name = '$BACKUP_FILE_NAME' and trashed = false")
        .setFields("files(id, name)")
        .execute()
        .files
        .firstOrNull()
        ?.id

    /** Flushes the WAL into the main DB file, then uploads/overwrites it in `appDataFolder`. */
    suspend fun backupToDrive(account: GoogleSignInAccount) = withContext(Dispatchers.IO) {
        // `query(...)` alone never runs the PRAGMA — Android's Cursor is lazy and only actually
        // executes the statement once a row is read (moveToFirst/getCount). Without that read, this
        // checkpoint was a silent no-op: the backup was relying entirely on SQLite's own opportunistic
        // auto-checkpoint (only every ~1000 WAL pages, skippable while a reader is active), so recent
        // writes across random tables would or wouldn't have made it into the main .db file by chance.
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
        val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)

        val service = driveService(account)
        val mediaContent = FileContent("application/octet-stream", dbFile)
        val existingId = findRemoteBackupId(service)
        if (existingId != null) {
            service.files().update(existingId, null, mediaContent).execute()
        } else {
            val metadata = com.google.api.services.drive.model.File().apply {
                name = BACKUP_FILE_NAME
                parents = listOf("appDataFolder")
            }
            service.files().create(metadata, mediaContent).setFields("id").execute()
        }

        prefs.edit().putLong(KEY_LAST_BACKUP, System.currentTimeMillis()).apply()
    }

    /**
     * Downloads the remote backup to a temp file, validates it, then swaps it in for the live DB.
     * Throws [FileNotFoundException] if no backup exists, or [IOException] for an empty/corrupt one
     * — in both cases the live database file is left completely untouched.
     */
    suspend fun restoreFromDrive(account: GoogleSignInAccount) = withContext(Dispatchers.IO) {
        val service = driveService(account)
        val remoteId = findRemoteBackupId(service) ?: throw FileNotFoundException("No remote backup found.")

        val tempFile = File(context.cacheDir, "$BACKUP_FILE_NAME.tmp")
        try {
            FileOutputStream(tempFile).use { out ->
                service.files().get(remoteId).executeMediaAndDownloadTo(out)
            }
            validateSqliteFile(tempFile)

            // Only close/replace the live DB once the download is confirmed good, so a bad
            // download never leaves the app mid-restore with no working database.
            AppDatabase.closeInstance()

            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            tempFile.copyTo(dbFile, overwrite = true)
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()
        } finally {
            tempFile.delete()
        }

        prefs.edit().putLong(KEY_LAST_BACKUP, System.currentTimeMillis()).apply()
    }

    private fun validateSqliteFile(file: File) {
        if (!file.exists() || file.length() == 0L) {
            throw IOException("Downloaded backup was empty.")
        }
        val header = ByteArray(16)
        RandomAccessFile(file, "r").use { it.readFully(header) }
        val magic = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
        if (!header.contentEquals(magic)) {
            throw IOException("Downloaded backup is not a valid database file.")
        }
    }

    fun signOut(onComplete: () -> Unit) {
        signInClient().signOut().addOnCompleteListener { onComplete() }
    }

    companion object {
        private const val PREFS_NAME = "drive_backup_prefs"
        private const val KEY_LAST_BACKUP = "last_backup_time"
        private const val BACKUP_FILE_NAME = AppDatabase.DATABASE_NAME + ".bak"

        /**
         * Restoring swaps out the on-disk DB file underneath every Room/App singleton that already
         * holds a reference to it (App.database is a `by lazy` that can't be re-run in place), so
         * the only reliable way to make the whole app see the new data is a full process relaunch
         * rather than just `activity.recreate()`.
         */
        fun restartApp(activity: Activity) {
            val intent = activity.packageManager.getLaunchIntentForPackage(activity.packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            activity.startActivity(intent)
            activity.finish()
            Runtime.getRuntime().exit(0)
        }
    }
}
