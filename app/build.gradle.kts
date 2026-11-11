plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.nightstand.clock"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.nightstand.clock"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // minSdk 24 沒有 java.time，靠 desugaring 補上。
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

// Robolectric 測試（截圖預覽、鬧鐘流程）只在加上 -Probo 時執行：
// 第一次要下載約 200MB 的模擬 Android 環境，一般的 ./gradlew test 只跑純 JVM 單元測試。
tasks.withType<Test>().configureEach {
    if (!project.hasProperty("robo")) {
        exclude("**/robo/**")
        // app 目前只有 Robolectric 測試，排除後沒有測試可跑是正常的。
        failOnNoDiscoveredTests = false
    }
    // Robolectric 在 JDK 17+ 需要存取 JDK 內部 API。
    jvmArgs(
        "--add-exports", "java.base/jdk.internal.access=ALL-UNNAMED",
        "--add-opens", "java.base/java.io=ALL-UNNAMED",
    )
    systemProperty("preview.outDir", rootProject.layout.buildDirectory.dir("preview").get().asFile.absolutePath)
}

dependencies {
    implementation(project(":clockmath"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
