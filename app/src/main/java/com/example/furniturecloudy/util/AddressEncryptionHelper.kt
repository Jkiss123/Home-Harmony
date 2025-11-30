package com.example.furniturecloudy.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.furniturecloudy.data.Address
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Helper class for encrypting/decrypting sensitive Address data
 * Uses AES-256-GCM encryption with Android Keystore
 *
 * Encrypted fields: phone, addressFull
 * Non-encrypted fields: fullName, wards, district, city, id (for display/search)
 */
@Suppress("unused")
class AddressEncryptionHelper(context: Context) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "HomeHarmonyAddressKey"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12

        // Prefix to identify encrypted data
        const val ENCRYPTED_PREFIX = "ENC:"
    }

    init {
        // Ensure key exists on initialization
        getOrCreateSecretKey()
    }

    /**
     * Encrypt an Address object before saving to Firestore
     * Encrypts: phone, addressFull
     */
    fun encryptAddress(address: Address): Address {
        return address.copy(
            phone = encryptField(address.phone),
            addressFull = encryptField(address.addressFull)
        )
    }

    /**
     * Decrypt an Address object after reading from Firestore
     * Decrypts: phone, addressFull
     */
    fun decryptAddress(address: Address): Address {
        return address.copy(
            phone = decryptField(address.phone),
            addressFull = decryptField(address.addressFull)
        )
    }

    /**
     * Decrypt a list of addresses
     */
    fun decryptAddresses(addresses: List<Address>): List<Address> {
        return addresses.map { decryptAddress(it) }
    }

    /**
     * Encrypt a single field
     * Returns: "ENC:<base64_iv>:<base64_ciphertext>"
     */
    private fun encryptField(plainText: String): String {
        if (plainText.isEmpty()) return plainText
        if (plainText.startsWith(ENCRYPTED_PREFIX)) return plainText // Already encrypted

        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val cipherBase64 = Base64.encodeToString(cipherText, Base64.NO_WRAP)

            "$ENCRYPTED_PREFIX$ivBase64:$cipherBase64"
        } catch (e: Exception) {
            e.printStackTrace()
            plainText // Return original on error
        }
    }

    /**
     * Decrypt a single field
     * Input: "ENC:<base64_iv>:<base64_ciphertext>"
     */
    private fun decryptField(encryptedText: String): String {
        if (encryptedText.isEmpty()) return encryptedText
        if (!encryptedText.startsWith(ENCRYPTED_PREFIX)) return encryptedText // Not encrypted

        return try {
            val data = encryptedText.removePrefix(ENCRYPTED_PREFIX)
            val parts = data.split(":")
            if (parts.size != 2) return encryptedText

            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val cipherText = Base64.decode(parts[1], Base64.NO_WRAP)

            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val plainText = cipher.doFinal(cipherText)
            String(plainText, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            encryptedText // Return original on error
        }
    }

    /**
     * Get or create AES-256 key in Android Keystore
     */
    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        // Return existing key if available
        keyStore.getKey(KEY_ALIAS, null)?.let {
            return it as SecretKey
        }

        // Generate new key
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    /**
     * Check if a field is encrypted
     */
    fun isEncrypted(field: String): Boolean {
        return field.startsWith(ENCRYPTED_PREFIX)
    }

    /**
     * Mask phone number for display (e.g., "0912***456")
     */
    fun maskPhone(phone: String): String {
        val decrypted = if (isEncrypted(phone)) decryptField(phone) else phone
        return if (decrypted.length > 6) {
            "${decrypted.take(4)}***${decrypted.takeLast(3)}"
        } else {
            decrypted
        }
    }
}