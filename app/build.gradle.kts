plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20"
    id("com.google.devtools.ksp")
}

android {
    namespace = "mp.gradia"
    compileSdk = 35

    defaultConfig {
        applicationId = "mp.gradia"
        minSdk = 26
        targetSdk = 35
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
    buildFeatures {
        compose = true
    }
}

dependencies {
    // implementation(files("../libs/ScheduleView.aar"))
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    testImplementation(libs.mockito.core)

    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-reactivestreams-ktx:2.9.0")
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")

    val compose_runtime_version = "1.7.8"
    implementation("androidx.compose.runtime:runtime:$compose_runtime_version")


    // Fragment
    val fragment_version = "1.8.3"
    implementation("androidx.fragment:fragment:$fragment_version")

    // Navigation
    implementation ("androidx.navigation:navigation-fragment:2.7.7")
    implementation ("androidx.navigation:navigation-ui:2.7.7")


    // Android Room & RxJava
    val room_version = "2.6.1"
    ksp("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    implementation("androidx.room:room-rxjava3:$room_version")
    implementation("io.reactivex.rxjava3:rxjava:3.1.5")
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")

    // Widget Library
    implementation("com.github.orion-gz:Pomodoro-Timer-Widget:1.0.7")
    implementation("com.github.orion-gz:Android-Schedule-View-Widget:1.0.4")
    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation ("com.github.Dhaval2404:ColorPicker:2.3")
    // implementation("com.github.AnyChart:AnyChart-Android:1.1.5")

    // 구글 연동
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    // 카카오 로그인 SDK
    implementation("com.kakao.sdk:v2-user:2.21.2")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    // annotationProcessor 'com.github.bumptech.glide:compiler:4.12.0' // Java 프로젝트의 경우 필요할 수 있지만, ksp와 함께 사용 시 생략 가능성 있음
    // 또는 최신 가이드에 따라 ksp 사용
    ksp("com.github.bumptech.glide:ksp:4.16.0") // Glide KSP
}