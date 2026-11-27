package com.example.healthlog.backup

import org.json.JSONObject
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encrypted backup envelope holding cryptographic parameters and the AES-256-GCM ciphertext.
 */
data class EncryptedBackupEnvelope(
    val format: String = BackupSerializer.ENCRYPTED_MAGIC_HEADER,
    val version: Int = BackupSerializer.CURRENT_VERSION,
    val kdf: String = BackupCrypto.KDF_ALGORITHM,
    val iterations: Int = BackupCrypto.PBKDF2_ITERATIONS,
    val saltBase64: String,
    val ivBase64: String,
    val ciphertextBase64: String,
    val metadata: BackupMetadata
)

/**
 * Handles AES-256-GCM encryption and decryption with PBKDF2-HMAC-SHA256 key derivation.
 */
object BackupCrypto {
    const val KDF_ALGORITHM: String = "PBKDF2WithHmacSHA256"
    const val CIPHER_TRANSFORMATION: String = "AES/GCM/NoPadding"
    const val KEY_SIZE_BITS: Int = 256
    const val GCM_TAG_LENGTH_BITS: Int = 128
    const val IV_SIZE_BYTES: Int = 12
    const val SALT_SIZE_BYTES: Int = 16
    const val PBKDF2_ITERATIONS: Int = 100_000

    private val secureRandom = SecureRandom()

    /**
     * Encrypts the raw JSON backup string using AES-256-GCM derived from [password].
     * Returns an envelope serialized as JSON string.
     */
    fun encrypt(plainJson: String, password: CharArray, metadata: BackupMetadata): String {
        require(password.isNotEmpty()) { "Password cannot be empty" }

        val salt = ByteArray(SALT_SIZE_BYTES).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(IV_SIZE_BYTES).also { secureRandom.nextBytes(it) }

        val keySpec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_SIZE_BITS)
        val keyFactory = SecretKeyFactory.getInstance(KDF_ALGORITHM)
        val secretKeyBytes = keyFactory.generateSecret(keySpec).encoded
        val secretKey = SecretKeySpec(secretKeyBytes, "AES")

        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val plaintextBytes = plainJson.toByteArray(Charsets.UTF_8)
        val ciphertext = cipher.doFinal(plaintextBytes)

        val encoder = Base64.getEncoder()
        val envelope = EncryptedBackupEnvelope(
            saltBase64 = encoder.encodeToString(salt),
            ivBase64 = encoder.encodeToString(iv),
            ciphertextBase64 = encoder.encodeToString(ciphertext),
            metadata = metadata.copy(isEncrypted = true)
        )

        return envelopeToJson(envelope)
    }

    /**
     * Decrypts an encrypted backup envelope string using [password].
     * Returns the original plaintext JSON backup string.
     * Throws an exception if the password is incorrect or data is corrupted (GCM auth tag verification failure).
     */
    fun decrypt(encryptedJson: String, password: CharArray): String {
        require(password.isNotEmpty()) { "Password cannot be empty" }

        val envelope = jsonToEnvelope(encryptedJson)
        val decoder = Base64.getDecoder()

        val salt = decoder.decode(envelope.saltBase64)
        val iv = decoder.decode(envelope.ivBase64)
        val ciphertext = decoder.decode(envelope.ciphertextBase64)

        val keySpec = PBEKeySpec(password, salt, envelope.iterations, KEY_SIZE_BITS)
        val keyFactory = SecretKeyFactory.getInstance(envelope.kdf)
        val secretKeyBytes = keyFactory.generateSecret(keySpec).encoded
        val secretKey = SecretKeySpec(secretKeyBytes, "AES")

        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        val decryptedBytes = cipher.doFinal(ciphertext)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    /**
     * Checks if a raw backup string is an encrypted backup without performing full decryption.
     */
    fun isEncryptedBackup(content: String): Boolean {
        return try {
            val root = JSONObject(content)
            root.optString("format") == BackupSerializer.ENCRYPTED_MAGIC_HEADER
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Reads metadata from either a plain or encrypted backup string.
     */
    fun peekMetadata(content: String): BackupMetadata? {
        return try {
            val root = JSONObject(content)
            val format = root.optString("format")
            val metaObj = root.optJSONObject("metadata")

            when (format) {
                BackupSerializer.MAGIC_HEADER -> {
                    BackupMetadata(
                        version = root.optInt("version", BackupSerializer.CURRENT_VERSION),
                        appVersion = root.optString("appVersion", "1.0"),
                        exportedAt = root.optLong("exportedAt", System.currentTimeMillis()),
                        measurementsCount = metaObj?.optInt("measurementsCount") ?: 0,
                        medicationsCount = metaObj?.optInt("medicationsCount") ?: 0,
                        symptomsCount = metaObj?.optInt("symptomsCount") ?: 0,
                        isEncrypted = false
                    )
                }
                BackupSerializer.ENCRYPTED_MAGIC_HEADER -> {
                    BackupMetadata(
                        version = root.optInt("version", BackupSerializer.CURRENT_VERSION),
                        appVersion = root.optString("appVersion", "1.0"),
                        exportedAt = root.optLong("exportedAt", System.currentTimeMillis()),
                        measurementsCount = metaObj?.optInt("measurementsCount") ?: 0,
                        medicationsCount = metaObj?.optInt("medicationsCount") ?: 0,
                        symptomsCount = metaObj?.optInt("symptomsCount") ?: 0,
                        isEncrypted = true
                    )
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun envelopeToJson(envelope: EncryptedBackupEnvelope): String {
        val root = JSONObject().apply {
            put("format", envelope.format)
            put("version", envelope.version)
            put("kdf", envelope.kdf)
            put("iterations", envelope.iterations)
            put("salt", envelope.saltBase64)
            put("iv", envelope.ivBase64)
            put("ciphertext", envelope.ciphertextBase64)
            put("metadata", JSONObject().apply {
                put("measurementsCount", envelope.metadata.measurementsCount)
                put("medicationsCount", envelope.metadata.medicationsCount)
                put("symptomsCount", envelope.metadata.symptomsCount)
                put("isEncrypted", true)
            })
            put("exportedAt", envelope.metadata.exportedAt)
            put("appVersion", envelope.metadata.appVersion)
        }
        return root.toString()
    }

    private fun jsonToEnvelope(jsonString: String): EncryptedBackupEnvelope {
        val root = JSONObject(jsonString)
        val format = root.optString("format")
        if (format != BackupSerializer.ENCRYPTED_MAGIC_HEADER) {
            throw IllegalArgumentException("Not an encrypted backup: $format")
        }

        val metaObj = root.optJSONObject("metadata")
        val metadata = BackupMetadata(
            version = root.optInt("version", BackupSerializer.CURRENT_VERSION),
            appVersion = root.optString("appVersion", "1.0"),
            exportedAt = root.optLong("exportedAt", System.currentTimeMillis()),
            measurementsCount = metaObj?.optInt("measurementsCount") ?: 0,
            medicationsCount = metaObj?.optInt("medicationsCount") ?: 0,
            symptomsCount = metaObj?.optInt("symptomsCount") ?: 0,
            isEncrypted = true
        )

        return EncryptedBackupEnvelope(
            format = format,
            version = root.optInt("version", BackupSerializer.CURRENT_VERSION),
            kdf = root.optString("kdf", KDF_ALGORITHM),
            iterations = root.optInt("iterations", PBKDF2_ITERATIONS),
            saltBase64 = root.getString("salt"),
            ivBase64 = root.getString("iv"),
            ciphertextBase64 = root.getString("ciphertext"),
            metadata = metadata
        )
    }
}
