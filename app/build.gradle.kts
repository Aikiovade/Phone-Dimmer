plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "io.github.aikiovade.nightdimmer"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "io.github.aikiovade.nightdimmer"
    minSdk = 26
    targetSdk = 36
    versionCode = 3
    versionName = "1.1.1"
  }

  // A release signing config is only registered when a keystore is available.
  // CI injects it through environment variables (see README); local builds
  // simply produce an unsigned release APK instead of failing.
  val releaseKeystoreFile: File? = sequenceOf(
    System.getenv("KEYSTORE_PATH"),
    System.getenv("RELEASE_KEYSTORE_PATH"),
    "${rootDir}/release-keystore.jks",
  )
    .filterNotNull()
    .map(::file)
    .firstOrNull { it.exists() }

  signingConfigs {
    if (releaseKeystoreFile != null) {
      create("release") {
        storeFile = releaseKeystoreFile
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
        keyPassword = System.getenv("KEY_PASSWORD")
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.findByName("release")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  lint {
    warningsAsErrors = false
    abortOnError = true
    checkDependencies = true
  }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)

  testImplementation(libs.junit)

  debugImplementation(libs.androidx.compose.ui.tooling)
}
