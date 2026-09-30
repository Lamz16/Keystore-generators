package org.lamz.keystore

import java.nio.file.Path

data class KeystoreConfig(
    val outputPath: Path,

    // Keystore
    val storeType: String = "JKS",
    val storePassword: CharArray,

    // Key
    val keyAlias: String,
    val keyPassword: CharArray,
    val keyAlgorithm: String = "RSA",
    val keySize: Int = 2048,
    val signatureAlgorithm: String = "SHA256withRSA",

    // Certificate
    val validityDays: Long = 10_000,

    val commonName: String,
    val organizationalUnit: String? = null,
    val organization: String? = null,
    val city: String? = null,
    val state: String? = null,
    val countryCode: String? = null,

    // File
    val overwrite: Boolean = false
)