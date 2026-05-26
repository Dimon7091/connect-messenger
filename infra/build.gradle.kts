plugins {
    id("common-conventions")
}

group = "ru.gorbunov"
version = "unspecified"

repositories {
    mavenCentral()
}

// Задаем централизованную версию для всего AWS SDK v2
val awsSdkVersion = "2.44.10" // проверьте актуальную версию в Maven Central

dependencies {
    implementation(project(":core"))

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Импортируем BOM (Bill of Materials) для синхронизации версий
    implementation(platform("software.amazon.awssdk:bom:$awsSdkVersion"))

    // Подключаем необходимые сервисы (версия подтянется из BOM)
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:dynamodb")
    implementation("software.amazon.awssdk:s3control")
}

tasks.test {
    useJUnitPlatform()
}