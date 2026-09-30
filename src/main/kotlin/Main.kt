package org.lamz

import org.lamz.keystore.DebugKeystoreGenerator
import org.lamz.keystore.KeystoreConfig
import org.lamz.keystore.KeystorePropertiesGenerator
import org.lamz.keystore.KeystoreVerifier
import java.nio.file.Files
import java.nio.file.Path


fun main() {
    while (true) {
        printHeader()
        printMenu()

        when (readInput("Pilih menu")) {
            "1" -> generateKeystore()
            "2" -> generateReleaseKeystore()

            "3" -> generatePropertiesOnly()

            "4" -> generateAll()

            "5" -> verifyKeystore()

            "0" -> return

            else -> {
                println()
                println("Pilihan tidak valid.")
                pause()
            }
        }
    }
}

private fun generateKeystore() {
    clearConsole()

    println("==========================================")
    println("          Generate Keystore")
    println("==========================================")
    println()

    val defaultOutput = Path.of(
        System.getProperty("user.dir"),
        "debug.keystore"
    )

    val outputPath = Path.of(
        readInput(
            label = "Output path",
            defaultValue = defaultOutput.toString()
        )
    )

    println()
    println("--- Keystore Configuration ---")

    val storeType = readInput(
        label = "Store type",
        defaultValue = "JKS"
    ).uppercase()

    val storePassword = readPassword(
        label = "Store password",
        defaultValue = "android"
    )

    val keyAlias = readInput(
        label = "Key alias",
        defaultValue = "androiddebugkey"
    )

    val keyPassword = readPassword(
        label = "Key password",
        defaultValue = "android"
    )

    println()
    println("--- Key Configuration ---")

    val keyAlgorithm = readInput(
        label = "Key algorithm",
        defaultValue = "RSA"
    ).uppercase()

    val keySize = readInt(
        label = "Key size",
        defaultValue = 2048
    )

    val signatureAlgorithm = readInput(
        label = "Signature algorithm",
        defaultValue = "SHA256withRSA"
    )

    val validityDays = readLong(
        label = "Validity days",
        defaultValue = 10_000
    )

    println()
    println("--- Certificate Information ---")

    val commonName = readInput(
        label = "Common Name (CN)",
        defaultValue = "Android Debug"
    )

    val organizationalUnit = readOptionalInput(
        label = "Organizational Unit (OU)"
    )

    val organization = readOptionalInput(
        label = "Organization (O)",
        defaultValue = "Android"
    )

    val city = readOptionalInput(
        label = "City / Locality (L)"
    )

    val state = readOptionalInput(
        label = "State / Province (ST)"
    )

    val country = readOptionalInput(
        label = "Country Code (C)",
        defaultValue = "US"
    )

    val overwrite = if (Files.exists(outputPath)) {
        readYesNo(
            label = "File sudah ada. Timpa?",
            defaultValue = false
        )
    } else {
        false
    }

    val config = KeystoreConfig(
        outputPath = outputPath,

        storeType = storeType,
        storePassword = storePassword,

        keyAlias = keyAlias,
        keyPassword = keyPassword,

        keyAlgorithm = keyAlgorithm,
        keySize = keySize,
        signatureAlgorithm = signatureAlgorithm,

        validityDays = validityDays,

        commonName = commonName,
        organizationalUnit = organizationalUnit,
        organization = organization,
        city = city,
        state = state,
        countryCode = country,

        overwrite = overwrite
    )

    println()
    println("Membuat keystore...")

    try {
        DebugKeystoreGenerator.generate(config)

        println()

        val generateProperties = readYesNo(
            label = "Generate keystore.properties juga?",
            defaultValue = true
        )

        if (generateProperties) {
            println()

            val defaultPropertiesPath = Path.of(
                System.getProperty("user.dir"),
                "keystore.properties"
            )

            val propertiesPath = Path.of(
                readInput(
                    label = "Output keystore.properties",
                    defaultValue = defaultPropertiesPath.toString()
                )
            )

            val overwriteProperties =
                if (Files.exists(propertiesPath)) {
                    readYesNo(
                        label = "keystore.properties sudah ada. Timpa?",
                        defaultValue = false
                    )
                } else {
                    false
                }

            KeystorePropertiesGenerator.generate(
                config = config,
                outputPath = propertiesPath,
                overwrite = overwriteProperties
            )
        }

        println()
        println("Selesai.")

    } catch (e: Exception) {

        println()
        println("Gagal membuat file.")
        println("Reason: ${e.message}")

    } finally {

        /*
         * Bersihkan credential dari memory setelah
         * seluruh proses selesai.
         */
        storePassword.fill('\u0000')
        keyPassword.fill('\u0000')
    }

    pause()
}

