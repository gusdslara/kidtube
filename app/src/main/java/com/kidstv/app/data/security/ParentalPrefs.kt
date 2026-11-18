package com.kidstv.app.data.security

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import com.kidstv.app.data.storage.DeviceStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

data class ParentalState(
    val hasPinConfigured: Boolean = false,
    val isKidLockActive: Boolean = false,
    val forceTvListOnlyMode: Boolean = false,
    val storageQuotaMb: Int = DeviceStorageManager.DEFAULT_QUOTA_MB,
    val pairedTvBaseUrl: String? = null
)

class ParentalPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("kidstv_parental_prefs", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<ParentalState> = _state.asStateFlow()

    private fun loadState(): ParentalState {
        val hasPin = !prefs.getString(KEY_PIN_HASH, null).isNullOrBlank()
        return ParentalState(
            hasPinConfigured = hasPin,
            isKidLockActive = prefs.getBoolean(KEY_KID_LOCK_ACTIVE, false),
            forceTvListOnlyMode = prefs.getBoolean(KEY_FORCE_TV_LIST_ONLY, false),
            storageQuotaMb = prefs.getInt(KEY_STORAGE_QUOTA_MB, DeviceStorageManager.DEFAULT_QUOTA_MB),
            pairedTvBaseUrl = prefs.getString(KEY_PAIRED_TV_URL, null)
        )
    }

    fun hasPinConfigured(): Boolean = !prefs.getString(KEY_PIN_HASH, null).isNullOrBlank()

    fun setPin(rawPin: String): Boolean {
        val trimmed = rawPin.trim()
        if (trimmed.length < 4) return false
        val saltBytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val salt = Base64.getEncoder().encodeToString(saltBytes)
        val hash = sha256("$salt:$trimmed")
        prefs.edit()
            .putString(KEY_PIN_SALT, salt)
            .putString(KEY_PIN_HASH, hash)
            .apply()
        _state.value = loadState()
        return true
    }

    fun verifyPin(rawPin: String): Boolean {
        val salt = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val candidate = sha256("$salt:${rawPin.trim()}")
        return MessageDigest.isEqual(storedHash.toByteArray(), candidate.toByteArray())
    }

    fun setKidLockActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_KID_LOCK_ACTIVE, active).apply()
        _state.value = loadState()
    }

    fun setForceTvListOnlyMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FORCE_TV_LIST_ONLY, enabled).apply()
        _state.value = loadState()
    }

    fun setStorageQuotaMb(quotaMb: Int) {
        val clamped = quotaMb.coerceIn(256, 128 * 1024)
        prefs.edit().putInt(KEY_STORAGE_QUOTA_MB, clamped).apply()
        _state.value = loadState()
    }

    fun setPairedTvBaseUrl(url: String?) {
        prefs.edit().putString(KEY_PAIRED_TV_URL, url?.trim()?.trimEnd('/')).apply()
        _state.value = loadState()
    }

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_KID_LOCK_ACTIVE = "kid_lock_active"
        private const val KEY_FORCE_TV_LIST_ONLY = "force_tv_list_only"
        private const val KEY_STORAGE_QUOTA_MB = "storage_quota_mb"
        private const val KEY_PAIRED_TV_URL = "paired_tv_url"
    }
}

object KioskLockController {

    fun isDeviceOwner(context: Context): Boolean {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            dpm?.isDeviceOwnerApp(context.packageName) == true
        } catch (_: Exception) {
            false
        }
    }

    fun isInLockTaskMode(context: Context): Boolean {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            am?.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
        } catch (_: Exception) {
            false
        }
    }

    fun enterLockTask(activity: Activity): Boolean {
        return try {
            activity.startLockTask()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun exitLockTask(activity: Activity): Boolean {
        return try {
            activity.stopLockTask()
            true
        } catch (_: Exception) {
            false
        }
    }
}
