import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("io.freefair.lombok")
    checkstyle
}

repositories {
    mavenCentral()
}

// Общие настройки для всех модулей
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.set(listOf("-Xjsr305=strict"))
    }
}

checkstyle {
    toolVersion = "10.12.5"
    // Ваши остальные настройки, если они есть
}


dependencies {
    // То, что нужно в каждом модуле (например, логирование)
    implementation("org.springframework.boot:spring-boot-starter-web")
}