private fun printHeader() {
    clearConsole()

    println("==========================================")
    println("       Android Keystore Generator")
    println("==========================================")
    println()
}

private fun printMenu() {
    println("1. Generate keystore")
    println("2. Generate release keystore (.jks)")
    println("3. Generate keystore.properties")
    println("4. Generate semuanya")
    println("5. Verify keystore")
    println("0. Exit")
    println()
}

private fun readInput(
    label: String,
    defaultValue: String? = null
): String {
    while (true) {
        if (defaultValue != null) {
            print("$label [$defaultValue]: ")
        } else {
            print("$label: ")
        }

        val input = readlnOrNull()
            ?.trim()
            .orEmpty()

        val result = if (input.isBlank()) {
            defaultValue.orEmpty()
        } else {
            input
        }

        if (result.isNotBlank()) {
            return result
        }

        println("Input tidak boleh kosong.")
    }
}

private fun readOptionalInput(
    label: String,
    defaultValue: String? = null
): String? {
    if (defaultValue != null) {
        print("$label [$defaultValue]: ")
    } else {
        print("$label [optional]: ")
    }

    val input = readlnOrNull()
        ?.trim()
        .orEmpty()

    return when {
        input.isNotBlank() -> input
        defaultValue != null -> defaultValue
        else -> null
    }
}

private fun readPassword(
    label: String,
    defaultValue: String? = null
): CharArray {
    while (true) {
        val console = System.console()

        val value = if (console != null) {

            val prompt = if (defaultValue != null) {
                "$label [default tersedia]: "
            } else {
                "$label: "
            }

            val password = console.readPassword(prompt)

            if ((password == null || password.isEmpty()) && defaultValue != null) {
                defaultValue.toCharArray()
            } else {
                password ?: CharArray(0)
            }

        } else {
            /*
             * IntelliJ Run Console biasanya tidak menyediakan System.console().
             * Dalam kondisi ini input password akan terlihat.
             */
            val input = readInput(
                label = label,
                defaultValue = defaultValue
            )

            input.toCharArray()
        }

        if (value.isNotEmpty()) {
            return value
        }

        println("Password tidak boleh kosong.")
    }
}

private fun readInt(
    label: String,
    defaultValue: Int
): Int {
    while (true) {
        print("$label [$defaultValue]: ")

        val input = readlnOrNull()
            ?.trim()
            .orEmpty()

        if (input.isBlank()) {
            return defaultValue
        }

        input.toIntOrNull()?.let {
            return it
        }

        println("Masukkan angka yang valid.")
    }
}

private fun readLong(
    label: String,
    defaultValue: Long
): Long {
    while (true) {
        print("$label [$defaultValue]: ")

        val input = readlnOrNull()
            ?.trim()
            .orEmpty()

        if (input.isBlank()) {
            return defaultValue
        }

        input.toLongOrNull()?.let {
            return it
        }

        println("Masukkan angka yang valid.")
    }
}

private fun readYesNo(
    label: String,
    defaultValue: Boolean
): Boolean {
    val defaultLabel = if (defaultValue) {
        "Y/n"
    } else {
        "y/N"
    }

    while (true) {
        print("$label [$defaultLabel]: ")

        return when (
            readlnOrNull()
                ?.trim()
                ?.lowercase()
        ) {
            "y", "yes" -> true
            "n", "no" -> false
            "", null -> defaultValue

            else -> {
                println("Masukkan y atau n.")
                continue
            }
        }
    }
}

private fun pause() {
    println()
    print("Tekan Enter untuk kembali...")
    readlnOrNull()
}

private fun clearConsole() {
    print("\u001b[H\u001b[2J")
    System.out.flush()
}

