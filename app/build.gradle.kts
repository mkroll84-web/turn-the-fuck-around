import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val localSecrets = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val billingPublicKey = providers.environmentVariable("GOOGLE_PLAY_LICENSE_KEY").orNull ?: localSecrets.getProperty("GOOGLE_PLAY_LICENSE_KEY", "")
val placesKey = providers.environmentVariable("GOOGLE_PLACES_API_KEY").orNull
    ?: localSecrets.getProperty("GOOGLE_PLACES_API_KEY", "")
// Generate a string literal without logging or committing the configured value.
fun quoted(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\""

plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
android {
    namespace = "com.ttfa"
    compileSdk = 35
    buildToolsVersion = "35.0.0"
    defaultConfig { applicationId = "com.ttfa"; minSdk = 26; targetSdk = 35; versionCode = 3; versionName = "0.3.0"; buildConfigField("String", "GOOGLE_PLACES_API_KEY", quoted(placesKey)); buildConfigField("String", "GOOGLE_PLAY_LICENSE_KEY", quoted(billingPublicKey)) }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    testOptions { unitTests.isReturnDefaultValues = true }
}
kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.01.00"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    implementation("com.google.android.libraries.places:places:6.0.2")
    implementation("com.android.billingclient:billing:9.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.11.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}
