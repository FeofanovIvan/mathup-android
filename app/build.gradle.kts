plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
    alias(libs.plugins.ksp)
}
val releaseStoreFile: String by project
val releaseStorePassword: String by project
val releaseKeyAlias: String by project
val releaseKeyPassword: String by project


android {
    namespace = "com.feofanova.mathup"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.feofanova.mathup"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        create("release") {
            storeFile = file("keystore/release-key.jks")
            storePassword = "Nasty630320"
            keyAlias = "release_alias"
            keyPassword = "Nasty630320"
        }
    }
    plugins {
        alias(libs.plugins.android.application)
        alias(libs.plugins.kotlin.android)
        alias(libs.plugins.kotlin.compose)
        alias(libs.plugins.google.gms.google.services)
        alias(libs.plugins.google.firebase.crashlytics)
        alias(libs.plugins.ksp)
    }
    val releaseStoreFile: String by project
    val releaseStorePassword: String by project
    val releaseKeyAlias: String by project
    val releaseKeyPassword: String by project


    android {
        namespace = "com.feofanova.mathup"
        compileSdk = 35

        defaultConfig {
            applicationId = "com.feofanova.mathup"
            minSdk = 26
            targetSdk = 35
            versionCode = 1
            versionName = "1.0"

            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        signingConfigs {
            create("release") {
                storeFile = file("keystore/release-key.jks")
                storePassword = "Nasty630320"
                keyAlias = "release_alias"
                keyPassword = "Nasty630320"
            }
        }


        buildTypes {
            release {
                isMinifyEnabled = true
                isShrinkResources = true
                signingConfig = signingConfigs.getByName("release")
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

        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.lifecycle.runtime.ktx)
        implementation(libs.androidx.activity.compose)
        implementation(platform(libs.androidx.compose.bom))
        implementation(libs.androidx.ui)
        implementation(libs.androidx.ui.graphics)
        implementation(libs.androidx.ui.tooling.preview)
        implementation(libs.androidx.material3)
        implementation(libs.firebase.auth)
        implementation(libs.androidx.credentials)
        implementation(libs.androidx.credentials.play.services.auth)
        implementation(libs.googleid)
        implementation(libs.firebase.firestore)
        implementation(libs.firebase.storage)
        implementation(libs.firebase.crashlytics)
        implementation(libs.androidx.navigation.runtime.android)
        implementation(libs.androidx.navigation.compose)
        implementation(libs.androidx.room.common.jvm)
        implementation(libs.androidx.room.runtime.android)
        implementation(libs.firebase.crashlytics.buildtools)
        implementation(libs.firebase.database)
        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.junit)
        androidTestImplementation(libs.androidx.espresso.core)
        androidTestImplementation(platform(libs.androidx.compose.bom))
        androidTestImplementation(libs.androidx.ui.test.junit4)
        debugImplementation(libs.androidx.ui.tooling)
        debugImplementation(libs.androidx.ui.test.manifest)
        implementation(libs.androidx.material.icons.extended)
        implementation(libs.androidx.credentials.v120)
        implementation(libs.androidx.credentials.play.services.auth.v120)
        implementation(libs.googleid.v110)
        implementation(libs.androidx.room.ktx)
        implementation (libs.gson)
        ksp(libs.androidx.room.compiler.v261)
        implementation(libs.androidx.work.runtime.ktx)
        implementation(libs.androidx.work.runtime.ktx.v290)
        implementation(libs.accompanist.navigation.animation)
        implementation (libs.androidx.security.crypto.v110alpha06)
        implementation(libs.kotlinx.coroutines.play.services)
        implementation(libs.androidx.datastore.preferences)

    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
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

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.crashlytics)
    implementation(libs.androidx.navigation.runtime.android)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.common.jvm)
    implementation(libs.androidx.room.runtime.android)
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.firebase.database)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.credentials.v120)
    implementation(libs.androidx.credentials.play.services.auth.v120)
    implementation(libs.googleid.v110)
    implementation(libs.androidx.room.ktx)
    implementation (libs.gson)
    ksp(libs.androidx.room.compiler.v261)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.work.runtime.ktx.v290)
    implementation(libs.accompanist.navigation.animation)
    implementation (libs.androidx.security.crypto.v110alpha06)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.datastore.preferences)

}