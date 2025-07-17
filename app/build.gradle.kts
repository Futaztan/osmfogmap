plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.osmfogmap"
    compileSdk = 35

    packagingOptions {
        resources {
            excludes += setOf(
                "META-INF/native-image/macosx-arm64/jnijavacpp/jni-config.json"
            )
    }

    defaultConfig {
        applicationId = "com.osmfogmap"
        minSdk = 24
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17

    }
        packaging{
            resources {
                excludes += "META-INF/native-image/**"
            }
        }
}

dependencies {

    implementation(libs.play.services.ads)
    implementation (libs.jts.core)
    implementation(libs.smile.core)
    implementation(libs.proj4j)
    implementation (libs.ambilwarna)
    implementation (libs.osmdroid.android)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.ui.graphics.android)
    implementation(libs.play.services.location)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
}