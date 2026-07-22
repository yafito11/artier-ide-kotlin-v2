package com.artier.ide.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SecureStorage - Encrypted storage for API keys and sensitive data
 * Uses Android Keystore via EncryptedSharedPreferences
 */
@Singleton
class SecureStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val sharedPreferences: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Save a string value
     */
    fun putString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    /**
     * Get a string value
     */
    fun getString(key: String, defaultValue: String = ""): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    /**
     * Save an int value
     */
    fun putInt(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }

    /**
     * Get an int value
     */
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }

    /**
     * Save a boolean value
     */
    fun putBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    /**
     * Get a boolean value
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    /**
     * Save a long value
     */
    fun putLong(key: String, value: Long) {
        sharedPreferences.edit().putLong(key, value).apply()
    }

    /**
     * Get a long value
     */
    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return sharedPreferences.getLong(key, defaultValue)
    }

    /**
     * Check if key exists
     */
    fun contains(key: String): Boolean {
        return sharedPreferences.contains(key)
    }

    /**
     * Remove a key
     */
    fun remove(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }

    /**
     * Clear all stored data
     */
    fun clear() {
        sharedPreferences.edit().clear().apply()
    }

    // ==================== API Key Operations ====================

    /**
     * Save API key for a provider
     */
    fun saveApiKey(provider: String, apiKey: String) {
        putString("$KEY_PREFIX_API_KEY$provider", apiKey)
    }

    /**
     * Get API key for a provider
     */
    fun getApiKey(provider: String): String {
        return getString("$KEY_PREFIX_API_KEY$provider")
    }

    /**
     * Check if API key exists for a provider
     */
    fun hasApiKey(provider: String): Boolean {
        return contains("$KEY_PREFIX_API_KEY$provider")
    }

    /**
     * Remove API key for a provider
     */
    fun removeApiKey(provider: String) {
        remove("$KEY_PREFIX_API_KEY$provider")
    }

    // ==================== Provider Settings ====================

    /**
     * Save provider configuration
     */
    fun saveProviderConfig(provider: String, config: Map<String, String>) {
        config.forEach { (key, value) ->
            putString("${KEY_PREFIX_PROVIDER}${provider}_$key", value)
        }
    }

    /**
     * Get provider configuration
     */
    fun getProviderConfig(provider: String): Map<String, String> {
        val config = mutableMapOf<String, String>()
        val keys = listOf("endpoint", "model", "maxTokens", "temperature")
        
        keys.forEach { key ->
            val value = getString("${KEY_PREFIX_PROVIDER}${provider}_$key")
            if (value.isNotEmpty()) {
                config[key] = value
            }
        }
        
        return config
    }

    // ==================== User Preferences ====================

    /**
     * Save theme mode
     */
    fun saveThemeMode(isDarkMode: Boolean) {
        putBoolean(KEY_THEME_DARK, isDarkMode)
    }

    /**
     * Get theme mode
     */
    fun isDarkMode(): Boolean {
        return getBoolean(KEY_THEME_DARK, true)
    }

    /**
     * Save last used agent
     */
    fun saveLastAgent(agentName: String) {
        putString(KEY_LAST_AGENT, agentName)
    }

    /**
     * Get last used agent
     */
    fun getLastAgent(): String {
        return getString(KEY_LAST_AGENT, "opencode")
    }

    /**
     * Save last opened project
     */
    fun saveLastProject(projectPath: String) {
        putString(KEY_LAST_PROJECT, projectPath)
    }

    /**
     * Get last opened project
     */
    fun getLastProject(): String {
        return getString(KEY_LAST_PROJECT)
    }

    companion object {
        private const val PREFS_FILE_NAME = "artier_secure_prefs"
        private const val KEY_PREFIX_API_KEY = "api_key_"
        private const val KEY_PREFIX_PROVIDER = "provider_"
        private const val KEY_THEME_DARK = "theme_dark"
        private const val KEY_LAST_AGENT = "last_agent"
        private const val KEY_LAST_PROJECT = "last_project"
    }
}