private fun generateReleaseKeystore() {
    clearConsole()

    println("==========================================")
    println("       Generate Release Keystore")
    println("==========================================")
    println()

    val defaultOutput = Path.of(
        System.getProperty("user.dir"),
        "release.jks"
    )

    val outputPath = Path.of(
        readInput(
            label = "Output path",
            defaultValue = defaultOutput.toString()
        )
    )

    println()
    println("--- Keystore Configuration ---")

    val storePassword = readPassword(
        label = "Store password"
    )

    val keyAlias = readInput(
        label = "Key alias",
        defaultValue = "release"
    )

    val keyPassword = readPassword(
        label = "Key password"
    )

    println()
    println("--- Key Configuration ---")

    val keySize = readInt(
        label = "RSA Key size",
        defaultValue = 2048
    )

    val validityDays = readLong(
        label = "Validity days",
        defaultValue = 10_000
    )

    println()
    println("--- Certificate Information ---")

    val commonName = readInput(
        label = "Common Name (CN)"
    )

    val organizationalUnit = readOptionalInput(
        label = "Organizational Unit (OU)"
    )

    val organization = readOptionalInput(
        label = "Organization (O)"
    )

    val city = readOptionalInput(
        label = "City / Locality (L)"
    )

    val state = readOptionalInput(
        label = "State / Province (ST)"
    )

    val country = readCountryCode(
        label = "Country Code (C)",
        defaultValue = "ID"
    )

    val overwrite = if (Files.exists(outputPath)) {
        readYesNo(
            label = "File sudah ada. Timpa?",
            defaultValue = false
        )
    } else {
        false
    }

    val config = KeystoreConfig(
        outputPath = outputPath,

        storeType = "JKS",

        storePassword = storePassword,

        keyAlias = keyAlias,
        keyPassword = keyPassword,

        keyAlgorithm = "RSA",
        keySize = keySize,
        signatureAlgorithm = "SHA256withRSA",

        validityDays = validityDays,

        commonName = commonName,
        organizationalUnit = organizationalUnit,
        organization = organization,
        city = city,
        state = state,
        countryCode = country,

        overwrite = overwrite
    )

    try {
        DebugKeystoreGenerator.generate(config)

        println()

        val generateProperties = readYesNo(
            label = "Generate keystore.properties juga?",
            defaultValue = true
        )

        if (generateProperties) {

            val defaultPropertiesPath = Path.of(
                System.getProperty("user.dir"),
                "keystore.properties"
            )

            println()

            val propertiesPath = Path.of(
                readInput(
                    label = "Output keystore.properties",
                    defaultValue = defaultPropertiesPath.toString()
                )
            )

            val overwriteProperties =
                if (Files.exists(propertiesPath)) {
                    readYesNo(
                        label = "keystore.properties sudah ada. Timpa?",
                        defaultValue = false
                    )
                } else {
                    false
                }

            KeystorePropertiesGenerator.generate(
                config = config,
                outputPath = propertiesPath,
                overwrite = overwriteProperties
            )
        }

        println()
        println("Release signing files berhasil dibuat.")

    } catch (e: Exception) {

        println()
        println("Gagal membuat release keystore.")
        println("Reason: ${e.message}")

    } finally {

        storePassword.fill('\u0000')
        keyPassword.fill('\u0000')
    }

    pause()
}

private fun readCountryCode(
    label: String,
    defaultValue: String
): String {
    while (true) {
        val value = readInput(
            label = label,
            defaultValue = defaultValue
        ).uppercase()

        if (
            value.length == 2 &&
            value.all { it in 'A'..'Z' }
        ) {
            return value
        }

        println(
            "Country code harus 2 huruf ISO, contoh: ID, US, JP."
        )
    }
}

