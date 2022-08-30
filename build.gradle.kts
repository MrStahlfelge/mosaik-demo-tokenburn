import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.7.0"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.spring") version "1.6.21"
}

group = "org.ergoplatform.mosaik.example"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.h2database:h2")

    // Mosaik
    val mosaikVersion = "1.0.2"
    implementation("com.github.MrStahlfelge.mosaik:common-model:$mosaikVersion")
    implementation("com.github.MrStahlfelge.mosaik:common-model-ktx:$mosaikVersion")
    implementation("com.github.MrStahlfelge.mosaik:serialization-jackson:$mosaikVersion")

    // ErgoPay
    implementation ("org.ergoplatform:ergo-appkit_2.12:4.0.10")
    implementation ("com.github.MrStahlfelge:ergoplatform-jackson:4.0.10")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
