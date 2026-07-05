plugins {
    id("common-conventions")
}

group = "ru.gorbunov"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Spring
    implementation ("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // Caffeine
    implementation("com.github.ben-manes.caffeine:caffeine")

    // База данных
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")

    // Маппер
    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")

    // Json
    implementation("org.openapitools:jackson-databind-nullable:0.2.6")

    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.15.2")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // a thumbnail generation library for Java
    implementation("net.coobird:thumbnailator:0.4.21")
}

tasks.test {
    useJUnitPlatform()
}