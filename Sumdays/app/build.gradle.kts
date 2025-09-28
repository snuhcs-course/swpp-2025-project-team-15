plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.sumdays"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.sumdays"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.security:security-crypto:1.0.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Retrofit: 네트워크 통신 라이브러리
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // Gson Converter: JSON을 Kotlin 데이터 클래스로 변환
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // OkHttp Logging Interceptor (선택사항): 통신 로그를 확인하여 디버깅에 유용
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
}