// LOCATION: :androidApp/build.gradle.kts
plugins {
    id("com.android.application")
    alias(libs.plugins.composeCompiler) // Needed for compose runtime inside your activity
}

android {
    namespace = "com.plcoding.kmp_gradle9_migration.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.plcoding.kmp_gradle9_migration"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // 🔗 LINK YOUR MULTIPLATFORM CODE HERE (Change ":shared" to match your folder name)
    implementation(project(":composeApp")) // or ":composeApp" depending on your configuration

    implementation(libs.androidx.activity.compose)
    implementation("androidx.compose.ui:ui:1.6.0")
    implementation("androidx.compose.material3:material3:1.2.0")

    // ⬇️ ADD THESE LINES TO FIX THE UNRESOLVED REFERENCES ⬇️
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.0")
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.0")
    testImplementation("junit:junit:4.13.2")

    // ⬇️ 2. ADD THESE LINES for Instrumented UI Testing ⬇️
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // ⬇️ ADD THIS LINE TO FIX THE UNRESOLVED INITIALIZATIONPROVIDER CLASS ⬇️
    implementation("androidx.startup:startup-runtime:1.1.1")
}
