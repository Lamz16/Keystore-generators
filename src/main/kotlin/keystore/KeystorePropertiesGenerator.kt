package org.lamz.keystore

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

object KeystorePropertiesGenerator {

    fun generate(
        config: KeystoreConfig,
        outputPath: Path,
        overwrite: Boolean = false
    ) {
        val absoluteOutput = outputPath.toAbsolutePath()

        if (Files.exists(absoluteOutput)) {
            if (!overwrite) {
                error(
                    "File sudah ada: $absoluteOutput"
                )
            }

            Files.delete(absoluteOutput)
        }

        absoluteOutput.parent?.let {
            Files.createDirectories(it)
        }

        /*
         * Untuk Android Gradle signing configuration:
         *
         * storeFile
         * storePassword
         * keyAlias
         * keyPassword
         */
        val properties = Properties()

        properties.setProperty(
            "storeFile",
            normalizePath(
                config.outputPath.toAbsolutePath()
            )
        )

        properties.setProperty(
            "storePassword",
            config.storePassword.concatToString()
        )

        properties.setProperty(
            "keyAlias",
            config.keyAlias
        )

        properties.setProperty(
            "keyPassword",
            config.keyPassword.concatToString()
        )

        Files.newOutputStream(
            absoluteOutput
        ).use { output ->

            properties.store(
                output,
                "Android Release Signing Configuration"
            )
        }

        println()
        println("keystore.properties berhasil dibuat.")
        println("------------------------------------------")
        println("Path      : $absoluteOutput")
        println(
            "StoreFile : ${
                normalizePath(
                    config.outputPath.toAbsolutePath()
                )
            }"
        )
        println("Key Alias : ${config.keyAlias}")
        println("------------------------------------------")
    }

    private fun normalizePath(
        path: Path
    ): String {
        /*
         * Gunakan forward slash agar aman ketika dibaca Gradle
         * pada Windows.
         *
         * Contoh:
         *
         * C:/development/keys/release.jks
         */
        return path
            .normalize()
            .toString()
            .replace("\\", "/")
    }
}