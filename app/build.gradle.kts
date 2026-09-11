plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "app.harbor"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "app.harbor"
        minSdk = 26
        targetSdk = 37
        // Bumped for every build that reaches a phone. Android refuses an
        // APK whose versionCode is below the installed one, so a participant
        // who gets builds out of order is told no rather than quietly
        // downgraded onto a version that may read their data differently.
        versionCode = 2
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // One debug signature for every machine that builds Harbor.
    //
    // Without this, Gradle signs debug builds with whatever throwaway keystore
    // it finds in the builder's home directory -- a different one on your
    // laptop, on a teammate's, and on every single CI run. Installing one
    // build over another then fails with INSTALL_FAILED_UPDATE_INCOMPATIBLE,
    // and the only way through is to uninstall, which deletes the ledger.
    //
    // During the study that would mean handing a participant an update and
    // erasing their garden -- the one thing we are measuring. So the key is
    // checked in, and every build shares it.
    //
    // Committing a keystore is safe *because* this one is worthless: it is the
    // stock Android debug identity, its password is the published constant
    // "android", and Play will refuse an APK signed with it. Nothing is ever
    // released with this key. A real release key does not go in this repo.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            optimization {
                enable = false
            }
            // Deliberately left unsigned here. Release signing is a separate
            // decision with a separate key, and it must never inherit the
            // debug config above by accident.
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
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
    implementation(libs.play.services.location)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}