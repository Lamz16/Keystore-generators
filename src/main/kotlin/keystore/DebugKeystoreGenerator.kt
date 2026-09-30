package org.lamz.keystore

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.nio.file.Files
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

object DebugKeystoreGenerator {

    fun generate(
        config: KeystoreConfig
    ) {
        validateConfig(config)

        val output = config.outputPath.toAbsolutePath()

        if (Files.exists(output)) {
            if (!config.overwrite) {
                error("File sudah ada: $output")
            }

            Files.delete(output)
        }

        output.parent?.let {
            Files.createDirectories(it)
        }

        println()
        println("Generating key pair...")
        println("Algorithm : ${config.keyAlgorithm}")
        println("Key size  : ${config.keySize} bit")

        val keyPairGenerator = KeyPairGenerator.getInstance(
            config.keyAlgorithm
        )

        keyPairGenerator.initialize(
            config.keySize,
            SecureRandom()
        )

        val keyPair = keyPairGenerator.generateKeyPair()

        println("Generating self-signed X.509 certificate...")

        val certificate = createCertificate(
            config = config,
            publicKey = keyPair.public,
            privateKey = keyPair.private
        )

        println("Creating ${config.storeType} keystore...")

        val keyStore = java.security.KeyStore.getInstance(
            config.storeType
        )

        keyStore.load(
            null,
            config.storePassword
        )

        keyStore.setKeyEntry(
            config.keyAlias,
            keyPair.private,
            config.keyPassword,
            arrayOf(certificate)
        )

        Files.newOutputStream(output).use { stream ->
            keyStore.store(
                stream,
                config.storePassword
            )
        }

        println()
        println("Keystore berhasil dibuat.")
        println("------------------------------------------")
        println("Path       : $output")
        println("Store Type : ${config.storeType}")
        println("Alias      : ${config.keyAlias}")
        println("Algorithm  : ${config.keyAlgorithm}")
        println("Key Size   : ${config.keySize}")
        println("Validity   : ${config.validityDays} days")
        println("Subject    : ${buildDistinguishedName(config)}")
        println("------------------------------------------")
    }

    private fun createCertificate(
        config: KeystoreConfig,
        publicKey: java.security.PublicKey,
        privateKey: java.security.PrivateKey
    ): X509Certificate {

        val now = Instant.now()

        val notBefore = Date.from(
            now.minus(1, ChronoUnit.DAYS)
        )

        val notAfter = Date.from(
            now.plus(config.validityDays, ChronoUnit.DAYS)
        )

        val distinguishedName = buildDistinguishedName(
            config
        )

        val subject = X500Name(
            distinguishedName
        )

        val serialNumber = generateSerialNumber()

        val certificateBuilder =
            JcaX509v3CertificateBuilder(
                subject,
                serialNumber,
                notBefore,
                notAfter,
                subject,
                publicKey
            )

        val contentSigner =
            JcaContentSignerBuilder(
                config.signatureAlgorithm
            ).build(privateKey)

        val certificateHolder =
            certificateBuilder.build(
                contentSigner
            )

        val certificate =
            JcaX509CertificateConverter()
                .getCertificate(
                    certificateHolder
                )

        /*
         * Pastikan certificate yang baru dibuat valid
         * dan benar-benar cocok dengan public key.
         */
        certificate.checkValidity(
            Date()
        )

        certificate.verify(
            publicKey
        )

        return certificate
    }

    private fun buildDistinguishedName(
        config: KeystoreConfig
    ): String {
        val attributes = mutableListOf<String>()

        attributes += "CN=${escapeDn(config.commonName)}"

        config.organizationalUnit
            ?.takeIf { it.isNotBlank() }
            ?.let {
                attributes += "OU=${escapeDn(it)}"
            }

        config.organization
            ?.takeIf { it.isNotBlank() }
            ?.let {
                attributes += "O=${escapeDn(it)}"
            }

        config.city
            ?.takeIf { it.isNotBlank() }
            ?.let {
                attributes += "L=${escapeDn(it)}"
            }

        config.state
            ?.takeIf { it.isNotBlank() }
            ?.let {
                attributes += "ST=${escapeDn(it)}"
            }

        config.countryCode
            ?.takeIf { it.isNotBlank() }
            ?.let {
                attributes += "C=${escapeDn(it.uppercase())}"
            }

        return attributes.joinToString(",")
    }

    private fun escapeDn(
        value: String
    ): String {
        return value
            .replace("\\", "\\\\")
            .replace(",", "\\,")
            .replace("+", "\\+")
            .replace("\"", "\\\"")
            .replace("<", "\\<")
            .replace(">", "\\>")
            .replace(";", "\\;")
    }

    private fun generateSerialNumber(): BigInteger {
        return BigInteger(
            160,
            SecureRandom()
        ).abs()
    }

    private fun validateConfig(
        config: KeystoreConfig
    ) {
        require(config.storePassword.isNotEmpty()) {
            "Store password tidak boleh kosong."
        }

        require(config.keyPassword.isNotEmpty()) {
            "Key password tidak boleh kosong."
        }

        require(config.keyAlias.isNotBlank()) {
            "Key alias tidak boleh kosong."
        }

        require(config.commonName.isNotBlank()) {
            "Common Name tidak boleh kosong."
        }

        require(config.keySize >= 2048) {
            "Key size minimal 2048 bit."
        }

        require(config.validityDays > 0) {
            "Validity harus lebih dari 0 hari."
        }

        config.countryCode
            ?.takeIf { it.isNotBlank() }
            ?.let {
                require(it.length == 2) {
                    "Country code harus 2 karakter, contoh: ID."
                }
            }
    }
}