private fun generatePropertiesOnly() {
    clearConsole()

    println("==========================================")
    println("       Generate keystore.properties")
    println("==========================================")
    println()

    val defaultKeystorePath = Path.of(
        System.getProperty("user.dir"),
        "release.jks"
    )

    val keystorePath = Path.of(
        readInput(
            label = "Keystore path",
            defaultValue = defaultKeystorePath.toString()
        )
    ).toAbsolutePath().normalize()

    if (!Files.exists(keystorePath)) {
        println()
        println("Keystore tidak ditemukan:")
        println(keystorePath)

        pause()
        return
    }

    if (!Files.isRegularFile(keystorePath)) {
        println()
        println("Path bukan sebuah file:")
        println(keystorePath)

        pause()
        return
    }

    println()
    println("--- Signing Configuration ---")

    val storePassword = readPassword(
        label = "Store password"
    )

    val keyAlias = readInput(
        label = "Key alias",
        defaultValue = "release"
    )

    val keyPassword = readPassword(
        label = "Key password"
    )

    println()

    val defaultPropertiesPath = Path.of(
        System.getProperty("user.dir"),
        "keystore.properties"
    )

    val propertiesPath = Path.of(
        readInput(
            label = "Output keystore.properties",
            defaultValue = defaultPropertiesPath.toString()
        )
    ).toAbsolutePath().normalize()

    val overwrite = if (Files.exists(propertiesPath)) {
        readYesNo(
            label = "keystore.properties sudah ada. Timpa?",
            defaultValue = false
        )
    } else {
        false
    }

    /*
     * KeystorePropertiesGenerator kita saat ini menerima
     * KeystoreConfig.
     *
     * Parameter certificate/key generation di bawah tidak
     * digunakan untuk pembuatan keystore.properties.
     */
    val config = KeystoreConfig(
        outputPath = keystorePath,

        storeType = "JKS",

        storePassword = storePassword,

        keyAlias = keyAlias,
        keyPassword = keyPassword,

        keyAlgorithm = "RSA",
        keySize = 2048,
        signatureAlgorithm = "SHA256withRSA",

        validityDays = 10_000,

        commonName = "Not Used",

        overwrite = false
    )

    try {
        KeystorePropertiesGenerator.generate(
            config = config,
            outputPath = propertiesPath,
            overwrite = overwrite
        )

        println()
        println("keystore.properties berhasil dibuat.")
        println()
        println("Keystore : $keystorePath")
        println("Properties: $propertiesPath")

    } catch (e: Exception) {

        println()
        println("Gagal membuat keystore.properties.")
        println("Reason: ${e.message}")

    } finally {

        storePassword.fill('\u0000')
        keyPassword.fill('\u0000')
    }

    pause()
}

