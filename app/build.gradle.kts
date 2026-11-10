import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// 백엔드 주소. 모든 API 가 이 값 하나만 쓴다. Retrofit baseUrl 은 '/'로 끝나야 한다.
// 기본값은 배포 서버(GSM SV VM, 외부 HTTP 포트 25125). 내 PC 의 로컬 백엔드에 붙이려면 local.properties 에
// moemi.baseUrl=http://10.0.2.2:8080/ 처럼 적는다 (10.0.2.2 는 에뮬레이터에서 본 내 PC).
val deployedBaseUrl = "http://service.gsmsv.site:25125/"
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val moemiBaseUrl: String = localProperties.getProperty("moemi.baseUrl") ?: deployedBaseUrl

android {
    namespace = "com.example.moemi"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.moemi"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "BASE_URL", "\"$moemiBaseUrl\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    // 안드로이드의 org.json 은 로컬 단위 테스트에서 동작하지 않아 실제 구현을 넣는다.
    testImplementation(libs.json)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}