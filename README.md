# Android Keystore Generator

CLI sederhana berbasis **Kotlin/JVM + GraalVM Native Image** untuk membuat dan memverifikasi Android keystore tanpa perlu menggunakan `keytool` secara manual.

## Features

- Generate `debug.keystore`
- Generate release `release.jks`
- Generate `keystore.properties`
- Generate seluruh signing files sekaligus
- Verify keystore
- Custom password, alias, key size, validity, dan certificate information
- RSA + SHA256withRSA
- SHA-256 certificate fingerprint
- Native Windows `.exe`

## Tech Stack

- Kotlin
- Java Security API
- Bouncy Castle
- Gradle
- GraalVM Native Image

## Requirements

Untuk development:

- JDK / GraalVM 21
- Gradle Wrapper
- Visual Studio Build Tools 2022
  - MSVC v143
  - Windows SDK
  - Desktop Development with C++

## Compile

```bash
./gradlew compileKotlin