private fun generateAll() {
    clearConsole()

    println("==========================================")
    println("             Generate Semua")
    println("==========================================")
    println()
    println("File yang akan dibuat:")
    println("1. debug.keystore")
    println("2. release.jks")
    println("3. keystore.properties")
    println()

    val defaultOutputDirectory = Path.of(
        System.getProperty("user.dir")
    )

    val outputDirectory = Path.of(
        readInput(
            label = "Output directory",
            defaultValue = defaultOutputDirectory.toString()
        )
    ).toAbsolutePath().normalize()

    Files.createDirectories(outputDirectory)

    /*
     * ==========================================================
     * DEBUG KEYSTORE
     * ==========================================================
     */

    println()
    println("==========================================")
    println("         Debug Keystore Configuration")
    println("==========================================")
    println()

    val debugPath = outputDirectory.resolve(
        "debug.keystore"
    )

    println("Output:")
    println(debugPath)
    println()

    val debugStorePassword = readPassword(
        label = "Debug store password",
        defaultValue = "android"
    )

    val debugAlias = readInput(
        label = "Debug key alias",
        defaultValue = "androiddebugkey"
    )

    val debugKeyPassword = readPassword(
        label = "Debug key password",
        defaultValue = "android"
    )

    val debugKeySize = readInt(
        label = "Debug RSA key size",
        defaultValue = 2048
    )

    val debugValidity = readLong(
        label = "Debug validity days",
        defaultValue = 10_000
    )

    println()
    println("--- Debug Certificate ---")

    val debugCommonName = readInput(
        label = "Common Name (CN)",
        defaultValue = "Android Debug"
    )

    val debugOrganizationalUnit = readOptionalInput(
        label = "Organizational Unit (OU)"
    )

    val debugOrganization = readOptionalInput(
        label = "Organization (O)",
        defaultValue = "Android"
    )

    val debugCity = readOptionalInput(
        label = "City / Locality (L)"
    )

    val debugState = readOptionalInput(
        label = "State / Province (ST)"
    )

    val debugCountry = readCountryCode(
        label = "Country Code (C)",
        defaultValue = "US"
    )

    val overwriteDebug =
        if (Files.exists(debugPath)) {
            readYesNo(
                label = "debug.keystore sudah ada. Timpa?",
                defaultValue = false
            )
        } else {
            false
        }

    /*
     * ==========================================================
     * RELEASE KEYSTORE
     * ==========================================================
     */

    println()
    println("==========================================")
    println("        Release Keystore Configuration")
    println("==========================================")
    println()

    val releasePath = outputDirectory.resolve(
        "release.jks"
    )

    println("Output:")
    println(releasePath)
    println()

    val releaseStorePassword = readPassword(
        label = "Release store password"
    )

    val releaseAlias = readInput(
        label = "Release key alias",
        defaultValue = "release"
    )

    val releaseKeyPassword = readPassword(
        label = "Release key password"
    )

    val releaseKeySize = readInt(
        label = "Release RSA key size",
        defaultValue = 2048
    )

    val releaseValidity = readLong(
        label = "Release validity days",
        defaultValue = 10_000
    )

    println()
    println("--- Release Certificate ---")

    val releaseCommonName = readInput(
        label = "Common Name (CN)"
    )

    val releaseOrganizationalUnit = readOptionalInput(
        label = "Organizational Unit (OU)"
    )

    val releaseOrganization = readOptionalInput(
        label = "Organization (O)"
    )

    val releaseCity = readOptionalInput(
        label = "City / Locality (L)"
    )

    val releaseState = readOptionalInput(
        label = "State / Province (ST)"
    )

    val releaseCountry = readCountryCode(
        label = "Country Code (C)",
        defaultValue = "ID"
    )

    val overwriteRelease =
        if (Files.exists(releasePath)) {
            readYesNo(
                label = "release.jks sudah ada. Timpa?",
                defaultValue = false
            )
        } else {
            false
        }

    /*
     * ==========================================================
     * KEYSTORE.PROPERTIES
     * ==========================================================
     */

    val propertiesPath = outputDirectory.resolve(
        "keystore.properties"
    )

    val overwriteProperties =
        if (Files.exists(propertiesPath)) {

            println()

            readYesNo(
                label = "keystore.properties sudah ada. Timpa?",
                defaultValue = false
            )

        } else {
            false
        }

    /*
     * ==========================================================
     * BUILD CONFIG
     * ==========================================================
     */

    val debugConfig = KeystoreConfig(
        outputPath = debugPath,

        storeType = "JKS",

        storePassword = debugStorePassword,

        keyAlias = debugAlias,
        keyPassword = debugKeyPassword,

        keyAlgorithm = "RSA",
        keySize = debugKeySize,
        signatureAlgorithm = "SHA256withRSA",

        validityDays = debugValidity,

        commonName = debugCommonName,
        organizationalUnit = debugOrganizationalUnit,
        organization = debugOrganization,
        city = debugCity,
        state = debugState,
        countryCode = debugCountry,

        overwrite = overwriteDebug
    )

    val releaseConfig = KeystoreConfig(
        outputPath = releasePath,

        storeType = "JKS",

        storePassword = releaseStorePassword,

        keyAlias = releaseAlias,
        keyPassword = releaseKeyPassword,

        keyAlgorithm = "RSA",
        keySize = releaseKeySize,
        signatureAlgorithm = "SHA256withRSA",

        validityDays = releaseValidity,

        commonName = releaseCommonName,
        organizationalUnit = releaseOrganizationalUnit,
        organization = releaseOrganization,
        city = releaseCity,
        state = releaseState,
        countryCode = releaseCountry,

        overwrite = overwriteRelease
    )

    /*
     * ==========================================================
     * GENERATE FILES
     * ==========================================================
     */

    try {

        println()
        println("==========================================")
        println("              Processing")
        println("==========================================")

        println()
        println("[1/3] Generating debug.keystore...")

        DebugKeystoreGenerator.generate(
            debugConfig
        )

        println()
        println("[2/3] Generating release.jks...")

        DebugKeystoreGenerator.generate(
            releaseConfig
        )

        println()
        println("[3/3] Generating keystore.properties...")

        /*
         * PENTING:
         *
         * properties menggunakan releaseConfig,
         * sehingga storeFile menunjuk ke release.jks.
         */
        KeystorePropertiesGenerator.generate(
            config = releaseConfig,
            outputPath = propertiesPath,
            overwrite = overwriteProperties
        )

        println()
        println("==========================================")
        println("          Semua File Berhasil")
        println("==========================================")
        println()

        println("Debug Keystore:")
        println(debugPath)

        println()
        println("Release Keystore:")
        println(releasePath)

        println()
        println("Keystore Properties:")
        println(propertiesPath)

    } catch (e: Exception) {

        println()
        println("==========================================")
        println("                 ERROR")
        println("==========================================")
        println()

        println(
            e.message ?: "Terjadi error yang tidak diketahui."
        )

    } finally {

        /*
         * Bersihkan semua password dari memory.
         */
        debugStorePassword.fill('\u0000')
        debugKeyPassword.fill('\u0000')

        releaseStorePassword.fill('\u0000')
        releaseKeyPassword.fill('\u0000')
    }

    pause()
}

