import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// 純 Kotlin 模組：刻意不依賴 Android，所有計算都能用一般 JVM 單元測試驗證。
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnit()
}
