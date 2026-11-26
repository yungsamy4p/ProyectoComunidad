plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.yungsamy.comunidadconectada"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.yungsamy.comunidadconectada"
        minSdk = 24
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)

    // Firebase (Auth, Realtime Database, Cloud Messaging)
    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-messaging")

    // MQTT Paho
    implementation("com.github.hannesa2:paho.mqtt.android:3.3.5")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")

    // Pruebas
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.14.2")
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}