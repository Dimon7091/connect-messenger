plugins {
    id("java")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("io.freefair.lombok")
    checkstyle
}

group = "ru.connect.messenger"
version = "1.0-SNAPSHOT"

java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }
checkstyle { toolVersion = "10.18.0" }
repositories { mavenCentral() }

dependencies {
    // === Spring Boot Стартеры ===
    implementation("org.springframework.boot:spring-boot-starter-web") // Добавлен для работы WebSocket и REST
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // === Kotlin Специфичные библиотеки ===
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // === AWS SDK ===
    implementation(platform("software.amazon.awssdk:bom:2.27.21"))
    implementation("software.amazon.awssdk:s3")

    // === Кеширование ===
    implementation("com.github.ben-manes.caffeine:caffeine")

    // === Базы данных ===
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.15.2")

    // === Мапперы ===
    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")

    // === Дополнительные утилиты ===
    implementation("org.openapitools:jackson-databind-nullable:0.2.6")
    implementation("net.coobird:thumbnailator:0.4.21")
    implementation("net.datafaker:datafaker:2.5.4")

    // === Тестирование ===
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("net.datafaker:datafaker:2.5.4")
}

tasks.test {
    useJUnitPlatform()
}

// Настройка компилятора Java и MapStruct
tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf(
        "-Amapstruct.defaultComponentModel=spring",
        "-Xlint:unchecked",
        "-Xlint:deprecation"
    ))
    options.isIncremental = true
}

tasks.test { useJUnitPlatform() }