import com.adobe.marketing.mobile.gradle.BuildConstants

plugins {
    id("aep-library")
}

val mavenCoreVersion: String by project
val navigationComposeVersion = "2.4.0"
val viewModelComposeVersion = "2.5.1"

aepLibrary {
    namespace = "com.adobe.marketing.mobile.assurance"
    compose = true
    enableSpotless = true
    //enableCheckStyle = true

    publishing {
        gitRepoName = "aepsdk-assurance-android"
        addCoreDependency(mavenCoreVersion)

        addMavenDependency("org.jetbrains.kotlin", "kotlin-stdlib-jdk8", BuildConstants.Versions.KOTLIN)
        addMavenDependency("androidx.appcompat", "appcompat", BuildConstants.Versions.ANDROIDX_APPCOMPAT)
        addMavenDependency("androidx.compose.runtime", "runtime", BuildConstants.Versions.COMPOSE)
        addMavenDependency("androidx.compose.material", "material", BuildConstants.Versions.COMPOSE_MATERIAL)
        addMavenDependency("androidx.activity", "activity-compose", BuildConstants.Versions.ANDROIDX_ACTIVITY_COMPOSE)
        addMavenDependency("androidx.navigation", "navigation-compose", navigationComposeVersion)
        addMavenDependency("androidx.lifecycle", "lifecycle-viewmodel-compose", viewModelComposeVersion)
    }
}

dependencies {
    // Stop using SNAPSHOT after Core release.
    implementation("com.adobe.marketing.mobile:core:$mavenCoreVersion-SNAPSHOT")
    // Compose Navigation
    implementation("androidx.navigation:navigation-compose:$navigationComposeVersion")
    // Compose ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$viewModelComposeVersion")

    // TODO: Will be removed once QuickConnect migrates to Compose shortly
    implementation("androidx.constraintlayout:constraintlayout:1.1.3")

    testImplementation("org.mockito:mockito-inline:4.5.1")
    testImplementation("net.sf.kxml:kxml2:2.3.0@jar")
    testImplementation("org.json:json:20171018")
    testImplementation("org.robolectric:robolectric:4.2")
}