private fun verifyKeystore() {
    clearConsole()

    println("==========================================")
    println("            Verify Keystore")
    println("==========================================")
    println()

    val defaultPath = Path.of(
        System.getProperty("user.dir"),
        "release.jks"
    )

    val keystorePath = Path.of(
        readInput(
            label = "Keystore path",
            defaultValue = defaultPath.toString()
        )
    ).toAbsolutePath().normalize()

    if (!Files.exists(keystorePath)) {
        println()
        println("Keystore tidak ditemukan:")
        println(keystorePath)

        pause()
        return
    }

    val storeType = readInput(
        label = "Store type",
        defaultValue = "JKS"
    ).uppercase()

    val storePassword = readPassword(
        label = "Store password"
    )

    var keyPassword: CharArray? = null

    try {

        /*
         * Buka keystore terlebih dahulu menggunakan
         * store password dan baca daftar alias.
         */
        val aliases = KeystoreVerifier.getAliases(
            keystorePath = keystorePath,
            storeType = storeType,
            storePassword = storePassword
        )

        if (aliases.isEmpty()) {
            println()
            println("Keystore berhasil dibuka, tetapi tidak memiliki alias.")

            pause()
            return
        }

        println()
        println("Alias ditemukan:")
        println()

        aliases.forEachIndexed { index, alias ->
            println("${index + 1}. $alias")
        }

        println()

        val defaultAlias = aliases.first()

        val alias = readInput(
            label = "Alias yang akan diverifikasi",
            defaultValue = defaultAlias
        )

        keyPassword = readPassword(
            label = "Key password"
        )

        println()
        println("Verifying...")
        println()

        val result = KeystoreVerifier.verify(
            keystorePath = keystorePath,
            storeType = storeType,
            storePassword = storePassword,
            alias = alias,
            keyPassword = keyPassword
        )

        println("==========================================")
        println("          Verification Result")
        println("==========================================")
        println()

        println("Keystore")
        println("------------------------------------------")
        println("Path       : ${result.path}")
        println("Store Type : ${result.storeType}")
        println("Alias      : ${result.alias}")

        println()
        println("Key")
        println("------------------------------------------")
        println("Algorithm  : ${result.keyAlgorithm}")

        result.keySize?.let {
            println("Key Size   : $it bit")
        }

        println("Private Key: AVAILABLE")

        println(
            "Key Pair   : ${
                if (result.keyPairMatches) {
                    "MATCH"
                } else {
                    "NOT MATCH"
                }
            }"
        )

        println()
        println("Certificate")
        println("------------------------------------------")

        println(
            "Type       : ${result.certificateType}"
        )

        println(
            "Signature  : ${result.signatureAlgorithm}"
        )

        println(
            "Subject    : ${result.subject}"
        )

        println(
            "Issuer     : ${result.issuer}"
        )

        println(
            "Serial     : ${result.serialNumber}"
        )

        println()

        println(
            "Valid From : ${result.validFrom}"
        )

        println(
            "Valid Until: ${result.validUntil}"
        )

        println(
            "Status     : ${result.certificateStatus}"
        )

        if (
            result.certificateStatus ==
            KeystoreVerifier.CertificateStatus.VALID
        ) {

            println(
                "Remaining  : ${result.remainingDays} days"
            )
        }

        println()
        println("SHA-256 Fingerprint")
        println("------------------------------------------")
        println(result.sha256Fingerprint)

        println()
        println("==========================================")

        if (
            result.keyPairMatches &&
            result.certificateStatus ==
            KeystoreVerifier.CertificateStatus.VALID
        ) {

            println("VERIFICATION SUCCESSFUL")

        } else {

            println("VERIFICATION FAILED")
        }

        println("==========================================")

    } catch (e: Exception) {

        println()
        println("==========================================")
        println("          VERIFICATION FAILED")
        println("==========================================")
        println()

        println(
            "Reason: ${
                e.message ?: e.javaClass.simpleName
            }"
        )

    } finally {

        storePassword.fill('\u0000')

        keyPassword?.fill('\u0000')
    }

    pause()
}