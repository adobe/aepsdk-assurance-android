/**
 * Copyright 2024 Adobe. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

import com.adobe.marketing.mobile.gradle.BuildConstants

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.adobe.marketing.mobile.assurance.testapp"

    defaultConfig {
        applicationId = "com.adobe.mobile.marketing.assurance.testapp"
        minSdk = BuildConstants.Versions.MIN_SDK_VERSION
        compileSdk = BuildConstants.Versions.COMPILE_SDK_VERSION
        targetSdk = BuildConstants.Versions.TARGET_SDK_VERSION

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        getByName(BuildConstants.BuildTypes.RELEASE) {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

    dependencies {
        val composeVersion = "1.5.0"

        implementation("androidx.core:core-ktx:1.9.0")
        implementation("androidx.compose.ui:ui:$composeVersion")
        implementation("androidx.compose.material:material:$composeVersion")
        implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")

        implementation("androidx.appcompat:appcompat:1.0.0")
        implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.3.1")
        implementation("androidx.activity:activity-compose:1.5.0")
        implementation("androidx.navigation:navigation-compose:2.4.0")

        implementation(project(":assurance"))
        implementation("com.adobe.marketing.mobile:core:3.0.0-SNAPSHOT") {}
        implementation("com.adobe.marketing.mobile:signal:3.0.0-SNAPSHOT") {
            isTransitive = false
        }
        implementation("com.adobe.marketing.mobile:lifecycle:3.0.0-SNAPSHOT") {
            isTransitive = false
        }
        implementation("com.adobe.marketing.mobile:messaging:2.1.4") {
            isTransitive = false
        }
        implementation("com.adobe.marketing.mobile:edge:2.1.0") {
            isTransitive = false
        }
        implementation("com.adobe.marketing.mobile:edgeidentity:2.0.0") {
            isTransitive = false
        }

        testImplementation("junit:junit:4.13.2")
        androidTestImplementation("androidx.test.ext:junit:1.1.3")
        androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
        androidTestImplementation("androidx.compose.ui:ui-test-junit4:${BuildConstants.Versions.COMPOSE}")
        debugImplementation("androidx.compose.ui:ui-tooling:${BuildConstants.Versions.COMPOSE}")
    }
}
