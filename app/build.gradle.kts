plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.example.schede"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.schede"
        minSdk = 24
        targetSdk = 34
        versionCode = 5
        versionName = "1.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions.add("user")
    productFlavors {
        create("ilario") {
            dimension = "user"
            isDefault = true
            applicationId = "com.example.schede.ilario"
            resValue("string", "app_name", "Schede Ilario")
            resValue("string", "user_name", "Zanetti Ilario")
        }
        create("luca") {
            dimension = "user"
            applicationId = "com.example.schede.luca"
            resValue("string", "app_name", "Schede Luca")
            resValue("string", "user_name", "Garau Luca")
        }
        create("angelo") {
            dimension = "user"
            applicationId = "com.example.schede.angelo"
            resValue("string", "app_name", "S")
            resValue("string", "user_name", "Angelo Boi")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,DEPENDENCIES}"
        }
    }
    
    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this
            if (output is com.android.build.gradle.internal.api.BaseVariantOutputImpl) {
                output.outputFileName = "app-${variant.name}-${variant.versionName}.apk"
            }
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // Splash Screen Library
    implementation("androidx.core:core-splashscreen:1.0.1")
    
    // Room Database
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // Gson for JSON conversion
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    
    // OkHttp for Google Sheets sync
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
