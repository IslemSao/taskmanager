plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.taskmanager"
    compileSdk = 35


    defaultConfig {
        applicationId = "com.example.taskmanager"
        minSdk = 26
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    packagingOptions {
        exclude("META-INF/DEPENDENCIES")
        // You might also need to exclude other duplicate files
        // exclude 'META-INF/LICENSE'
        // exclude 'META-INF/LICENSE.txt'
        // exclude 'META-INF/NOTICE'
    }
}

dependencies {
    implementation ("com.google.firebase:firebase-messaging-ktx:23.3.1")
    implementation ("androidx.core:core-splashscreen:1.0.1")
    implementation ("androidx.compose.material:material-icons-extended:1.7.8")
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.multidex)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.appcompat)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.0.0")
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("com.google.truth:truth:1.1.3")
    testImplementation("app.cash.turbine:turbine:0.12.3")
    testImplementation ("androidx.arch.core:core-testing:2.2.0")
    val compose_version = "1.2.1"
    implementation ("com.google.accompanist:accompanist-flowlayout:0.28.0")
    implementation ("com.airbnb.android:lottie-compose:6.0.0")
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.okhttp3:okhttp:4.11.0")
// For authentication
    implementation ("com.google.auth:google-auth-library-oauth2-http:1.20.0")
    // Material 3 extended icons
    implementation ("androidx.compose.material:material-icons-extended:1.5.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    implementation("androidx.compose.foundation:foundation:1.6.7") // Use the latest stable version

    // Shimmer loading effects
    implementation ("com.valentinilk.shimmer:compose-shimmer:1.0.5")

    // Image loading (for icons if needed)
    implementation ("io.coil-kt:coil-compose:2.4.0")
    testImplementation ("androidx.compose.ui:ui-test-junit4:$compose_version")
    testImplementation ("androidx.compose.ui:ui-test-manifest:$compose_version")
    testImplementation ("androidx.compose.material3:material3:$compose_version")
    // Android testing
    // For JUnit 4
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")  // Use 3.5.0 to match what Compose wants
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.7.8")
    testImplementation ("junit:junit:4.13.2")

    // For running Compose UI tests on JVM
    testImplementation ("org.robolectric:robolectric:4.10.3")

    // Hilt testing
    testImplementation("com.google.dagger:hilt-android-testing:2.45")
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    // Coroutines testing
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.1")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("com.google.firebase:firebase-analytics")
    implementation(platform("com.google.firebase:firebase-bom:33.11.0"))
    // Robolectric
    testImplementation("org.robolectric:robolectric:4.10.3")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation("androidx.compose.material3:material3:1.2.0-alpha08") // Or a later version
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-android-compiler:2.51.1")

    val room_version = "2.6.1"

    implementation("androidx.room:room-runtime:$room_version")

    implementation ("com.google.code.gson:gson:2.12.1")
    // If this project uses any Kotlin source, use Kotlin Symbol Processing (KSP)
    // See Add the KSP plugin to your project
    kapt("androidx.room:room-compiler:$room_version")

    // If this project only uses Java source, use the Java annotationProcessor
    // No additional plugins are necessary
    annotationProcessor("androidx.room:room-compiler:$room_version")

    // optional - Kotlin Extensions and Coroutines support for Room
    implementation("androidx.room:room-ktx:$room_version")

    // optional - RxJava2 support for Room
    implementation("androidx.room:room-rxjava2:$room_version")

    // optional - RxJava3 support for Room
    implementation("androidx.room:room-rxjava3:$room_version")

    // optional - Guava support for Room, including Optional and ListenableFuture
    implementation("androidx.room:room-guava:$room_version")

    // optional - Test helpers
    testImplementation("androidx.room:room-testing:$room_version")

    // optional - Paging 3 Integration
    implementation("androidx.room:room-paging:$room_version")
}


// Allow references to generated code
kapt {
    correctErrorTypes = true
}