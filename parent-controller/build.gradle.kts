import org.gradle.api.tasks.Copy

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val childApkAssetsDir = layout.buildDirectory.dir("generated/child-apk-assets")

val copyChildDebugApk by tasks.registering(Copy::class) {
    dependsOn(":child-agent:assembleDebug")
    from(project(":child-agent").layout.buildDirectory.dir("outputs/apk/debug")) {
        include("child-agent-debug.apk")
        rename { "kidguard-child.apk" }
    }
    into(childApkAssetsDir)
}

val copyChildReleaseApk by tasks.registering(Copy::class) {
    // The repository does not define a release signing key for the child yet.
    // Bundle the signed debug artifact rather than an unusable unsigned APK.
    dependsOn(":child-agent:assembleDebug")
    from(project(":child-agent").layout.buildDirectory.dir("outputs/apk/debug")) {
        include("child-agent-debug.apk")
        rename { "kidguard-child.apk" }
    }
    into(childApkAssetsDir)
}

tasks.configureEach {
    when (name) {
        "preDebugBuild" -> dependsOn(copyChildDebugApk)
        "preReleaseBuild" -> dependsOn(copyChildReleaseApk)
    }
}

android {
    namespace = "com.parentalguard.parent"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.parentalguard.controller"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "2.4.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

android.sourceSets.getByName("main").assets.srcDir(childApkAssetsDir)

dependencies {
    implementation(project(":common"))

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1") // Added for AppCompatActivity
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    
    // Compose
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")
    
    // Compose Foundation for pagers
    implementation("androidx.compose.foundation:foundation")
    
    // Networking Client
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-cio:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    val cameraVersion = "1.4.2"
    implementation("androidx.camera:camera-camera2:$cameraVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraVersion")
    implementation("androidx.camera:camera-view:$cameraVersion")
    implementation("com.google.zxing:core:3.5.2")
    implementation("androidx.compose.material:material-icons-extended")
    
    // Biometric
    implementation("androidx.biometric:biometric:1.1.0")

    // WebSocket Client
    implementation("io.ktor:ktor-client-websockets:2.3.7")
    
    // SLF4J Implementation
    implementation("org.slf4j:slf4j-simple:2.0.7")
}
