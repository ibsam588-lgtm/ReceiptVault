plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

val configuredVersionCode = (providers.environmentVariable("ANDROID_VERSION_CODE").orNull
    ?: providers.gradleProperty("ANDROID_VERSION_CODE").orNull)
    ?.toIntOrNull()
    ?: 4
val configuredVersionName = providers.environmentVariable("ANDROID_VERSION_NAME").orNull
    ?: providers.gradleProperty("ANDROID_VERSION_NAME").orNull
    ?: "0.1.3"
val googleSignInWebClientId = providers.environmentVariable("GOOGLE_SIGN_IN_WEB_CLIENT_ID").orNull
    ?: providers.gradleProperty("GOOGLE_SIGN_IN_WEB_CLIENT_ID").orNull
    ?: ""

if (googleSignInWebClientId.isBlank()) {
    logger.warn(
        "ReceiptVault: GOOGLE_SIGN_IN_WEB_CLIENT_ID is not set. The build will ship with " +
            "Google SSO disabled. Provide the Firebase Web OAuth client ID via the env var or " +
            "gradle property to enable \"Continue with Google\"."
    )
}

android {
    namespace = "com.corsairlabs.receiptvault"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.corsairlabs.receiptvault"
        minSdk = 26
        targetSdk = 36
        versionCode = configuredVersionCode
        versionName = configuredVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "R2_BACKUP_API_URL", "\"https://receiptvault-backup.ibsam588.workers.dev\"")
        buildConfigField("String", "GOOGLE_SIGN_IN_WEB_CLIENT_ID", "\"${googleSignInWebClientId.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
            val keystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            val keyAlias = System.getenv("ANDROID_KEY_ALIAS")
            val keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.play.billing)
    implementation(libs.play.services.auth)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
