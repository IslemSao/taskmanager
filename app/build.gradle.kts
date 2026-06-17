import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.kotlin.compose)
}

val googleServicesJson = file("google-services.json")

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

fun String?.nonBlankOrNull(): String? = this?.takeIf { it.isNotBlank() }

val releaseKeystoreFile =
    if (keystorePropertiesFile.exists()) {
        rootProject.file(keystoreProperties.getProperty("storeFile", "release-key.keystore"))
    } else {
        null
    }

val releaseSigningReady =
    releaseKeystoreFile != null &&
        releaseKeystoreFile.isFile &&
        keystoreProperties.getProperty("storePassword").nonBlankOrNull() != null &&
        keystoreProperties.getProperty("keyAlias").nonBlankOrNull() != null &&
        keystoreProperties.getProperty("keyPassword").nonBlankOrNull() != null

android {
    namespace = "com.saokt.taskmanager"
    compileSdk = 35

    signingConfigs {
        create("release") {
            if (releaseSigningReady) {
                storeFile = releaseKeystoreFile
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    defaultConfig {
        applicationId = "com.saokt.taskmanager"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "1.4"

        testInstrumentationRunner = "com.saokt.taskmanager.testing.TaskManagerTestRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (releaseSigningReady) {
                signingConfig = signingConfigs.getByName("release")
            }
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
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

if (googleServicesJson.exists()) {
    apply(plugin = "com.google.gms.google-services")
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.11.0"))
    implementation(platform(libs.androidx.compose.bom))

    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-config-ktx")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.multidex)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.appcompat)
    testImplementation("org.mockito:mockito-core:5.0.0")
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("com.google.truth:truth:1.1.3")
    testImplementation("app.cash.turbine:turbine:0.12.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    implementation("com.google.accompanist:accompanist-flowlayout:0.28.0")
    implementation("com.airbnb.android:lottie-compose:6.0.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.20.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("com.valentinilk.shimmer:compose-shimmer:1.0.5")

    implementation("io.coil-kt:coil-compose:2.4.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    testImplementation("org.robolectric:robolectric:4.10.3")

    testImplementation("com.google.dagger:hilt-android-testing:2.45")
    implementation("com.google.android.gms:play-services-auth:21.0.0") {
        exclude(group = "com.google.android.gms", module = "play-services-ads-identifier")
    }

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-android-compiler:2.51.1")

    implementation("androidx.hilt:hilt-work:1.1.0")
    kapt("androidx.hilt:hilt-compiler:1.1.0")

    implementation("androidx.startup:startup-runtime:1.1.1")

    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("com.google.code.gson:gson:2.12.1")
    kapt("androidx.room:room-compiler:$room_version")
    annotationProcessor("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    implementation("androidx.room:room-rxjava2:$room_version")
    implementation("androidx.room:room-rxjava3:$room_version")
    implementation("androidx.room:room-guava:$room_version")
    testImplementation("androidx.room:room-testing:$room_version")
    implementation("androidx.room:room-paging:$room_version")
    implementation(libs.google.material)

    implementation("androidx.glance:glance:1.1.0")
    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("androidx.glance:glance-material3:1.1.0")
}


// Allow references to generated code
kapt {
    correctErrorTypes = true
}

afterEvaluate {
    val signingSetupMessage =
        """
        Release signing is not configured (Play uploads need a real upload key).

        1) Copy keystore.properties.example to keystore.properties in the project root.
        2) Set storeFile to your upload keystore path, and set storePassword, keyAlias, keyPassword.

        Expected keystore file: ${releaseKeystoreFile?.absolutePath ?: "(keystore.properties missing)"}
        """.trimIndent()

    // Fail before packaging/signing so we never produce a debug-signed "release" artifact by mistake.
    tasks.named("packageRelease").configure {
        doFirst {
            if (!releaseSigningReady) {
                throw GradleException(signingSetupMessage)
            }
        }
    }
    tasks.named("signReleaseBundle").configure {
        doFirst {
            if (!releaseSigningReady) {
                throw GradleException(signingSetupMessage)
            }
        }
    }
}
