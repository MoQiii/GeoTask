plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt")
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.syj.geotask"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.syj.geotask"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // 16 KB页面大小兼容性配置
        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a"))
        }
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    buildFeatures {
        compose = true
    }
    packagingOptions {
        jniLibs {
            // 16 KB 页面大小兼容性：必须禁用 legacy 打包方式
            useLegacyPackaging = false

            // 解决重复 so 的问题（Pick first）
            pickFirsts.add("**/libc++_shared.so")
            pickFirsts.add("**/libjsc.so")
        }

        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
//    packaging {
//        // 16 KB页面大小兼容性配置
//        // 解决高德地图SDK的native库对齐问题
//        jniLibs {
//            useLegacyPackaging = false
//        }
//
//        // 排除重复的库文件
//        resources {
//            excludes += "/META-INF/{AL2.0,LGPL2.1}"
//        }
//
//        // 16 KB页面大小对齐
//        jniLibs {
//            pickFirsts += "**/libc++_shared.so"
//            pickFirsts += "**/libjsc.so"
//        }
//    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.2.0")

    // Location Services
    implementation("com.google.android.gms:play-services-location:21.2.0")

    // 高德地图SDK - 升级到最新版本，支持16 KB页面大小
    // 3D地图SDK (已包含定位功能，无需单独添加定位SDK)
//    implementation("com.amap.api:3dmap:9.7.0")
//    // 搜索SDK
//    implementation("com.amap.api:search:9.7.0")
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Timber for logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
