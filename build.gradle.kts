plugins {
    kotlin("jvm") version "2.4.0"
    application
    id("org.graalvm.buildtools.native") version "1.1.14"
}

group = "org.lamz"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.bouncycastle:bcprov-jdk18on:1.86")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.86")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("org.lamz.MainKt")
}

tasks.test {
    useJUnitPlatform()
}