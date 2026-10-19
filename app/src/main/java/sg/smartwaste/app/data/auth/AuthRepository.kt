package sg.smartwaste.app.data.auth

import android.app.Activity
import android.util.Base64
import android.util.Patterns
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import sg.smartwaste.app.BuildConfig
import sg.smartwaste.app.data.db.UserDao
import sg.smartwaste.app.data.db.UserEntity
import sg.smartwaste.app.data.prefs.UserPrefs
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val user: UserEntity) : SessionState
}

class AuthException(message: String) : Exception(message)

/**
 * On-device accounts. Passwords are never stored: only a salted PBKDF2-HMAC-SHA256 hash.
 * "Remember me" persists the session; otherwise it lasts until the app process ends.
 * Google sign-in uses Android Credential Manager when a Web client ID is configured.
 *
 * Production note: replace with a server-backed identity provider so points and
 * vouchers sync across devices and reset codes are emailed.
 */
class AuthRepository(
    private val users: UserDao,
    private val prefs: UserPrefs,
) {
    /** Session for when "Remember me" is off (memory only). */
    private val memorySession = MutableStateFlow<String?>(null)
    private val loggedOut = MutableStateFlow(false)
    private val random = SecureRandom()

    private data class PendingReset(val email: String, val code: String, val expiresAt: Long)
    private var pendingReset: PendingReset? = null

    /** The signed-in user's email, or null. */
    val currentEmail: Flow<String?> = combine(memorySession, prefs.sessionEmail, loggedOut) { mem, persisted, out ->
        if (out) null else mem ?: persisted
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val session: Flow<SessionState> = currentEmail.flatMapLatest { email ->
        if (email == null) flowOf(SessionState.SignedOut)
        else users.observe(email).map { u -> if (u == null) SessionState.SignedOut else SessionState.SignedIn(u) }
    }

    val lastEmail: Flow<String> = prefs.lastEmail

    suspend fun currentEmailNow(): String = currentEmail.first().orEmpty()

    // ---------------------------------------------------------- email / password

    suspend fun register(name: String, email: String, password: String, remember: Boolean = true) {
        val n = name.trim()
        val e = normaliseEmail(email)
        if (n.length < 2) throw AuthException("Please enter your full name.")
        validatePassword(password)
        if (users.find(e) != null) throw AuthException("An account with this email already exists. Sign in instead.")
        val salt = newSalt()
        users.insert(
            UserEntity(
                email = e, name = n, provider = "password",
                salt = salt.b64(), passwordHash = hash(password, salt).b64(), createdAt = System.currentTimeMillis(),
            )
        )
        startSession(e, remember)
    }

    suspend fun login(email: String, password: String, remember: Boolean) {
        val e = normaliseEmail(email)
        if (password.isEmpty()) throw AuthException("Please enter your password.")
        val user = users.find(e) ?: throw AuthException("No account found for this email. Sign up first.")
        if (user.provider == "google" && user.passwordHash.isEmpty()) {
            throw AuthException("This account uses Google. Tap \"Google\" below to sign in.")
        }
        val ok = MessageDigest.isEqual(hash(password, user.salt.unb64()), user.passwordHash.unb64())
        if (!ok) throw AuthException("Incorrect password. Try again or reset it.")
        startSession(e, remember)
    }

    suspend fun logout() {
        memorySession.value = null
        prefs.setSession(null, remember = false)
        loggedOut.value = true
    }

    suspend fun rename(name: String) {
        val e = currentEmailNow()
        if (e.isNotEmpty() && name.isNotBlank()) users.rename(e, name.trim())
    }

    // ---------------------------------------------------------- password reset

    /**
     * Creates a 5-digit reset code valid for 10 minutes and returns it.
     * There is no mail server in this build, so the UI shows the code (demo mode).
     */
    suspend fun requestResetCode(email: String): String {
        val e = normaliseEmail(email)
        val user = users.find(e) ?: throw AuthException("No account found for this email.")
        if (user.provider == "google" && user.passwordHash.isEmpty()) {
            throw AuthException("This account signs in with Google, so there's no password to reset.")
        }
        val code = (random.nextInt(90_000) + 10_000).toString()
        pendingReset = PendingReset(e, code, System.currentTimeMillis() + 10 * 60_000)
        return code
    }

    suspend fun resetPassword(email: String, code: String, newPassword: String) {
        val e = normaliseEmail(email)
        val pending = pendingReset
        if (pending == null || pending.email != e) throw AuthException("Request a new code first.")
        if (System.currentTimeMillis() > pending.expiresAt) throw AuthException("This code has expired. Request a new one.")
        if (pending.code != code.trim()) throw AuthException("That code is incorrect.")
        validatePassword(newPassword)
        val salt = newSalt()
        users.updatePassword(e, salt.b64(), hash(newPassword, salt).b64())
        pendingReset = null
    }

    // ---------------------------------------------------------- Google

    val googleConfigured: Boolean get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    suspend fun signInWithGoogle(activity: Activity) {
        if (!googleConfigured) {
            throw AuthException("Google sign-in isn't set up in this build. Add SMARTWASTE_GOOGLE_WEB_CLIENT_ID (see INSTALLATION.md), or use email.")
        }
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = try {
            CredentialManager.create(activity).getCredential(activity, request).credential
        } catch (e: GetCredentialCancellationException) {
            throw AuthException("Google sign-in was cancelled.")
        } catch (e: GetCredentialException) {
            throw AuthException("Google sign-in failed: ${e.message ?: e.type}")
        }
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw AuthException("Unexpected credential from Google.")
        }
        val google = GoogleIdTokenCredential.createFrom(credential.data)
        val e = normaliseEmail(google.id)
        val existing = users.find(e)
        if (existing == null) {
            users.upsert(
                UserEntity(
                    email = e, name = google.displayName ?: e.substringBefore('@'), provider = "google",
                    salt = "", passwordHash = "", createdAt = System.currentTimeMillis(),
                )
            )
        }
        startSession(e, remember = true)
    }

    // ---------------------------------------------------------- helpers

    private suspend fun startSession(email: String, remember: Boolean) {
        memorySession.value = email
        prefs.setSession(email, remember)
        loggedOut.value = false
    }

    private fun normaliseEmail(email: String): String {
        val e = email.trim().lowercase()
        if (!Patterns.EMAIL_ADDRESS.matcher(e).matches()) throw AuthException("Please enter a valid email address.")
        return e
    }

    private fun validatePassword(password: String) {
        if (password.length < 8) throw AuthException("Password must be at least 8 characters.")
        if (!password.any { it.isLetter() } || !password.any { it.isDigit() }) {
            throw AuthException("Password needs at least one letter and one number.")
        }
    }

    private fun newSalt() = ByteArray(16).also { random.nextBytes(it) }

    private suspend fun hash(password: String, salt: ByteArray): ByteArray = withContext(Dispatchers.Default) {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256)
        try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun ByteArray.b64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
    private fun String.unb64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

    private companion object {
        const val ITERATIONS = 60_000
    }
}
