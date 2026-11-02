import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val localSettings = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.isFile) localFile.inputStream().use { load(it) }
}
val apiBaseUrl = gradle.startParameter.projectProperties["MOTTAINAI_API_BASE_URL"]
    ?: localSettings.getProperty("MOTTAINAI_API_BASE_URL")
    ?: providers.gradleProperty("MOTTAINAI_API_BASE_URL").orElse("").get()
val mapsApiKey = providers.gradleProperty("MOTTAINAI_MAPS_API_KEY").orElse("").get()
val assistantBaseUrl = gradle.startParameter.projectProperties["MOTTAINAI_ASSISTANT_BASE_URL"]
    ?: localSettings.getProperty("MOTTAINAI_ASSISTANT_BASE_URL")
    ?: providers.gradleProperty("MOTTAINAI_ASSISTANT_BASE_URL").orElse("").get()
val cloudinaryCloudName = gradle.startParameter.projectProperties["MOTTAINAI_CLOUDINARY_CLOUD_NAME"]
    ?: localSettings.getProperty("MOTTAINAI_CLOUDINARY_CLOUD_NAME")
    ?: providers.gradleProperty("MOTTAINAI_CLOUDINARY_CLOUD_NAME").orElse("").get()
val cloudinaryUploadPreset = gradle.startParameter.projectProperties["MOTTAINAI_CLOUDINARY_UPLOAD_PRESET"]
    ?: localSettings.getProperty("MOTTAINAI_CLOUDINARY_UPLOAD_PRESET")
    ?: providers.gradleProperty("MOTTAINAI_CLOUDINARY_UPLOAD_PRESET").orElse("").get()
val notificationsBaseUrl = gradle.startParameter.projectProperties["MOTTAINAI_NOTIFICATIONS_BASE_URL"]
    ?: localSettings.getProperty("MOTTAINAI_NOTIFICATIONS_BASE_URL")
    ?: providers.gradleProperty("MOTTAINAI_NOTIFICATIONS_BASE_URL").orElse("").get()
val notificationsTokenPath = gradle.startParameter.projectProperties["MOTTAINAI_NOTIFICATIONS_TOKEN_PATH"]
    ?: localSettings.getProperty("MOTTAINAI_NOTIFICATIONS_TOKEN_PATH")
    ?: providers.gradleProperty("MOTTAINAI_NOTIFICATIONS_TOKEN_PATH").orElse("").get()


if (file("google-services.json").exists()) {
    pluginManager.apply("com.google.gms.google-services")
}

android {
    namespace = "com.mottainai.cliente"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.mottainai.cliente"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        buildConfigField("String", "MAPS_API_KEY", "\"${mapsApiKey.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        buildConfigField("String", "ASSISTANT_BASE_URL", "\"${assistantBaseUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"${cloudinaryCloudName.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET", "\"${cloudinaryUploadPreset.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        buildConfigField("String", "NOTIFICATIONS_BASE_URL", "\"${notificationsBaseUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        buildConfigField("String", "NOTIFICATIONS_TOKEN_PATH", "\"${notificationsTokenPath.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation("com.caverock:androidsvg-aar:1.4")
    implementation(libs.glide)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.cardview)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.play.services.maps)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
