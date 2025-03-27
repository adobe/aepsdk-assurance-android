import com.adobe.marketing.mobile.gradle.BuildConstants

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.adobe.marketing.mobile.assurance.tv.testapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.adobe.marketing.mobile.assurance.tv.testapp"
        minSdk = BuildConstants.Versions.MIN_SDK_VERSION
        compileSdk = BuildConstants.Versions.COMPILE_SDK_VERSION
        targetSdk = BuildConstants.Versions.TARGET_SDK_VERSION
        versionName = "1.0"
        vectorDrawables {
            useSupportLibrary = true
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
        sourceCompatibility = BuildConstants.Versions.JAVA_SOURCE_COMPATIBILITY
        targetCompatibility = BuildConstants.Versions.JAVA_TARGET_COMPATIBILITY
    }
    kotlinOptions {
        jvmTarget = BuildConstants.Versions.KOTLIN_JVM_TARGET
        languageVersion = BuildConstants.Versions.KOTLIN_LANGUAGE_VERSION
        apiVersion = BuildConstants.Versions.KOTLIN_API_VERSION
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = BuildConstants.Versions.COMPOSE_COMPILER
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

//    implementation("androidx.core:core-ktx:1.15.0")
//    implementation("androidx.appcompat:appcompat:1.7.0")
//    implementation(platform("androidx.compose:compose-bom:2024.04.01"))
//    implementation("androidx.compose.ui:ui-tooling-preview")
//    implementation("androidx.tv:tv-foundation:1.0.0-alpha12")
//    implementation("androidx.tv:tv-material:1.0.0")
//    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
//    implementation("androidx.activity:activity-compose:1.10.1")
//    androidTestImplementation(platform("androidx.compose:compose-bom:2024.04.01"))
//    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
//    debugImplementation("androidx.compose.ui:ui-tooling")
//    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("androidx.core:core-ktx:1.8.0")

    implementation(platform("androidx.compose:compose-bom:2024.01.00"))
    // Compose dependencies
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material3:material3-android:1.3.1")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Compose Navigation, Activity, and Lifecycle dependencies
    implementation("androidx.appcompat:appcompat:1.0.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.3.1")
    implementation("androidx.activity:activity-compose:1.5.0")
    implementation("androidx.navigation:navigation-compose:2.4.0")

    // AEP SDK dependencies
    implementation(project(":assurance"))
    implementation("com.adobe.marketing.mobile:core:3.0.0")
    implementation("com.adobe.marketing.mobile:signal:3.0.0")
    implementation("com.adobe.marketing.mobile:lifecycle:3.0.0")
    // Messaging, Edge, and EdgeIdentity will be available after Core, Assurance release.
    // Use Snapshot version for initial Assurance release.
    implementation("com.adobe.marketing.mobile:messaging:3.0.0")
    implementation("com.adobe.marketing.mobile:edge:3.0.0")
    implementation("com.adobe.marketing.mobile:edgeidentity:3.0.0")

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.01.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation(platform("androidx.compose:compose-bom:2024.01.00"))
    debugImplementation("androidx.compose.ui:ui-tooling")
}