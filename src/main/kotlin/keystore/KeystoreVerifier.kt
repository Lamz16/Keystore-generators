package org.lamz.keystore

import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Signature
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateNotYetValidException
import java.security.cert.X509Certificate
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

object KeystoreVerifier {

    data class VerificationResult(
        val path: Path,
        val storeType: String,
        val alias: String,

        val certificateType: String,

        val keyAlgorithm: String,
        val keySize: Int?,

        val signatureAlgorithm: String,

        val subject: String,
        val issuer: String,
        val serialNumber: String,

        val validFrom: Date,
        val validUntil: Date,
        val certificateStatus: CertificateStatus,

        val remainingDays: Long,

        val sha256Fingerprint: String,

        val privateKeyAvailable: Boolean,
        val keyPairMatches: Boolean
    )

    enum class CertificateStatus {
        VALID,
        EXPIRED,
        NOT_YET_VALID
    }

    fun getAliases(
        keystorePath: Path,
        storeType: String,
        storePassword: CharArray
    ): List<String> {

        validateFile(keystorePath)

        val keyStore = loadKeystore(
            path = keystorePath,
            storeType = storeType,
            storePassword = storePassword
        )

        return keyStore
            .aliases()
            .toList()
    }

    fun verify(
        keystorePath: Path,
        storeType: String,
        storePassword: CharArray,
        alias: String,
        keyPassword: CharArray
    ): VerificationResult {

        validateFile(keystorePath)

        val keyStore = loadKeystore(
            path = keystorePath,
            storeType = storeType,
            storePassword = storePassword
        )

        require(keyStore.containsAlias(alias)) {
            "Alias '$alias' tidak ditemukan di dalam keystore."
        }

        require(keyStore.isKeyEntry(alias)) {
            "Alias '$alias' bukan PrivateKeyEntry."
        }

        val key = keyStore.getKey(
            alias,
            keyPassword
        )

        require(key is PrivateKey) {
            "Private key tidak ditemukan untuk alias '$alias'."
        }

        val certificate = keyStore
            .getCertificate(alias)

        require(certificate is X509Certificate) {
            "Certificate untuk alias '$alias' bukan X.509 certificate."
        }

        val certificateStatus = checkCertificateStatus(
            certificate
        )

        val keyPairMatches = verifyKeyPair(
            privateKey = key,
            certificate = certificate
        )

        val remainingDays = ChronoUnit.DAYS.between(
            Instant.now(),
            certificate.notAfter.toInstant()
        )

        return VerificationResult(
            path = keystorePath.toAbsolutePath().normalize(),

            storeType = storeType,

            alias = alias,

            certificateType = certificate.type,

            keyAlgorithm = certificate.publicKey.algorithm,

            keySize = getKeySize(
                certificate
            ),

            signatureAlgorithm = certificate.sigAlgName,

            subject = certificate.subjectX500Principal.name,

            issuer = certificate.issuerX500Principal.name,

            serialNumber = certificate.serialNumber
                .toString(16)
                .uppercase(),

            validFrom = certificate.notBefore,

            validUntil = certificate.notAfter,

            certificateStatus = certificateStatus,

            remainingDays = remainingDays,

            sha256Fingerprint = createFingerprint(
                certificate
            ),

            privateKeyAvailable = true,

            keyPairMatches = keyPairMatches
        )
    }

    private fun validateFile(
        path: Path
    ) {
        require(Files.exists(path)) {
            "Keystore tidak ditemukan: ${path.toAbsolutePath()}"
        }

        require(Files.isRegularFile(path)) {
            "Path bukan file keystore: ${path.toAbsolutePath()}"
        }
    }

    private fun loadKeystore(
        path: Path,
        storeType: String,
        storePassword: CharArray
    ): KeyStore {

        val keyStore = KeyStore.getInstance(
            storeType
        )

        Files.newInputStream(path).use { input ->
            keyStore.load(
                input,
                storePassword
            )
        }

        return keyStore
    }

    private fun checkCertificateStatus(
        certificate: X509Certificate
    ): CertificateStatus {

        return try {

            certificate.checkValidity()

            CertificateStatus.VALID

        } catch (_: CertificateExpiredException) {

            CertificateStatus.EXPIRED

        } catch (_: CertificateNotYetValidException) {

            CertificateStatus.NOT_YET_VALID
        }
    }

    private fun getKeySize(
        certificate: X509Certificate
    ): Int? {

        return when (
            val publicKey = certificate.publicKey
        ) {

            is RSAPublicKey -> {
                publicKey.modulus.bitLength()
            }

            is ECPublicKey -> {
                publicKey.params.curve.field.fieldSize
            }

            else -> null
        }
    }

    private fun verifyKeyPair(
        privateKey: PrivateKey,
        certificate: X509Certificate
    ): Boolean {

        val signatureAlgorithm = when (
            privateKey.algorithm.uppercase()
        ) {

            "RSA" -> "SHA256withRSA"

            "EC", "ECDSA" -> "SHA256withECDSA"

            else -> return false
        }

        val testData =
            "KeystoreGenerator-KeyPair-Test"
                .toByteArray(Charsets.UTF_8)

        val signer = Signature.getInstance(
            signatureAlgorithm
        )

        signer.initSign(
            privateKey
        )

        signer.update(
            testData
        )

        val signatureBytes =
            signer.sign()

        val verifier = Signature.getInstance(
            signatureAlgorithm
        )

        verifier.initVerify(
            certificate.publicKey
        )

        verifier.update(
            testData
        )

        return verifier.verify(
            signatureBytes
        )
    }

    private fun createFingerprint(
        certificate: X509Certificate
    ): String {

        val digest = MessageDigest.getInstance(
            "SHA-256"
        )

        return digest
            .digest(certificate.encoded)
            .joinToString(":") {
                "%02X".format(it)
            }
    }
}