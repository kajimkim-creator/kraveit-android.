plugins {
    id("com.android.application")
}

android {
    namespace = "ke.co.kraveit.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "ke.co.kraveit.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 6
        versionName = "1.3.1"
        buildConfigField("String", "KRAVEIT_URL", "\"https://kraveit.netlify.app/\"")
    }

    val releaseStoreFile = System.getenv("KRAVEIT_KEYSTORE_FILE")
    val releaseStorePassword = System.getenv("KRAVEIT_STORE_PASSWORD")
    val releaseKeyAlias = System.getenv("KRAVEIT_KEY_ALIAS")
    val releaseKeyPassword = System.getenv("KRAVEIT_KEY_PASSWORD")

    signingConfigs {
        create("release") {
            if (!releaseStoreFile.isNullOrBlank()) {
                storeFile = file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // CI can build an unsigned release APK when signing secrets are absent.
            // When release signing environment variables are present, Gradle signs normally.
            if (!releaseStoreFile.isNullOrBlank() &&
                !releaseStorePassword.isNullOrBlank() &&
                !releaseKeyAlias.isNullOrBlank() &&
                !releaseKeyPassword.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.activity:activity:1.9.3")
    implementation("androidx.core:core:1.13.1")
    implementation("androidx.webkit:webkit:1.12.1")
}
