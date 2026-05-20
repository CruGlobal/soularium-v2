# Soularium v2 Mobile — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Soularium v2 mobile app from scratch as a Kotlin Multiplatform + Compose Multiplatform project, achieving feature parity with the discontinued iOS/Android originals plus modern UX, accessibility, and i18n.

**Architecture:** Three Gradle modules (`:domain` pure Kotlin, `:data` KMP, `:composeApp` KMP UI). Pure-domain conversation state machine drives UI rendering. Room (KMP) for persistence. Firebase for analytics/crash. Crowdin via GitHub Actions for translations. See `docs/superpowers/specs/2026-05-20-soularium-v2-design.md` for the full spec.

**Tech Stack:** Kotlin 2.x, Compose Multiplatform 1.8+, Gradle KTS, Koin (DI), Room (KMP), Coil 3, Firebase (Analytics + Crashlytics), Compose Navigation (KMP), kotlinx.coroutines + Flow, Crowdin CLI, Fastlane.

**Spec reference:** `docs/superpowers/specs/2026-05-20-soularium-v2-design.md`

---

## Phase 0: Project Bootstrap

Goal: a buildable, runnable KMP+CMP shell on both platforms with the three-module structure and CI on day 1.

### Task 1: Initialize KMP project structure

**Files:**
- Create: `mobile/settings.gradle.kts`
- Create: `mobile/build.gradle.kts`
- Create: `mobile/gradle.properties`
- Create: `mobile/gradle/libs.versions.toml`
- Create: `mobile/gradle/wrapper/gradle-wrapper.properties`
- Create: `.gitignore` (repo root, expanded)

- [ ] **Step 1: Create the `mobile/` directory and Gradle wrapper**

Run:
```bash
cd /Users/danielbisgrove/Documents/Web_Dev/soularium-v2
mkdir -p mobile/gradle/wrapper
gradle wrapper --gradle-version 8.10.2 --project-dir mobile
```

Expected: `gradle-wrapper.jar`, `gradle-wrapper.properties`, `gradlew`, `gradlew.bat` created in `mobile/`.

- [ ] **Step 2: Write `mobile/settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "soularium"

include(":composeApp", ":domain", ":data")
```

- [ ] **Step 3: Write `mobile/gradle/libs.versions.toml`**

```toml
[versions]
kotlin = "2.1.0"
agp = "8.7.3"
compose-multiplatform = "1.7.3"
androidx-lifecycle = "2.8.4"
androidx-navigation = "2.8.0-alpha10"
room = "2.7.0-alpha11"
sqlite = "2.5.0-alpha11"
koin = "4.0.0"
coil = "3.0.4"
coroutines = "1.9.0"
ksp = "2.1.0-1.0.29"
firebase-bom = "33.7.0"
firebase-crashlytics-gradle = "3.0.2"
google-services = "4.4.2"
ktlint = "12.1.2"
turbine = "1.2.0"
kotest = "5.9.1"
multiplatform-markdown-renderer = "0.27.0"
libphonenumber = "1.0.4"

[libraries]
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "androidx-lifecycle" }
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "androidx-navigation" }
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
sqlite-bundled = { module = "androidx.sqlite:sqlite-bundled", version.ref = "sqlite" }
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin" }
koin-compose-viewmodel = { module = "io.insert-koin:koin-compose-viewmodel", version.ref = "koin" }
coil-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil" }
coil-network-okhttp = { module = "io.coil-kt.coil3:coil-network-okhttp", version.ref = "coil" }
coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
firebase-bom = { module = "com.google.firebase:firebase-bom", version.ref = "firebase-bom" }
firebase-analytics = { module = "com.google.firebase:firebase-analytics-ktx" }
firebase-crashlytics = { module = "com.google.firebase:firebase-crashlytics-ktx" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
kotest-assertions = { module = "io.kotest:kotest-assertions-core", version.ref = "kotest" }
markdown-renderer = { module = "com.mikepenz:multiplatform-markdown-renderer-m3", version.ref = "multiplatform-markdown-renderer" }
libphonenumber = { module = "io.michaelrocks:libphonenumber-android", version.ref = "libphonenumber" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
compose-multiplatform = { id = "org.jetbrains.compose", version.ref = "compose-multiplatform" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
room = { id = "androidx.room", version.ref = "room" }
ktlint = { id = "org.jlleitschuh.gradle.ktlint", version.ref = "ktlint" }
google-services = { id = "com.google.gms.google-services", version.ref = "google-services" }
firebase-crashlytics = { id = "com.google.firebase.crashlytics", version.ref = "firebase-crashlytics-gradle" }
```

- [ ] **Step 4: Write `mobile/build.gradle.kts` (root)**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}
```

- [ ] **Step 5: Write `mobile/gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true

kotlin.code.style=official
kotlin.mpp.enableCInteropCommonization=true

android.useAndroidX=true
android.nonTransitiveRClass=true
```

- [ ] **Step 6: Write repo-root `.gitignore`**

```gitignore
# IDEs
.idea/
*.iml
.vscode/
.fleet/

# Build outputs
build/
**/build/
.gradle/
**/.gradle/
local.properties

# Android
*.apk
*.aab
captures/

# iOS / Xcode
xcuserdata/
DerivedData/
**/Pods/
*.xcworkspace/xcshareddata/swiftpm/
.swiftpm/

# Kotlin / Native
.kotlin/
**/.kotlin/

# macOS
.DS_Store

# Secrets
*.keystore
*.jks
google-services.json
GoogleService-Info.plist
!**/example.google-services.json
```

- [ ] **Step 7: Commit**

```bash
cd /Users/danielbisgrove/Documents/Web_Dev/soularium-v2
git add .gitignore mobile/
git commit -m "chore: initialize KMP project structure"
```

---

### Task 2: Create the `:domain` module (pure Kotlin)

**Files:**
- Create: `mobile/domain/build.gradle.kts`
- Create: `mobile/domain/src/main/kotlin/org/cru/soularium/domain/.gitkeep`

- [ ] **Step 1: Write `mobile/domain/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
}

dependencies {
    implementation(libs.coroutines.core)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    testImplementation(kotlin("test"))
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.turbine)
}
```

- [ ] **Step 2: Verify domain module builds**

Run: `cd mobile && ./gradlew :domain:build`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add mobile/domain/
git commit -m "chore(domain): scaffold pure-Kotlin domain module"
```

---

### Task 3: Create the `:data` module (KMP)

**Files:**
- Create: `mobile/data/build.gradle.kts`
- Create: `mobile/data/src/commonMain/kotlin/org/cru/soularium/data/.gitkeep`
- Create: `mobile/data/src/androidMain/kotlin/org/cru/soularium/data/.gitkeep`
- Create: `mobile/data/src/iosMain/kotlin/org/cru/soularium/data/.gitkeep`

- [ ] **Step 1: Write `mobile/data/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.ktlint)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions { jvmTarget = "17" }
        }
    }
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "data"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":domain"))
            implementation(libs.coroutines.core)
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotest.assertions)
            implementation(libs.turbine)
        }
    }
}

android {
    namespace = "org.cru.soularium.data"
    compileSdk = 35
    defaultConfig { minSdk = 21 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosX64", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}
```

- [ ] **Step 2: Verify data module compiles**

Run: `cd mobile && ./gradlew :data:compileKotlinMetadata`
Expected: `BUILD SUCCESSFUL` (empty source sets are fine)

- [ ] **Step 3: Commit**

```bash
git add mobile/data/
git commit -m "chore(data): scaffold KMP data module with Room"
```

---

### Task 4: Create the `:composeApp` module (KMP + CMP)

**Files:**
- Create: `mobile/composeApp/build.gradle.kts`
- Create: `mobile/composeApp/src/commonMain/kotlin/org/cru/soularium/App.kt`
- Create: `mobile/composeApp/src/androidMain/kotlin/org/cru/soularium/MainActivity.kt`
- Create: `mobile/composeApp/src/androidMain/AndroidManifest.xml`
- Create: `mobile/composeApp/src/iosMain/kotlin/org/cru/soularium/MainViewController.kt`

- [ ] **Step 1: Write `mobile/composeApp/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ktlint)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions { jvmTarget = "17" }
        }
    }
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":domain"))
            implementation(project(":data"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.coil.compose)
            implementation(libs.markdown.renderer)
            implementation(libs.coroutines.core)
        }
        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.9.3")
            implementation(platform(libs.firebase.bom))
            implementation(libs.firebase.analytics)
            implementation(libs.firebase.crashlytics)
            implementation(libs.libphonenumber)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotest.assertions)
            implementation(libs.turbine)
        }
    }
}

android {
    namespace = "org.cru.soularium"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.cru.soularium"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "2.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        debug { applicationIdSuffix = ".dev" }
        release {
            isMinifyEnabled = false
        }
    }
}
```

- [ ] **Step 2: Write `mobile/composeApp/src/androidMain/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:label="Soularium"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:configChanges="orientation|screenSize|smallestScreenSize|screenLayout|keyboardHidden|uiMode">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 3: Write `mobile/composeApp/src/commonMain/kotlin/org/cru/soularium/App.kt`**

```kotlin
package org.cru.soularium

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Soularium v2 — bootstrap OK")
            }
        }
    }
}
```

- [ ] **Step 4: Write `mobile/composeApp/src/androidMain/kotlin/org/cru/soularium/MainActivity.kt`**

```kotlin
package org.cru.soularium

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}
```

- [ ] **Step 5: Write `mobile/composeApp/src/iosMain/kotlin/org/cru/soularium/MainViewController.kt`**

```kotlin
package org.cru.soularium

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController { App() }
```

- [ ] **Step 6: Run the Android app on emulator/device**

Run:
```bash
cd mobile && ./gradlew :composeApp:assembleDebug
```
Expected: `BUILD SUCCESSFUL`. APK at `composeApp/build/outputs/apk/debug/composeApp-debug.apk`.

Then launch on an emulator manually (Android Studio) or via `adb install` and confirm "Soularium v2 — bootstrap OK" displays.

- [ ] **Step 7: Commit**

```bash
git add mobile/composeApp/
git commit -m "chore(composeApp): scaffold KMP+CMP app module with bootstrap screen"
```

---

### Task 5: Create the iOS Xcode wrapper

**Files:**
- Create: `mobile/iosApp/iosApp.xcodeproj/project.pbxproj` (generated by Xcode)
- Create: `mobile/iosApp/iosApp/iOSApp.swift`
- Create: `mobile/iosApp/iosApp/ContentView.swift`
- Create: `mobile/iosApp/iosApp/Info.plist`

- [ ] **Step 1: Create the Xcode project shell**

In Xcode: File → New → Project → iOS App. Set:
- Product Name: `iosApp`
- Bundle Identifier: `org.cru.soularium`
- Interface: SwiftUI
- Language: Swift
- Location: `mobile/iosApp/`

- [ ] **Step 2: Write `mobile/iosApp/iosApp/iOSApp.swift`**

```swift
import SwiftUI

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
```

- [ ] **Step 3: Write `mobile/iosApp/iosApp/ContentView.swift`**

```swift
import SwiftUI
import ComposeApp

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.keyboard)
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
```

- [ ] **Step 4: Add a Run Script Build Phase to embed the Compose framework**

In Xcode: Target → Build Phases → New Run Script Phase (above "Compile Sources"). Script:

```bash
cd "$SRCROOT/.."
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
```

Add input file: `$(SRCROOT)/../composeApp/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`.

- [ ] **Step 5: Build & run on iOS Simulator**

In Xcode: Product → Run with an iOS Simulator selected. Expected: "Soularium v2 — bootstrap OK" displays.

- [ ] **Step 6: Commit**

```bash
git add mobile/iosApp/
git commit -m "chore(iosApp): add Xcode wrapper consuming Compose framework"
```

---

## Phase 1: Domain Types

Goal: every type the state machine and pure functions will need, with tests for the constructors that have invariants.

### Task 6: Domain primitive types

**Files:**
- Create: `mobile/domain/src/main/kotlin/org/cru/soularium/domain/Ids.kt`
- Create: `mobile/domain/src/main/kotlin/org/cru/soularium/domain/SessionKind.kt`
- Create: `mobile/domain/src/main/kotlin/org/cru/soularium/domain/ContactInfo.kt`
- Test: `mobile/domain/src/test/kotlin/org/cru/soularium/domain/IdsTest.kt`

- [ ] **Step 1: Write the test**

`mobile/domain/src/test/kotlin/org/cru/soularium/domain/IdsTest.kt`:

```kotlin
package org.cru.soularium.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class IdsTest {
    @Test
    fun `SessionId roundtrips through string`() {
        val id = SessionId.random()
        assertEquals(id, SessionId.fromString(id.value))
    }

    @Test
    fun `Two random SessionIds differ`() {
        assertNotEquals(SessionId.random(), SessionId.random())
    }
}
```

- [ ] **Step 2: Run the test, verify it fails**

Run: `cd mobile && ./gradlew :domain:test --tests "org.cru.soularium.domain.IdsTest"`
Expected: compilation failure, `SessionId` unresolved.

- [ ] **Step 3: Write `Ids.kt`**

```kotlin
package org.cru.soularium.domain

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class SessionId(val value: String) {
    companion object {
        fun random(): SessionId = SessionId(generateId())
        fun fromString(s: String): SessionId = SessionId(s)
    }
}

@Serializable
@JvmInline
value class ConversationId(val value: String) {
    companion object {
        fun random(): ConversationId = ConversationId(generateId())
        fun fromString(s: String): ConversationId = ConversationId(s)
    }
}

@Serializable
@JvmInline
value class CardPickId(val value: String) {
    companion object {
        fun random(): CardPickId = CardPickId(generateId())
    }
}

private fun generateId(): String {
    val bytes = ByteArray(16)
    secureRandom.nextBytes(bytes)
    bytes[6] = (bytes[6].toInt() and 0x0f or 0x40).toByte()
    bytes[8] = (bytes[8].toInt() and 0x3f or 0x80).toByte()
    return bytes.joinToString("") { "%02x".format(it) }.let {
        "${it.substring(0, 8)}-${it.substring(8, 12)}-${it.substring(12, 16)}-${it.substring(16, 20)}-${it.substring(20, 32)}"
    }
}

private val secureRandom = java.security.SecureRandom()
```

- [ ] **Step 4: Write `SessionKind.kt`**

```kotlin
package org.cru.soularium.domain

import kotlinx.serialization.Serializable

@Serializable
enum class SessionKind { SOLO, GROUP }
```

- [ ] **Step 5: Write `ContactInfo.kt`**

```kotlin
package org.cru.soularium.domain

import kotlinx.serialization.Serializable

@Serializable
data class ContactInfo(
    val name: String,
    val surname: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val notes: String? = null,
)
```

- [ ] **Step 6: Run the tests, verify they pass**

Run: `cd mobile && ./gradlew :domain:test`
Expected: 2 tests pass.

- [ ] **Step 7: Commit**

```bash
git add mobile/domain/
git commit -m "feat(domain): add Ids, SessionKind, ContactInfo"
```

---

### Task 7: Domain entities (Session, Conversation, CardPick)

**Files:**
- Create: `mobile/domain/src/main/kotlin/org/cru/soularium/domain/Session.kt`
- Create: `mobile/domain/src/main/kotlin/org/cru/soularium/domain/Conversation.kt`
- Create: `mobile/domain/src/main/kotlin/org/cru/soularium/domain/CardPick.kt`

- [ ] **Step 1: Write `Session.kt`**

```kotlin
package org.cru.soularium.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Session(
    val id: SessionId,
    val kind: SessionKind,
    val startedAt: Instant,
    val endedAt: Instant? = null,
    val bookmarkedAt: Instant? = null,
    val selectionInstructionsShown: Boolean = false,
)
```

- [ ] **Step 2: Add `kotlinx-datetime` to domain dependencies**

Edit `mobile/domain/build.gradle.kts`, in the `dependencies` block add:

```kotlin
implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
```

- [ ] **Step 3: Write `Conversation.kt`**

```kotlin
package org.cru.soularium.domain

import kotlinx.serialization.Serializable

@Serializable
data class Conversation(
    val id: ConversationId,
    val sessionId: SessionId,
    val displayOrder: Int,
    val contact: ContactInfo,
)
```

- [ ] **Step 4: Write `CardPick.kt`**

```kotlin
package org.cru.soularium.domain

import kotlinx.serialization.Serializable

@Serializable
data class CardPick(
    val id: CardPickId,
    val conversationId: ConversationId,
    val questionNumber: Int,
    val cardId: Int,
    val pickOrder: Int,
    val isFinal: Boolean,
)
```

- [ ] **Step 5: Verify compilation**

Run: `cd mobile && ./gradlew :domain:compileKotlin`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add mobile/domain/
git commit -m "feat(domain): add Session, Conversation, CardPick entities"
```

---

### Task 8: Domain content types (Question metadata, CardImage)

**Files:**
- Create: `mobile/domain/src/main/kotlin/org/cru/soularium/domain/content/Question.kt`
- Create: `mobile/domain/src/main/kotlin/org/cru/soularium/domain/content/CardImage.kt`
- Create: `mobile/domain/src/main/kotlin/org/cru/soularium/domain/content/Questions.kt`
- Test: `mobile/domain/src/test/kotlin/org/cru/soularium/domain/content/QuestionsTest.kt`

- [ ] **Step 1: Write `Question.kt`**

```kotlin
package org.cru.soularium.domain.content

data class Question(
    val number: Int,
    val selectionRounds: Int,
    val requiredImageCount: Int,
    val promptKey: String,
    val selectionKey: String,
    val finalizingKey: String,
    val discussionKey: String,
) {
    init {
        require(number in 1..5) { "Question number must be 1..5, was $number" }
        require(selectionRounds in 1..2) { "Selection rounds must be 1 or 2, was $selectionRounds" }
        require(requiredImageCount in 1..3) { "Required image count must be 1..3, was $requiredImageCount" }
    }
}
```

- [ ] **Step 2: Write `CardImage.kt`**

```kotlin
package org.cru.soularium.domain.content

data class CardImage(
    val id: Int,
) {
    init {
        require(id in 1..50) { "Card id must be 1..50, was $id" }
    }
}
```

- [ ] **Step 3: Write the test**

`mobile/domain/src/test/kotlin/org/cru/soularium/domain/content/QuestionsTest.kt`:

```kotlin
package org.cru.soularium.domain.content

import kotlin.test.Test
import kotlin.test.assertEquals

class QuestionsTest {
    @Test
    fun `All five questions are defined`() {
        assertEquals(5, Questions.all.size)
        assertEquals(listOf(1, 2, 3, 4, 5), Questions.all.map { it.number })
    }

    @Test
    fun `Q1 and Q2 require 3 final picks across 2 rounds`() {
        listOf(1, 2).forEach { n ->
            val q = Questions.byNumber(n)
            assertEquals(3, q.requiredImageCount)
            assertEquals(2, q.selectionRounds)
        }
    }

    @Test
    fun `Q3 Q4 Q5 require 1 pick in 1 round`() {
        listOf(3, 4, 5).forEach { n ->
            val q = Questions.byNumber(n)
            assertEquals(1, q.requiredImageCount)
            assertEquals(1, q.selectionRounds)
        }
    }

    @Test
    fun `Final pick count across all questions is 9`() {
        assertEquals(9, Questions.all.sumOf { it.requiredImageCount })
    }
}
```

- [ ] **Step 4: Run, verify test fails**

Run: `cd mobile && ./gradlew :domain:test --tests "org.cru.soularium.domain.content.QuestionsTest"`
Expected: compilation failure, `Questions` unresolved.

- [ ] **Step 5: Write `Questions.kt`**

```kotlin
package org.cru.soularium.domain.content

object Questions {
    val all: List<Question> = listOf(
        Question(1, selectionRounds = 2, requiredImageCount = 3,
            promptKey = "q1_prompt", selectionKey = "q1_selection",
            finalizingKey = "q1_finalizing", discussionKey = "q1_discussion"),
        Question(2, selectionRounds = 2, requiredImageCount = 3,
            promptKey = "q2_prompt", selectionKey = "q2_selection",
            finalizingKey = "q2_finalizing", discussionKey = "q2_discussion"),
        Question(3, selectionRounds = 1, requiredImageCount = 1,
            promptKey = "q3_prompt", selectionKey = "q3_selection",
            finalizingKey = "q3_finalizing", discussionKey = "q3_discussion"),
        Question(4, selectionRounds = 1, requiredImageCount = 1,
            promptKey = "q4_prompt", selectionKey = "q4_selection",
            finalizingKey = "q4_finalizing", discussionKey = "q4_discussion"),
        Question(5, selectionRounds = 1, requiredImageCount = 1,
            promptKey = "q5_prompt", selectionKey = "q5_selection",
            finalizingKey = "q5_finalizing", discussionKey = "q5_discussion"),
    )

    fun byNumber(n: Int): Question = all.first { it.number == n }
}
```

- [ ] **Step 6: Run, verify tests pass**

Run: `cd mobile && ./gradlew :domain:test`
Expected: All tests pass.

- [ ] **Step 7: Commit**

```bash
git add mobile/domain/
git commit -m "feat(domain): add Question and CardImage content types"
```

---

### Task 9: DomainError sealed hierarchy

**Files:**
- Create: `mobile/domain/src/main/kotlin/org/cru/soularium/domain/DomainError.kt`

- [ ] **Step 1: Write `DomainError.kt`**

```kotlin
package org.cru.soularium.domain

sealed interface DomainError {
    data object PersistenceFailed : DomainError
    data class InvalidStateTransition(val from: String, val event: String) : DomainError
    data class InvalidSelectionCount(val expected: Int, val got: Int) : DomainError
    data object ShareUnavailable : DomainError
    data class ContentLoadFailed(val resource: String) : DomainError
}
```

- [ ] **Step 2: Commit**

```bash
git add mobile/domain/
git commit -m "feat(domain): add DomainError sealed hierarchy"
```

---

## Phase 2: Conversation State Machine

Goal: a pure, exhaustively tested `transition(state, event, ctx)` function.

### Task 10: SessionState and SessionEvent

**Files:**
- Create: `mobile/domain/src/main/kotlin/org/cru/soularium/domain/session/SessionState.kt`
- Create: `mobile/domain/src/main/kotlin/org/cru/soularium/domain/session/SessionEvent.kt`
- Create: `mobile/domain/src/main/kotlin/org/cru/soularium/domain/session/SessionContext.kt`
- Create: `mobile/domain/src/main/kotlin/org/cru/soularium/domain/session/Effect.kt`
- Create: `mobile/domain/src/main/kotlin/org/cru/soularium/domain/session/TransitionResult.kt`

- [ ] **Step 1: Write `SessionState.kt`**

```kotlin
package org.cru.soularium.domain.session

import kotlinx.serialization.Serializable

@Serializable
sealed interface SessionState {
    @Serializable
    data object NotStarted : SessionState

    @Serializable
    data object AddingParticipants : SessionState

    @Serializable
    data class InQuestion(
        val questionNumber: Int,
        val activeParticipantIndex: Int,
        val activity: QuestionActivity,
    ) : SessionState

    @Serializable
    data object Summary : SessionState

    @Serializable
    data class CollectingContact(val participantIndex: Int) : SessionState

    @Serializable
    data object Concluded : SessionState
}

@Serializable
enum class QuestionActivity {
    ShowingPrompt,
    ShowingInstructions,
    SelectingRound1,
    SelectingRound2,
    Finalizing,
    Discussing,
}
```

- [ ] **Step 2: Write `SessionEvent.kt`**

```kotlin
package org.cru.soularium.domain.session

import org.cru.soularium.domain.ContactInfo
import org.cru.soularium.domain.SessionKind

sealed interface SessionEvent {
    data class StartSession(val kind: SessionKind) : SessionEvent
    data class AddParticipant(val name: String) : SessionEvent
    data class RemoveParticipant(val index: Int) : SessionEvent
    data object ConfirmParticipants : SessionEvent

    data object BeginSelection : SessionEvent
    data object DismissInstructions : SessionEvent
    data class PickCard(val cardId: Int) : SessionEvent
    data class UnpickCard(val cardId: Int) : SessionEvent
    data object ConfirmSelection : SessionEvent
    data object ConfirmFinal : SessionEvent
    data object EndDiscussion : SessionEvent

    data class CollectContact(val participantIndex: Int, val info: ContactInfo) : SessionEvent
    data object SkipContact : SessionEvent
    data object Conclude : SessionEvent

    data object Bookmark : SessionEvent
}
```

- [ ] **Step 3: Write `SessionContext.kt`**

```kotlin
package org.cru.soularium.domain.session

data class SessionContext(
    val participantNames: List<String>,
    val currentDraftPicks: List<Int>,
    val currentRoundFinalPicks: List<Int>,
    val showInstructionsForThisSession: Boolean,
)
```

- [ ] **Step 4: Write `Effect.kt`**

```kotlin
package org.cru.soularium.domain.session

import org.cru.soularium.domain.ContactInfo

sealed interface Effect {
    data class PersistState(val state: SessionState) : Effect
    data class PersistParticipants(val names: List<String>) : Effect
    data class PersistPicks(val questionNumber: Int, val participantIndex: Int, val cardIds: List<Int>, val isFinal: Boolean) : Effect
    data class PersistContact(val participantIndex: Int, val info: ContactInfo) : Effect
    data class PersistBookmark(val bookmark: Boolean) : Effect
    data class LogAnalytics(val event: String, val params: Map<String, Any>) : Effect
}
```

- [ ] **Step 5: Write `TransitionResult.kt`**

```kotlin
package org.cru.soularium.domain.session

import org.cru.soularium.domain.DomainError

data class TransitionResult(
    val next: SessionState,
    val effects: List<Effect> = emptyList(),
    val error: DomainError? = null,
)
```

- [ ] **Step 6: Verify compilation**

Run: `cd mobile && ./gradlew :domain:compileKotlin`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```bash
git add mobile/domain/
git commit -m "feat(domain): add session state, events, effects, transition result"
```

---

### Task 11: Transition — Start and AddParticipants

**Files:**
- Create: `mobile/domain/src/main/kotlin/org/cru/soularium/domain/session/Transition.kt`
- Test: `mobile/domain/src/test/kotlin/org/cru/soularium/domain/session/TransitionStartTest.kt`

- [ ] **Step 1: Write the test**

```kotlin
package org.cru.soularium.domain.session

import org.cru.soularium.domain.SessionKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TransitionStartTest {
    private val emptyCtx = SessionContext(emptyList(), emptyList(), emptyList(), false)

    @Test
    fun `NotStarted plus StartSession goes to AddingParticipants`() {
        val result = transition(SessionState.NotStarted, SessionEvent.StartSession(SessionKind.SOLO), emptyCtx)
        assertEquals(SessionState.AddingParticipants, result.next)
        assertNull(result.error)
        assertTrue(result.effects.any { it is Effect.LogAnalytics })
    }

    @Test
    fun `AddingParticipants plus AddParticipant emits persist effect`() {
        val ctx = SessionContext(participantNames = listOf("Alice"), emptyList(), emptyList(), false)
        val result = transition(SessionState.AddingParticipants, SessionEvent.AddParticipant("Bob"), ctx)
        assertEquals(SessionState.AddingParticipants, result.next)
        val persist = result.effects.filterIsInstance<Effect.PersistParticipants>().singleOrNull()
        assertEquals(listOf("Alice", "Bob"), persist?.names)
    }

    @Test
    fun `ConfirmParticipants with at least one participant moves to InQuestion 1 ShowingPrompt`() {
        val ctx = SessionContext(participantNames = listOf("Alice"), emptyList(), emptyList(), false)
        val result = transition(SessionState.AddingParticipants, SessionEvent.ConfirmParticipants, ctx)
        val next = assertIs<SessionState.InQuestion>(result.next)
        assertEquals(1, next.questionNumber)
        assertEquals(0, next.activeParticipantIndex)
        assertEquals(QuestionActivity.ShowingPrompt, next.activity)
    }

    @Test
    fun `ConfirmParticipants with zero participants returns error`() {
        val ctx = SessionContext(participantNames = emptyList(), emptyList(), emptyList(), false)
        val result = transition(SessionState.AddingParticipants, SessionEvent.ConfirmParticipants, ctx)
        assertEquals(SessionState.AddingParticipants, result.next)
        assertEquals(0, result.effects.filterIsInstance<Effect.PersistState>().size)
        assertTrue(result.error != null)
    }
}
```

- [ ] **Step 2: Run, verify fail**

Run: `cd mobile && ./gradlew :domain:test --tests "org.cru.soularium.domain.session.TransitionStartTest"`
Expected: compilation failure.

- [ ] **Step 3: Write the initial `Transition.kt`**

```kotlin
package org.cru.soularium.domain.session

import org.cru.soularium.domain.DomainError
import org.cru.soularium.domain.content.Questions

fun transition(state: SessionState, event: SessionEvent, ctx: SessionContext): TransitionResult {
    return when (state) {
        SessionState.NotStarted -> transitionNotStarted(event)
        SessionState.AddingParticipants -> transitionAddingParticipants(event, ctx)
        is SessionState.InQuestion -> TransitionResult(
            next = state,
            error = DomainError.InvalidStateTransition(state.toString(), event.toString()),
        )
        SessionState.Summary, is SessionState.CollectingContact, SessionState.Concluded ->
            TransitionResult(
                next = state,
                error = DomainError.InvalidStateTransition(state.toString(), event.toString()),
            )
    }
}

private fun transitionNotStarted(event: SessionEvent): TransitionResult = when (event) {
    is SessionEvent.StartSession -> TransitionResult(
        next = SessionState.AddingParticipants,
        effects = listOf(
            Effect.PersistState(SessionState.AddingParticipants),
            Effect.LogAnalytics("session_started", mapOf("kind" to event.kind.name.lowercase())),
        ),
    )
    else -> TransitionResult(
        next = SessionState.NotStarted,
        error = DomainError.InvalidStateTransition("NotStarted", event.toString()),
    )
}

private fun transitionAddingParticipants(event: SessionEvent, ctx: SessionContext): TransitionResult = when (event) {
    is SessionEvent.AddParticipant -> {
        val names = ctx.participantNames + event.name
        TransitionResult(
            next = SessionState.AddingParticipants,
            effects = listOf(Effect.PersistParticipants(names)),
        )
    }
    is SessionEvent.RemoveParticipant -> {
        val names = ctx.participantNames.toMutableList().apply {
            if (event.index in indices) removeAt(event.index)
        }
        TransitionResult(
            next = SessionState.AddingParticipants,
            effects = listOf(Effect.PersistParticipants(names)),
        )
    }
    SessionEvent.ConfirmParticipants -> {
        if (ctx.participantNames.isEmpty()) {
            TransitionResult(
                next = SessionState.AddingParticipants,
                error = DomainError.InvalidStateTransition("AddingParticipants", "ConfirmParticipants(empty)"),
            )
        } else {
            val next = SessionState.InQuestion(1, 0, QuestionActivity.ShowingPrompt)
            TransitionResult(
                next = next,
                effects = listOf(Effect.PersistState(next)),
            )
        }
    }
    else -> TransitionResult(
        next = SessionState.AddingParticipants,
        error = DomainError.InvalidStateTransition("AddingParticipants", event.toString()),
    )
}
```

- [ ] **Step 4: Run, verify pass**

Run: `cd mobile && ./gradlew :domain:test --tests "org.cru.soularium.domain.session.TransitionStartTest"`
Expected: 4 tests pass.

- [ ] **Step 5: Commit**

```bash
git add mobile/domain/
git commit -m "feat(domain): transition function for start + add-participants"
```

---

### Task 12: Transition — InQuestion happy paths

**Files:**
- Modify: `mobile/domain/src/main/kotlin/org/cru/soularium/domain/session/Transition.kt`
- Test: `mobile/domain/src/test/kotlin/org/cru/soularium/domain/session/TransitionInQuestionTest.kt`

- [ ] **Step 1: Write the test**

```kotlin
package org.cru.soularium.domain.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class TransitionInQuestionTest {
    private fun ctx(picks: List<Int> = emptyList(), finals: List<Int> = emptyList(), names: List<String> = listOf("Alice")) =
        SessionContext(names, picks, finals, false)

    @Test
    fun `BeginSelection from ShowingPrompt advances to SelectingRound1`() {
        val state = SessionState.InQuestion(1, 0, QuestionActivity.ShowingPrompt)
        val r = transition(state, SessionEvent.BeginSelection, ctx())
        val next = assertIs<SessionState.InQuestion>(r.next)
        assertEquals(QuestionActivity.SelectingRound1, next.activity)
        assertNull(r.error)
    }

    @Test
    fun `Q1 round1 ConfirmSelection with at least 3 picks advances to SelectingRound2`() {
        val state = SessionState.InQuestion(1, 0, QuestionActivity.SelectingRound1)
        val r = transition(state, SessionEvent.ConfirmSelection, ctx(picks = listOf(1, 2, 3, 4, 5)))
        val next = assertIs<SessionState.InQuestion>(r.next)
        assertEquals(QuestionActivity.SelectingRound2, next.activity)
    }

    @Test
    fun `Q3 (one-round) ConfirmSelection with 1 pick advances to Finalizing`() {
        val state = SessionState.InQuestion(3, 0, QuestionActivity.SelectingRound1)
        val r = transition(state, SessionEvent.ConfirmSelection, ctx(picks = listOf(7)))
        val next = assertIs<SessionState.InQuestion>(r.next)
        assertEquals(QuestionActivity.Finalizing, next.activity)
    }

    @Test
    fun `Q1 round2 ConfirmSelection with exactly 3 picks advances to Finalizing`() {
        val state = SessionState.InQuestion(1, 0, QuestionActivity.SelectingRound2)
        val r = transition(state, SessionEvent.ConfirmSelection, ctx(picks = listOf(1, 2, 3)))
        val next = assertIs<SessionState.InQuestion>(r.next)
        assertEquals(QuestionActivity.Finalizing, next.activity)
    }

    @Test
    fun `ConfirmFinal advances to Discussing`() {
        val state = SessionState.InQuestion(2, 0, QuestionActivity.Finalizing)
        val r = transition(state, SessionEvent.ConfirmFinal, ctx(finals = listOf(1, 2, 3)))
        val next = assertIs<SessionState.InQuestion>(r.next)
        assertEquals(QuestionActivity.Discussing, next.activity)
        assertEquals(1, r.effects.filterIsInstance<Effect.PersistPicks>().single().let { it.isFinal.compareTo(false) })
    }

    @Test
    fun `EndDiscussion to next participant within same question`() {
        val state = SessionState.InQuestion(1, 0, QuestionActivity.Discussing)
        val r = transition(state, SessionEvent.EndDiscussion, ctx(names = listOf("Alice", "Bob")))
        val next = assertIs<SessionState.InQuestion>(r.next)
        assertEquals(1, next.questionNumber)
        assertEquals(1, next.activeParticipantIndex)
        assertEquals(QuestionActivity.ShowingPrompt, next.activity)
    }

    @Test
    fun `EndDiscussion last participant advances to next question`() {
        val state = SessionState.InQuestion(1, 0, QuestionActivity.Discussing)
        val r = transition(state, SessionEvent.EndDiscussion, ctx(names = listOf("Alice")))
        val next = assertIs<SessionState.InQuestion>(r.next)
        assertEquals(2, next.questionNumber)
        assertEquals(0, next.activeParticipantIndex)
        assertEquals(QuestionActivity.ShowingPrompt, next.activity)
    }

    @Test
    fun `EndDiscussion last participant after Q5 goes to Summary`() {
        val state = SessionState.InQuestion(5, 0, QuestionActivity.Discussing)
        val r = transition(state, SessionEvent.EndDiscussion, ctx(names = listOf("Alice")))
        assertEquals(SessionState.Summary, r.next)
    }
}
```

- [ ] **Step 2: Run, verify fail**

Run: `cd mobile && ./gradlew :domain:test --tests "org.cru.soularium.domain.session.TransitionInQuestionTest"`
Expected: 8 tests fail.

- [ ] **Step 3: Extend `Transition.kt`**

Replace the `is SessionState.InQuestion ->` branch in the top-level `transition` function with:

```kotlin
is SessionState.InQuestion -> transitionInQuestion(state, event, ctx)
```

And add at the bottom of the file:

```kotlin
private fun transitionInQuestion(
    state: SessionState.InQuestion,
    event: SessionEvent,
    ctx: SessionContext,
): TransitionResult {
    val question = Questions.byNumber(state.questionNumber)
    return when (event) {
        SessionEvent.BeginSelection -> {
            val targetActivity =
                if (ctx.showInstructionsForThisSession && state.activity == QuestionActivity.ShowingPrompt)
                    QuestionActivity.ShowingInstructions
                else
                    QuestionActivity.SelectingRound1
            val next = state.copy(activity = targetActivity)
            TransitionResult(next = next, effects = listOf(Effect.PersistState(next)))
        }

        SessionEvent.DismissInstructions -> {
            val next = state.copy(activity = QuestionActivity.SelectingRound1)
            TransitionResult(next = next, effects = listOf(Effect.PersistState(next)))
        }

        is SessionEvent.PickCard, is SessionEvent.UnpickCard ->
            TransitionResult(next = state) // selection is owned by the UI / draft state; transition is a no-op here

        SessionEvent.ConfirmSelection -> {
            val targetActivity = when (state.activity) {
                QuestionActivity.SelectingRound1 -> {
                    val needed = if (question.selectionRounds == 2) question.requiredImageCount + 1 else question.requiredImageCount
                    if (ctx.currentDraftPicks.size < needed) {
                        return TransitionResult(
                            next = state,
                            error = org.cru.soularium.domain.DomainError.InvalidSelectionCount(needed, ctx.currentDraftPicks.size),
                        )
                    }
                    if (question.selectionRounds == 2) QuestionActivity.SelectingRound2 else QuestionActivity.Finalizing
                }
                QuestionActivity.SelectingRound2 -> {
                    if (ctx.currentDraftPicks.size != question.requiredImageCount) {
                        return TransitionResult(
                            next = state,
                            error = org.cru.soularium.domain.DomainError.InvalidSelectionCount(question.requiredImageCount, ctx.currentDraftPicks.size),
                        )
                    }
                    QuestionActivity.Finalizing
                }
                else -> return TransitionResult(
                    next = state,
                    error = org.cru.soularium.domain.DomainError.InvalidStateTransition(state.toString(), event.toString()),
                )
            }
            val next = state.copy(activity = targetActivity)
            val effects = mutableListOf<Effect>(Effect.PersistState(next))
            effects += Effect.PersistPicks(
                questionNumber = state.questionNumber,
                participantIndex = state.activeParticipantIndex,
                cardIds = ctx.currentDraftPicks,
                isFinal = targetActivity == QuestionActivity.Finalizing,
            )
            TransitionResult(next = next, effects = effects)
        }

        SessionEvent.ConfirmFinal -> {
            if (ctx.currentRoundFinalPicks.size != question.requiredImageCount && ctx.currentDraftPicks.size != question.requiredImageCount) {
                return TransitionResult(
                    next = state,
                    error = org.cru.soularium.domain.DomainError.InvalidSelectionCount(question.requiredImageCount, ctx.currentDraftPicks.size),
                )
            }
            val next = state.copy(activity = QuestionActivity.Discussing)
            TransitionResult(
                next = next,
                effects = listOf(
                    Effect.PersistState(next),
                    Effect.PersistPicks(
                        questionNumber = state.questionNumber,
                        participantIndex = state.activeParticipantIndex,
                        cardIds = ctx.currentDraftPicks.ifEmpty { ctx.currentRoundFinalPicks },
                        isFinal = true,
                    ),
                    Effect.LogAnalytics("question_completed", mapOf(
                        "question_number" to state.questionNumber,
                        "participant_index" to state.activeParticipantIndex,
                        "picks_count" to question.requiredImageCount,
                    )),
                ),
            )
        }

        SessionEvent.EndDiscussion -> {
            val isLastParticipant = state.activeParticipantIndex >= ctx.participantNames.size - 1
            val next = when {
                !isLastParticipant -> state.copy(
                    activeParticipantIndex = state.activeParticipantIndex + 1,
                    activity = QuestionActivity.ShowingPrompt,
                )
                state.questionNumber < 5 -> SessionState.InQuestion(
                    questionNumber = state.questionNumber + 1,
                    activeParticipantIndex = 0,
                    activity = QuestionActivity.ShowingPrompt,
                )
                else -> SessionState.Summary
            }
            TransitionResult(next = next, effects = listOf(Effect.PersistState(next)))
        }

        SessionEvent.Bookmark -> TransitionResult(
            next = state,
            effects = listOf(Effect.PersistBookmark(true)),
        )

        else -> TransitionResult(
            next = state,
            error = org.cru.soularium.domain.DomainError.InvalidStateTransition(state.toString(), event.toString()),
        )
    }
}
```

- [ ] **Step 4: Run, verify all tests pass**

Run: `cd mobile && ./gradlew :domain:test`
Expected: all tests pass.

- [ ] **Step 5: Commit**

```bash
git add mobile/domain/
git commit -m "feat(domain): transition for in-question happy paths"
```

---

### Task 13: Transition — Summary, Contact, Conclude, and Bookmark from any state

**Files:**
- Modify: `mobile/domain/src/main/kotlin/org/cru/soularium/domain/session/Transition.kt`
- Test: `mobile/domain/src/test/kotlin/org/cru/soularium/domain/session/TransitionEndTest.kt`

- [ ] **Step 1: Write the test**

```kotlin
package org.cru.soularium.domain.session

import org.cru.soularium.domain.ContactInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TransitionEndTest {
    private val ctx = SessionContext(listOf("Alice", "Bob"), emptyList(), emptyList(), false)

    @Test
    fun `Summary plus CollectContact zero advances to CollectingContact 0`() {
        val info = ContactInfo("Alice", email = "alice@example.com")
        val r = transition(SessionState.Summary, SessionEvent.CollectContact(0, info), ctx)
        val next = assertIs<SessionState.CollectingContact>(r.next)
        assertEquals(0, next.participantIndex)
    }

    @Test
    fun `CollectingContact plus CollectContact next index moves index forward`() {
        val info = ContactInfo("Bob", email = "bob@example.com")
        val r = transition(SessionState.CollectingContact(0), SessionEvent.CollectContact(1, info), ctx)
        val next = assertIs<SessionState.CollectingContact>(r.next)
        assertEquals(1, next.participantIndex)
    }

    @Test
    fun `CollectingContact last participant plus Conclude goes to Concluded`() {
        val r = transition(SessionState.CollectingContact(1), SessionEvent.Conclude, ctx)
        assertEquals(SessionState.Concluded, r.next)
    }

    @Test
    fun `Bookmark works from InQuestion`() {
        val state = SessionState.InQuestion(2, 0, QuestionActivity.Discussing)
        val r = transition(state, SessionEvent.Bookmark, ctx)
        assertEquals(state, r.next)
        assertEquals(1, r.effects.filterIsInstance<Effect.PersistBookmark>().count { it.bookmark })
    }
}
```

- [ ] **Step 2: Run, verify fail**

Run: `cd mobile && ./gradlew :domain:test --tests "org.cru.soularium.domain.session.TransitionEndTest"`

- [ ] **Step 3: Extend the top-level `transition` function**

Replace the `SessionState.Summary, is SessionState.CollectingContact, SessionState.Concluded ->` branch in the top-level `transition` function with:

```kotlin
SessionState.Summary -> transitionSummary(event)
is SessionState.CollectingContact -> transitionCollectingContact(state, event, ctx)
SessionState.Concluded -> TransitionResult(
    next = SessionState.Concluded,
    error = DomainError.InvalidStateTransition("Concluded", event.toString()),
)
```

Add at the bottom of the file:

```kotlin
private fun transitionSummary(event: SessionEvent): TransitionResult = when (event) {
    is SessionEvent.CollectContact -> {
        val next = SessionState.CollectingContact(event.participantIndex)
        TransitionResult(next = next, effects = listOf(
            Effect.PersistState(next),
            Effect.PersistContact(event.participantIndex, event.info),
        ))
    }
    SessionEvent.SkipContact -> TransitionResult(
        next = SessionState.Concluded,
        effects = listOf(Effect.PersistState(SessionState.Concluded)),
    )
    SessionEvent.Conclude -> TransitionResult(
        next = SessionState.Concluded,
        effects = listOf(
            Effect.PersistState(SessionState.Concluded),
            Effect.LogAnalytics("session_completed", emptyMap()),
        ),
    )
    SessionEvent.Bookmark -> TransitionResult(
        next = SessionState.Summary,
        effects = listOf(Effect.PersistBookmark(true)),
    )
    else -> TransitionResult(
        next = SessionState.Summary,
        error = DomainError.InvalidStateTransition("Summary", event.toString()),
    )
}

private fun transitionCollectingContact(
    state: SessionState.CollectingContact,
    event: SessionEvent,
    ctx: SessionContext,
): TransitionResult = when (event) {
    is SessionEvent.CollectContact -> {
        val nextIndex = event.participantIndex
        val next = SessionState.CollectingContact(nextIndex)
        TransitionResult(
            next = next,
            effects = listOf(
                Effect.PersistState(next),
                Effect.PersistContact(event.participantIndex, event.info),
            ),
        )
    }
    SessionEvent.SkipContact -> {
        val nextIndex = state.participantIndex + 1
        if (nextIndex >= ctx.participantNames.size) {
            TransitionResult(
                next = SessionState.Concluded,
                effects = listOf(Effect.PersistState(SessionState.Concluded)),
            )
        } else {
            val next = SessionState.CollectingContact(nextIndex)
            TransitionResult(next = next, effects = listOf(Effect.PersistState(next)))
        }
    }
    SessionEvent.Conclude -> TransitionResult(
        next = SessionState.Concluded,
        effects = listOf(
            Effect.PersistState(SessionState.Concluded),
            Effect.LogAnalytics("session_completed", emptyMap()),
        ),
    )
    SessionEvent.Bookmark -> TransitionResult(
        next = state,
        effects = listOf(Effect.PersistBookmark(true)),
    )
    else -> TransitionResult(
        next = state,
        error = DomainError.InvalidStateTransition(state.toString(), event.toString()),
    )
}
```

- [ ] **Step 4: Run, verify tests pass**

Run: `cd mobile && ./gradlew :domain:test`
Expected: all tests pass.

- [ ] **Step 5: Commit**

```bash
git add mobile/domain/
git commit -m "feat(domain): transitions for Summary, Contact, Concluded, Bookmark"
```

---

## Phase 3: Pure utility functions

### Task 14: Share URL generation

**Files:**
- Create: `mobile/domain/src/main/kotlin/org/cru/soularium/domain/share/ShareUrl.kt`
- Test: `mobile/domain/src/test/kotlin/org/cru/soularium/domain/share/ShareUrlTest.kt`

- [ ] **Step 1: Write the test**

```kotlin
package org.cru.soularium.domain.share

import org.cru.soularium.domain.CardPick
import org.cru.soularium.domain.CardPickId
import org.cru.soularium.domain.ContactInfo
import org.cru.soularium.domain.Conversation
import org.cru.soularium.domain.ConversationId
import org.cru.soularium.domain.SessionId
import kotlin.test.Test
import kotlin.test.assertEquals

class ShareUrlTest {
    private val conv = Conversation(
        id = ConversationId("c-1"),
        sessionId = SessionId("s-1"),
        displayOrder = 0,
        contact = ContactInfo(name = "John"),
    )

    private fun pick(q: Int, card: Int, order: Int) = CardPick(
        id = CardPickId("p-$q-$card"),
        conversationId = ConversationId("c-1"),
        questionNumber = q,
        cardId = card,
        pickOrder = order,
        isFinal = true,
    )

    @Test
    fun `Share URL has 9 cards in q1q2q3q4q5 order`() {
        val picks = listOf(
            pick(1, 5, 0), pick(1, 12, 1), pick(1, 33, 2),
            pick(2, 7, 0), pick(2, 18, 1), pick(2, 41, 2),
            pick(3, 22, 0),
            pick(4, 9, 0),
            pick(5, 50, 0),
        )
        val url = shareUrlFor(conv, picks)
        assertEquals("https://mysoularium.com/my-life-in-pictures/?images=5,12,33,7,18,41,22,9,50&person=John", url)
    }

    @Test
    fun `Name is URL-encoded`() {
        val picks = (1..5).flatMap { q ->
            val count = if (q <= 2) 3 else 1
            (1..count).map { pick(q, q * 10 + it, it - 1) }
        }
        val withSpace = conv.copy(contact = ContactInfo(name = "Mary Jane"))
        val url = shareUrlFor(withSpace, picks)
        assertEquals("Mary%20Jane", url.substringAfter("person="))
    }

    @Test
    fun `Round-1 picks ignored, only final picks included`() {
        val picks = listOf(
            pick(1, 5, 0), pick(1, 12, 1), pick(1, 33, 2),
        ) + listOf(
            CardPick(CardPickId("nonfinal"), ConversationId("c-1"), 1, 99, 99, isFinal = false),
        )
        val url = shareUrlFor(conv, picks + (2..5).flatMap { q ->
            val count = if (q <= 2) 3 else 1
            (1..count).map { pick(q, q * 10 + it, it - 1) }
        })
        assert("99" !in url)
    }
}
```

- [ ] **Step 2: Run, verify fail**

Run: `cd mobile && ./gradlew :domain:test --tests "org.cru.soularium.domain.share.ShareUrlTest"`

- [ ] **Step 3: Write `ShareUrl.kt`**

```kotlin
package org.cru.soularium.domain.share

import org.cru.soularium.domain.CardPick
import org.cru.soularium.domain.Conversation

fun shareUrlFor(conversation: Conversation, picks: List<CardPick>): String {
    val finals = picks.filter { it.isFinal }
    val orderedCardIds = (1..5).flatMap { q ->
        finals.filter { it.questionNumber == q }
            .sortedBy { it.pickOrder }
            .map { it.cardId }
    }
    val name = urlEncode(conversation.contact.name)
    return "https://mysoularium.com/my-life-in-pictures/?images=${orderedCardIds.joinToString(",")}&person=$name"
}

private fun urlEncode(s: String): String = buildString {
    for (c in s) {
        when {
            c.isLetterOrDigit() || c in "-_.~" -> append(c)
            c == ' ' -> append("%20")
            else -> {
                val bytes = c.toString().encodeToByteArray()
                for (b in bytes) {
                    append('%')
                    append(((b.toInt() and 0xff) shr 4).toString(16).uppercase())
                    append((b.toInt() and 0x0f).toString(16).uppercase())
                }
            }
        }
    }
}
```

- [ ] **Step 4: Run, verify pass**

Run: `cd mobile && ./gradlew :domain:test`

- [ ] **Step 5: Commit**

```bash
git add mobile/domain/
git commit -m "feat(domain): pure shareUrlFor function"
```

---

### Task 15: Repository interfaces

**Files:**
- Create: `mobile/domain/src/main/kotlin/org/cru/soularium/domain/ports/SessionRepository.kt`
- Create: `mobile/domain/src/main/kotlin/org/cru/soularium/domain/ports/ContentRepository.kt`
- Create: `mobile/domain/src/main/kotlin/org/cru/soularium/domain/ports/AnalyticsTracker.kt`
- Create: `mobile/domain/src/main/kotlin/org/cru/soularium/domain/ports/CrashReporter.kt`
- Create: `mobile/domain/src/main/kotlin/org/cru/soularium/domain/ports/Sharer.kt`

- [ ] **Step 1: Write `SessionRepository.kt`**

```kotlin
package org.cru.soularium.domain.ports

import kotlinx.coroutines.flow.Flow
import org.cru.soularium.domain.CardPick
import org.cru.soularium.domain.ContactInfo
import org.cru.soularium.domain.Conversation
import org.cru.soularium.domain.ConversationId
import org.cru.soularium.domain.Session
import org.cru.soularium.domain.SessionId
import org.cru.soularium.domain.session.SessionState

interface SessionRepository {
    suspend fun createSession(session: Session, initialState: SessionState): SessionId
    suspend fun loadSession(id: SessionId): Session?
    suspend fun loadState(id: SessionId): SessionState?
    suspend fun persistState(id: SessionId, state: SessionState)
    suspend fun setBookmarked(id: SessionId, bookmarked: Boolean)
    suspend fun setEnded(id: SessionId)

    suspend fun upsertParticipants(sessionId: SessionId, names: List<String>): List<ConversationId>
    suspend fun upsertContact(conversationId: ConversationId, info: ContactInfo)

    suspend fun upsertPicks(conversationId: ConversationId, questionNumber: Int, cardIds: List<Int>, isFinal: Boolean)
    suspend fun loadPicks(conversationId: ConversationId): List<CardPick>

    fun observeCompletedSessions(): Flow<List<Session>>
    fun observeBookmarkedSessions(): Flow<List<Session>>
    suspend fun deleteSession(id: SessionId)

    suspend fun loadConversations(sessionId: SessionId): List<Conversation>
}
```

- [ ] **Step 2: Write `ContentRepository.kt`**

```kotlin
package org.cru.soularium.domain.ports

import org.cru.soularium.domain.content.CardImage
import org.cru.soularium.domain.content.Question

interface ContentRepository {
    fun questions(): List<Question>
    fun cards(): List<CardImage>
}
```

- [ ] **Step 3: Write `AnalyticsTracker.kt`**

```kotlin
package org.cru.soularium.domain.ports

interface AnalyticsTracker {
    fun screenView(screenName: String)
    fun event(name: String, params: Map<String, Any> = emptyMap())
}
```

- [ ] **Step 4: Write `CrashReporter.kt`**

```kotlin
package org.cru.soularium.domain.ports

interface CrashReporter {
    fun recordNonFatal(throwable: Throwable, breadcrumb: String? = null)
    fun setKey(key: String, value: String)
}
```

- [ ] **Step 5: Write `Sharer.kt`**

```kotlin
package org.cru.soularium.domain.ports

interface Sharer {
    suspend fun share(text: String, subject: String? = null): ShareResult
}

sealed interface ShareResult {
    data object Succeeded : ShareResult
    data object Cancelled : ShareResult
    data object NoAppAvailable : ShareResult
}
```

- [ ] **Step 6: Commit**

```bash
git add mobile/domain/
git commit -m "feat(domain): add port interfaces for repositories, analytics, sharer"
```

---

## Phase 4: Data — Room schema

### Task 16: Room entities and converters

**Files:**
- Create: `mobile/data/src/commonMain/kotlin/org/cru/soularium/data/db/entities/SessionEntity.kt`
- Create: `mobile/data/src/commonMain/kotlin/org/cru/soularium/data/db/entities/ConversationEntity.kt`
- Create: `mobile/data/src/commonMain/kotlin/org/cru/soularium/data/db/entities/CardPickEntity.kt`
- Create: `mobile/data/src/commonMain/kotlin/org/cru/soularium/data/db/Converters.kt`

- [ ] **Step 1: Write `SessionEntity.kt`**

```kotlin
package org.cru.soularium.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "kind") val kind: String, // SOLO|GROUP
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long?,
    @ColumnInfo(name = "bookmarked_at") val bookmarkedAt: Long?,
    @ColumnInfo(name = "state_snapshot_json") val stateSnapshotJson: String,
    @ColumnInfo(name = "selection_instructions_shown") val selectionInstructionsShown: Boolean,
)
```

- [ ] **Step 2: Write `ConversationEntity.kt`**

```kotlin
package org.cru.soularium.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversations",
    foreignKeys = [ForeignKey(
        entity = SessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["session_id"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("session_id")],
)
data class ConversationEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "display_order") val displayOrder: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "surname") val surname: String?,
    @ColumnInfo(name = "email") val email: String?,
    @ColumnInfo(name = "phone") val phone: String?,
    @ColumnInfo(name = "notes") val notes: String?,
)
```

- [ ] **Step 3: Write `CardPickEntity.kt`**

```kotlin
package org.cru.soularium.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "card_picks",
    foreignKeys = [ForeignKey(
        entity = ConversationEntity::class,
        parentColumns = ["id"],
        childColumns = ["conversation_id"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("conversation_id"), Index(value = ["conversation_id", "question_number"])],
)
data class CardPickEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "conversation_id") val conversationId: String,
    @ColumnInfo(name = "question_number") val questionNumber: Int,
    @ColumnInfo(name = "card_id") val cardId: Int,
    @ColumnInfo(name = "pick_order") val pickOrder: Int,
    @ColumnInfo(name = "is_final") val isFinal: Boolean,
)
```

- [ ] **Step 4: Verify compilation**

Run: `cd mobile && ./gradlew :data:compileKotlinMetadata`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add mobile/data/
git commit -m "feat(data): Room entities for sessions, conversations, picks"
```

---

### Task 17: Room DAOs and database

**Files:**
- Create: `mobile/data/src/commonMain/kotlin/org/cru/soularium/data/db/SessionDao.kt`
- Create: `mobile/data/src/commonMain/kotlin/org/cru/soularium/data/db/ConversationDao.kt`
- Create: `mobile/data/src/commonMain/kotlin/org/cru/soularium/data/db/CardPickDao.kt`
- Create: `mobile/data/src/commonMain/kotlin/org/cru/soularium/data/db/SoulariumDatabase.kt`
- Create: `mobile/data/src/androidMain/kotlin/org/cru/soularium/data/db/DatabaseBuilder.android.kt`
- Create: `mobile/data/src/iosMain/kotlin/org/cru/soularium/data/db/DatabaseBuilder.ios.kt`
- Create: `mobile/data/src/commonMain/kotlin/org/cru/soularium/data/db/DatabaseBuilder.kt`

- [ ] **Step 1: Write `SessionDao.kt`**

```kotlin
package org.cru.soularium.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.cru.soularium.data.db.entities.SessionEntity

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: SessionEntity)

    @Update
    suspend fun update(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun byId(id: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE ended_at IS NOT NULL ORDER BY ended_at DESC")
    fun observeCompleted(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE bookmarked_at IS NOT NULL ORDER BY bookmarked_at DESC")
    fun observeBookmarked(): Flow<List<SessionEntity>>

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun delete(id: String)
}
```

- [ ] **Step 2: Write `ConversationDao.kt`**

```kotlin
package org.cru.soularium.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.cru.soularium.data.db.entities.ConversationEntity

@Dao
interface ConversationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(conversation: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(conversations: List<ConversationEntity>)

    @Query("SELECT * FROM conversations WHERE session_id = :sessionId ORDER BY display_order")
    suspend fun forSession(sessionId: String): List<ConversationEntity>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun byId(id: String): ConversationEntity?

    @Query("DELETE FROM conversations WHERE session_id = :sessionId")
    suspend fun deleteForSession(sessionId: String)
}
```

- [ ] **Step 3: Write `CardPickDao.kt`**

```kotlin
package org.cru.soularium.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.cru.soularium.data.db.entities.CardPickEntity

@Dao
interface CardPickDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(picks: List<CardPickEntity>)

    @Query("SELECT * FROM card_picks WHERE conversation_id = :conversationId ORDER BY question_number, pick_order")
    suspend fun forConversation(conversationId: String): List<CardPickEntity>

    @Query("DELETE FROM card_picks WHERE conversation_id = :conversationId AND question_number = :questionNumber AND is_final = :isFinal")
    suspend fun deleteForRound(conversationId: String, questionNumber: Int, isFinal: Boolean)
}
```

- [ ] **Step 4: Write `SoulariumDatabase.kt`**

```kotlin
package org.cru.soularium.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import org.cru.soularium.data.db.entities.CardPickEntity
import org.cru.soularium.data.db.entities.ConversationEntity
import org.cru.soularium.data.db.entities.SessionEntity

@Database(
    entities = [SessionEntity::class, ConversationEntity::class, CardPickEntity::class],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(SoulariumDatabaseConstructor::class)
abstract class SoulariumDatabase : RoomDatabase() {
    abstract fun sessions(): SessionDao
    abstract fun conversations(): ConversationDao
    abstract fun cardPicks(): CardPickDao
}

expect object SoulariumDatabaseConstructor : RoomDatabaseConstructor<SoulariumDatabase> {
    override fun initialize(): SoulariumDatabase
}
```

- [ ] **Step 5: Write platform-specific database builders**

`mobile/data/src/commonMain/kotlin/org/cru/soularium/data/db/DatabaseBuilder.kt`:
```kotlin
package org.cru.soularium.data.db

import androidx.room.RoomDatabase

expect fun getDatabaseBuilder(): RoomDatabase.Builder<SoulariumDatabase>
```

`mobile/data/src/androidMain/kotlin/org/cru/soularium/data/db/DatabaseBuilder.android.kt`:
```kotlin
package org.cru.soularium.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

private lateinit var appContext: Context

fun initDataAndroid(context: Context) {
    appContext = context.applicationContext
}

actual fun getDatabaseBuilder(): RoomDatabase.Builder<SoulariumDatabase> {
    val dbFile = appContext.getDatabasePath("soularium.db")
    return Room.databaseBuilder<SoulariumDatabase>(
        context = appContext,
        name = dbFile.absolutePath,
    )
}
```

`mobile/data/src/iosMain/kotlin/org/cru/soularium/data/db/DatabaseBuilder.ios.kt`:
```kotlin
package org.cru.soularium.data.db

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
actual fun getDatabaseBuilder(): RoomDatabase.Builder<SoulariumDatabase> {
    val dbFile = documentDirectory() + "/soularium.db"
    return Room.databaseBuilder<SoulariumDatabase>(name = dbFile)
}

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(documentDirectory?.path)
}
```

- [ ] **Step 6: Verify compilation**

Run: `cd mobile && ./gradlew :data:compileKotlinMetadata :data:compileDebugKotlinAndroid`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```bash
git add mobile/data/
git commit -m "feat(data): Room DAOs and database with platform builders"
```

---

### Task 18: SessionRepository implementation

**Files:**
- Create: `mobile/data/src/commonMain/kotlin/org/cru/soularium/data/repository/SessionRepositoryImpl.kt`

- [ ] **Step 1: Write the implementation**

```kotlin
package org.cru.soularium.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.cru.soularium.data.db.CardPickDao
import org.cru.soularium.data.db.ConversationDao
import org.cru.soularium.data.db.SessionDao
import org.cru.soularium.data.db.entities.CardPickEntity
import org.cru.soularium.data.db.entities.ConversationEntity
import org.cru.soularium.data.db.entities.SessionEntity
import org.cru.soularium.domain.CardPick
import org.cru.soularium.domain.CardPickId
import org.cru.soularium.domain.ContactInfo
import org.cru.soularium.domain.Conversation
import org.cru.soularium.domain.ConversationId
import org.cru.soularium.domain.Session
import org.cru.soularium.domain.SessionId
import org.cru.soularium.domain.SessionKind
import org.cru.soularium.domain.ports.SessionRepository
import org.cru.soularium.domain.session.SessionState

class SessionRepositoryImpl(
    private val sessionDao: SessionDao,
    private val conversationDao: ConversationDao,
    private val cardPickDao: CardPickDao,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : SessionRepository {

    override suspend fun createSession(session: Session, initialState: SessionState): SessionId {
        sessionDao.upsert(session.toEntity(initialState))
        return session.id
    }

    override suspend fun loadSession(id: SessionId): Session? =
        sessionDao.byId(id.value)?.toDomain()

    override suspend fun loadState(id: SessionId): SessionState? =
        sessionDao.byId(id.value)?.let { json.decodeFromString(it.stateSnapshotJson) }

    override suspend fun persistState(id: SessionId, state: SessionState) {
        val current = sessionDao.byId(id.value) ?: return
        sessionDao.upsert(current.copy(stateSnapshotJson = json.encodeToString(state)))
    }

    override suspend fun setBookmarked(id: SessionId, bookmarked: Boolean) {
        val current = sessionDao.byId(id.value) ?: return
        sessionDao.upsert(current.copy(bookmarkedAt = if (bookmarked) Clock.System.now().toEpochMilliseconds() else null))
    }

    override suspend fun setEnded(id: SessionId) {
        val current = sessionDao.byId(id.value) ?: return
        sessionDao.upsert(current.copy(endedAt = Clock.System.now().toEpochMilliseconds()))
    }

    override suspend fun upsertParticipants(sessionId: SessionId, names: List<String>): List<ConversationId> {
        val existing = conversationDao.forSession(sessionId.value)
        val keptIds = mutableListOf<ConversationId>()
        names.forEachIndexed { index, name ->
            val match = existing.find { it.displayOrder == index }
            val id = match?.id ?: ConversationId.random().value
            keptIds += ConversationId(id)
            conversationDao.upsert(
                ConversationEntity(
                    id = id,
                    sessionId = sessionId.value,
                    displayOrder = index,
                    name = name,
                    surname = match?.surname,
                    email = match?.email,
                    phone = match?.phone,
                    notes = match?.notes,
                ),
            )
        }
        return keptIds
    }

    override suspend fun upsertContact(conversationId: ConversationId, info: ContactInfo) {
        val current = conversationDao.byId(conversationId.value) ?: return
        conversationDao.upsert(
            current.copy(
                name = info.name,
                surname = info.surname,
                email = info.email,
                phone = info.phone,
                notes = info.notes,
            ),
        )
    }

    override suspend fun upsertPicks(
        conversationId: ConversationId,
        questionNumber: Int,
        cardIds: List<Int>,
        isFinal: Boolean,
    ) {
        cardPickDao.deleteForRound(conversationId.value, questionNumber, isFinal)
        val entities = cardIds.mapIndexed { i, cid ->
            CardPickEntity(
                id = CardPickId.random().value,
                conversationId = conversationId.value,
                questionNumber = questionNumber,
                cardId = cid,
                pickOrder = i,
                isFinal = isFinal,
            )
        }
        cardPickDao.upsertAll(entities)
    }

    override suspend fun loadPicks(conversationId: ConversationId): List<CardPick> =
        cardPickDao.forConversation(conversationId.value).map { it.toDomain() }

    override fun observeCompletedSessions(): Flow<List<Session>> =
        sessionDao.observeCompleted().map { it.map { e -> e.toDomain() } }

    override fun observeBookmarkedSessions(): Flow<List<Session>> =
        sessionDao.observeBookmarked().map { it.map { e -> e.toDomain() } }

    override suspend fun deleteSession(id: SessionId) {
        sessionDao.delete(id.value)
    }

    override suspend fun loadConversations(sessionId: SessionId): List<Conversation> =
        conversationDao.forSession(sessionId.value).map { it.toDomain() }

    private fun Session.toEntity(state: SessionState) = SessionEntity(
        id = id.value,
        kind = kind.name,
        startedAt = startedAt.toEpochMilliseconds(),
        endedAt = endedAt?.toEpochMilliseconds(),
        bookmarkedAt = bookmarkedAt?.toEpochMilliseconds(),
        stateSnapshotJson = json.encodeToString(state),
        selectionInstructionsShown = selectionInstructionsShown,
    )

    private fun SessionEntity.toDomain() = Session(
        id = SessionId(id),
        kind = SessionKind.valueOf(kind),
        startedAt = Instant.fromEpochMilliseconds(startedAt),
        endedAt = endedAt?.let(Instant::fromEpochMilliseconds),
        bookmarkedAt = bookmarkedAt?.let(Instant::fromEpochMilliseconds),
        selectionInstructionsShown = selectionInstructionsShown,
    )

    private fun ConversationEntity.toDomain() = Conversation(
        id = ConversationId(id),
        sessionId = SessionId(sessionId),
        displayOrder = displayOrder,
        contact = ContactInfo(name, surname, email, phone, notes),
    )

    private fun CardPickEntity.toDomain() = CardPick(
        id = CardPickId(id),
        conversationId = ConversationId(conversationId),
        questionNumber = questionNumber,
        cardId = cardId,
        pickOrder = pickOrder,
        isFinal = isFinal,
    )
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd mobile && ./gradlew :data:compileKotlinMetadata`

- [ ] **Step 3: Commit**

```bash
git add mobile/data/
git commit -m "feat(data): SessionRepositoryImpl backed by Room"
```

---

### Task 19: ContentRepository implementation (in-memory)

**Files:**
- Create: `mobile/data/src/commonMain/kotlin/org/cru/soularium/data/repository/ContentRepositoryImpl.kt`

- [ ] **Step 1: Write the implementation**

```kotlin
package org.cru.soularium.data.repository

import org.cru.soularium.domain.content.CardImage
import org.cru.soularium.domain.content.Question
import org.cru.soularium.domain.content.Questions
import org.cru.soularium.domain.ports.ContentRepository

class ContentRepositoryImpl : ContentRepository {
    override fun questions(): List<Question> = Questions.all
    override fun cards(): List<CardImage> = (1..50).map { CardImage(it) }
}
```

- [ ] **Step 2: Commit**

```bash
git add mobile/data/
git commit -m "feat(data): ContentRepositoryImpl with bundled 50 cards + 5 questions"
```

---

## Phase 5: Compose Theme, Resources, and Atomic UI

### Task 20: Migrate 50 card images from the Android repo

**Files:**
- Create: `mobile/composeApp/src/commonMain/composeResources/drawable/card_01.jpg` through `card_50.jpg`
- Create: `mobile/composeApp/src/commonMain/composeResources/drawable/card_01_thumb.jpg` through `card_50_thumb.jpg`

- [ ] **Step 1: Copy cards from the Android repo**

```bash
mkdir -p mobile/composeApp/src/commonMain/composeResources/drawable
cp /Users/danielbisgrove/Documents/Web_Dev/soularium-android/app/src/main/res/drawable/card_*.jpg \
   /Users/danielbisgrove/Documents/Web_Dev/soularium-android/app/src/main/res/drawable/card_*_thumb.png \
   mobile/composeApp/src/commonMain/composeResources/drawable/ 2>/dev/null || true
```

- [ ] **Step 2: Verify there are 50 card files + 50 thumb files**

Run:
```bash
ls mobile/composeApp/src/commonMain/composeResources/drawable/card_*.jpg | wc -l
ls mobile/composeApp/src/commonMain/composeResources/drawable/card_*_thumb.* | wc -l
```
Expected: 50, 50.

- [ ] **Step 3: Commit**

```bash
git add mobile/composeApp/src/commonMain/composeResources/drawable/
git commit -m "assets: import 50 card images from legacy Android repo"
```

---

### Task 21: Open Sans font and theme

**Files:**
- Create: `mobile/composeApp/src/commonMain/composeResources/font/OpenSans-Regular.ttf` and weights
- Create: `mobile/composeApp/src/commonMain/kotlin/org/cru/soularium/ui/theme/Color.kt`
- Create: `mobile/composeApp/src/commonMain/kotlin/org/cru/soularium/ui/theme/Typography.kt`
- Create: `mobile/composeApp/src/commonMain/kotlin/org/cru/soularium/ui/theme/Theme.kt`

- [ ] **Step 1: Copy Open Sans fonts from the Android repo**

```bash
mkdir -p mobile/composeApp/src/commonMain/composeResources/font
cp /Users/danielbisgrove/Documents/Web_Dev/soularium-android/app/src/main/assets/fonts/OpenSans*.ttf \
   mobile/composeApp/src/commonMain/composeResources/font/
ls mobile/composeApp/src/commonMain/composeResources/font/
```
Expected: at least Regular, Bold, Light, SemiBold (plus italics if available).

- [ ] **Step 2: Write `Color.kt`**

```kotlin
package org.cru.soularium.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val SoulariumOrange = Color(0xFFF05D2C)
val SoulariumOrangeLight = Color(0xFFF27619)
val SoulariumDark = Color(0xFF1A1A1A)
val SoulariumBackground = Color(0xFFECEAEB)
val SoulariumSurface = Color(0xFFFFFFFF)
val SoulariumOnSurface = Color(0xFF1A1A1A)

val QuestionProgressColors = listOf(
    Color(0xFF17BD97),
    Color(0xFF16A986),
    Color(0xFF1C9AA4),
    Color(0xFF25A9C4),
    Color(0xFF1680BD),
)

val SoulariumLightColors = lightColorScheme(
    primary = SoulariumOrange,
    onPrimary = Color.White,
    secondary = SoulariumOrangeLight,
    onSecondary = Color.White,
    background = SoulariumBackground,
    onBackground = SoulariumOnSurface,
    surface = SoulariumSurface,
    onSurface = SoulariumOnSurface,
)
```

- [ ] **Step 3: Write `Typography.kt`**

```kotlin
package org.cru.soularium.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import soularium.composeapp.generated.resources.OpenSans_Bold
import soularium.composeapp.generated.resources.OpenSans_Light
import soularium.composeapp.generated.resources.OpenSans_Regular
import soularium.composeapp.generated.resources.OpenSans_SemiBold
import soularium.composeapp.generated.resources.Res

@Composable
fun openSansFamily(): FontFamily = FontFamily(
    Font(Res.font.OpenSans_Regular, FontWeight.Normal),
    Font(Res.font.OpenSans_Light, FontWeight.Light),
    Font(Res.font.OpenSans_SemiBold, FontWeight.SemiBold),
    Font(Res.font.OpenSans_Bold, FontWeight.Bold),
)

@Composable
fun soulariumTypography(): Typography {
    val openSans = openSansFamily()
    val base = TextStyle(fontFamily = openSans)
    return Typography(
        headlineLarge = base.copy(fontWeight = FontWeight.Bold, fontSize = 32.sp),
        headlineMedium = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 24.sp),
        titleLarge = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
        bodyLarge = base.copy(fontWeight = FontWeight.Normal, fontSize = 16.sp),
        bodyMedium = base.copy(fontWeight = FontWeight.Normal, fontSize = 14.sp),
        labelLarge = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    )
}
```

- [ ] **Step 4: Write `Theme.kt`**

```kotlin
package org.cru.soularium.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun SoulariumTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SoulariumLightColors,
        typography = soulariumTypography(),
        content = content,
    )
}
```

- [ ] **Step 5: Apply theme in `App.kt`**

```kotlin
package org.cru.soularium

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.cru.soularium.ui.theme.SoulariumTheme

@Composable
fun App() {
    SoulariumTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Soularium v2 — bootstrap OK")
            }
        }
    }
}
```

- [ ] **Step 6: Build to verify resource generation**

Run: `cd mobile && ./gradlew :composeApp:assembleDebug`
Expected: build succeeds; generated `Res.font.OpenSans_Regular` symbols exist.

- [ ] **Step 7: Commit**

```bash
git add mobile/composeApp/
git commit -m "feat(ui): Soularium theme with Open Sans + orange palette"
```

---

## Phase 6: Strings, Navigation Skeleton, ViewModels

The remaining tasks follow the same TDD-driven, bite-sized pattern. Each task creates a screen composable with a contract test for its key behavior, wires it into the navigation graph, and commits.

For brevity, the per-task scaffolding below summarises files and steps. When executing, follow the same `write test → fail → minimal impl → pass → commit` rhythm as Phases 0–3.

### Task 22: Strings resource — first pass (English)

**Files:**
- Create: `mobile/composeApp/src/commonMain/composeResources/values/strings.xml`

- [ ] **Step 1:** Populate with all UI strings used by the spec — at minimum: app name, menu items, all question prompts (`q1_prompt` … `q5_prompt`), buttons (`continue`, `back`, `bookmark`, `share`), error messages, About copy, Resources labels.
- [ ] **Step 2:** Stub equivalents for `values-es/strings.xml`, `values-fr/strings.xml`, `values-pl/strings.xml`, `values-zh-rCN/strings.xml` with English fallbacks (Crowdin will fill these).
- [ ] **Step 3:** Commit.

### Task 23: DI setup (Koin)

**Files:**
- Create: `mobile/composeApp/src/commonMain/kotlin/org/cru/soularium/di/AppModule.kt`
- Create: `mobile/composeApp/src/commonMain/kotlin/org/cru/soularium/di/KoinInit.kt`
- Modify: `App.kt` to wrap content in `KoinApplication`.

- [ ] **Step 1:** Declare modules wiring `SessionRepository`, `ContentRepository`, `AnalyticsTracker` (placeholder), `CrashReporter` (placeholder), `Sharer` (`expect`/`actual`).
- [ ] **Step 2:** Initialize Koin in Android `MainActivity` and iOS `MainViewController`.
- [ ] **Step 3:** Commit.

### Task 24: ConversationViewModel

**Files:**
- Create: `mobile/composeApp/src/commonMain/kotlin/org/cru/soularium/ui/conversation/ConversationViewModel.kt`
- Test: `mobile/composeApp/src/commonTest/kotlin/org/cru/soularium/ui/conversation/ConversationViewModelTest.kt`

- [ ] **Step 1:** Write Turbine-based tests asserting that emitting `BeginSelection` updates the exposed `state` Flow correctly and dispatches the right effects through fake repository test doubles.
- [ ] **Step 2:** Implement `ConversationViewModel` using `viewModelScope` + a `StateFlow<SessionState>` + a `dispatch(event)` function that calls `transition` and consumes effects against injected repositories.
- [ ] **Step 3:** Commit.

### Task 25: Compose Navigation graph skeleton

**Files:**
- Create: `mobile/composeApp/src/commonMain/kotlin/org/cru/soularium/ui/nav/NavGraph.kt`
- Create: `mobile/composeApp/src/commonMain/kotlin/org/cru/soularium/ui/nav/Routes.kt`

- [ ] **Step 1:** Define `Routes` sealed object with constants for `Splash`, `Intro`, `Terms`, `Home`, `Conversation/{sessionId}`, `Past`, `About`, `Resources`, `CardsAndQuestions`, `Settings`, `Summary/{sessionId}`.
- [ ] **Step 2:** Wire `NavHost` with stub composables for each route (`Text("TODO: <route>")` is acceptable scaffolding here — they'll be filled in subsequent tasks).
- [ ] **Step 3:** Replace `App.kt` body with `NavGraph()`.
- [ ] **Step 4:** Commit.

---

## Phase 7: Screens

Each screen task follows the same shape: create screen composable with a clear input contract (a state object + a callback for events), wire its real implementation into `NavGraph`, write at least one Compose UI test verifying core interaction (button click → event), commit.

### Task 26: Intro + Terms screens
- `IntroScreen.kt`: two-page swipeable Pager + page indicators. Buttons advance to Terms.
- `TermsScreen.kt`: scrollable text + Agree button. On agree, persist `agreed_to_tos = true` in `device_state` and navigate to Home.

### Task 27: Home screen with bottom-sheet menu
- `HomeScreen.kt`: hero text + "Start a Conversation" CTA + "MySoularium" CTA + menu trigger (FAB or top-bar icon).
- `MenuBottomSheet.kt`: ModalBottomSheet with rows for MySoularium, Past Conversations, About, Resources, Cards & Questions, Settings.

### Task 28: AddParticipantsScreen
- Text field + add button + chip list of added names + reorder/remove + Continue button (disabled if 0 names).
- Fires `AddParticipant(name)`, `RemoveParticipant(index)`, `ConfirmParticipants`.

### Task 29: QuestionPromptScreen + InstructionPanelScreen
- Question text via `stringResource(Res.string.q1_prompt)` etc.
- Active participant name displayed prominently for group sessions.
- "Begin" button fires `BeginSelection`.
- Instruction panel is a modal that dismisses via `DismissInstructions`.

### Task 30: SelectionScreen
- Lazy vertical grid of cards (Coil-loaded), tap toggles selection.
- Round indicator at top, count of selected.
- Confirm button disabled until valid count; fires `ConfirmSelection`.
- Two visual modes (grid / scrolling-fullscreen) toggleable via a top-bar icon — matches original UX.

### Task 31: FinalizingScreen
- Shows the (up to 3) currently-final picks large.
- Allows tapping a final to swap it (returns to selection or shows a smaller picker).
- Confirm button fires `ConfirmFinal`.

### Task 32: DiscussingScreen
- Shows the finalized picks for the active participant.
- Subtitle text from `Question.discussionKey`.
- "Done" button fires `EndDiscussion`.

### Task 33: SummaryScreen
- Per-participant tabs (or carousel for group sessions).
- For each participant: 9-card mosaic, contact-summary preview.
- Share button → generates URL via `shareUrlFor` → invokes `Sharer.share(url)`.
- "Add contact" button → routes to ContactCollectionScreen for that participant.
- "Done" button → fires `Conclude`.

### Task 34: ContactCollectionScreen
- Form: name (prefilled), surname, email, phone, notes.
- Phone validated via libphonenumber; inline error if invalid.
- "Save" → `CollectContact(idx, info)`. "Skip" → `SkipContact`.

### Task 35: PastConversationsScreen
- Two `TabRow` tabs: Completed, Bookmarked.
- LazyColumn of Session rows: kind icon (solo/group), date, participant names.
- Tap → resume (bookmarked) or open Summary (completed).
- Swipe-to-delete with confirm dialog → `SessionRepository.deleteSession`.

### Task 36: AboutScreen
- Loads `about_body.md` from resources, renders via multiplatform-markdown-renderer.

### Task 37: ResourcesScreen
- LazyColumn of `ResourceLink` rows; tap opens URL via platform URI handler (`expect`/`actual`).

### Task 38: CardsAndQuestionsScreen
- Two tabs: Cards (grid of all 50), Questions (list with prompts).
- Tapping a card shows a full-screen viewer (Coil + zoom).

### Task 39: SettingsScreen
- Single section: Language. RadioGroup of supported locales. Persists to `device_state.last_known_locale` and recomposes UI.

For all of Phase 7, each screen task should include:
1. **Compose UI test** under `commonTest` verifying the primary interaction (e.g., "tapping Confirm with 3 picks fires `ConfirmFinal` event").
2. **A11y annotations**: every actionable element has a `contentDescription`, semantic role, and tap target ≥ 48dp.
3. **Locale-aware strings**: no literal strings in composables; everything via `stringResource(Res.string.xxx)`.

---

## Phase 8: Platform integration

### Task 40: Sharer expect/actual

**Files:**
- Create: `mobile/composeApp/src/commonMain/kotlin/org/cru/soularium/platform/Sharer.kt` (expect)
- Create: `mobile/composeApp/src/androidMain/kotlin/org/cru/soularium/platform/Sharer.android.kt`
- Create: `mobile/composeApp/src/iosMain/kotlin/org/cru/soularium/platform/Sharer.ios.kt`

- [ ] **Step 1:** Android implementation uses `Intent.ACTION_SEND` with `EXTRA_TEXT` and `Intent.createChooser`. Wrapped in a `try/catch` that returns `NoAppAvailable` if no resolver.
- [ ] **Step 2:** iOS implementation uses `UIActivityViewController` presented from the topmost `UIViewController`.
- [ ] **Step 3:** Wire both via Koin in `androidMain`/`iosMain` modules.
- [ ] **Step 4:** Commit.

### Task 41: Firebase analytics + Crashlytics (Android first)

**Files:**
- Create: `mobile/composeApp/src/androidMain/kotlin/org/cru/soularium/platform/FirebaseAnalyticsTracker.kt`
- Create: `mobile/composeApp/src/androidMain/kotlin/org/cru/soularium/platform/FirebaseCrashReporter.kt`
- Add: `google-services.json` (gitignored) + `example.google-services.json` template.
- Modify: `composeApp/build.gradle.kts` to apply `google-services` and `firebase-crashlytics` plugins (Android target only).

- [ ] **Step 1:** Implement `AnalyticsTracker` calling `FirebaseAnalytics.logEvent`. Scrub `params` to remove keys matching `name|email|phone|notes|card_id`.
- [ ] **Step 2:** Implement `CrashReporter` calling `FirebaseCrashlytics.recordException` for non-fatals.
- [ ] **Step 3:** Unit-test the scrubbing logic in `commonTest` against a fake `Map<String, Any>`.
- [ ] **Step 4:** Commit.

### Task 42: Firebase on iOS

**Files:**
- Modify: `mobile/iosApp/iosApp.xcodeproj` (add Firebase SPM dependency for Analytics + Crashlytics).
- Add: `GoogleService-Info.plist` (gitignored) + `example.GoogleService-Info.plist`.
- Create: `mobile/composeApp/src/iosMain/kotlin/org/cru/soularium/platform/FirebaseAnalyticsTracker.ios.kt`

- [ ] **Step 1:** Initialize Firebase in `iOSApp.swift` via `FirebaseApp.configure()`.
- [ ] **Step 2:** iOS `AnalyticsTracker` calls Firebase iOS SDK via Kotlin/Native interop. Same scrubbing as Android.
- [ ] **Step 3:** Commit.

### Task 43: BackHandler integration

- [ ] **Step 1:** In `ConversationHost`, register a `BackHandler` that shows a `AlertDialog` with "Bookmark and exit" / "Discard" / "Cancel" options.
- [ ] **Step 2:** Test on both platforms (iOS swipe-back; Android system back).
- [ ] **Step 3:** Commit.

---

## Phase 9: i18n pipeline (Crowdin GitHub Action)

### Task 44: crowdin.yml + secret setup

**Files:**
- Create: `crowdin.yml` (repo root)

- [ ] **Step 1:** Write config:

```yaml
project_id_env: CROWDIN_PROJECT_ID
api_token_env: CROWDIN_PERSONAL_TOKEN
base_path: "."
base_url: "https://api.crowdin.com"
preserve_hierarchy: true

files:
  - source: "mobile/composeApp/src/commonMain/composeResources/values/strings.xml"
    translation: "mobile/composeApp/src/commonMain/composeResources/values-%two_letters_code%/strings.xml"
    languages_mapping:
      two_letters_code:
        zh-CN: zh-rCN
```

- [ ] **Step 2:** Document required GitHub secrets in README: `CROWDIN_PROJECT_ID`, `CROWDIN_PERSONAL_TOKEN`.
- [ ] **Step 3:** Commit.

### Task 45: GitHub Action for Crowdin push/pull

**Files:**
- Create: `.github/workflows/crowdin.yml`

- [ ] **Step 1:** Write the workflow:

```yaml
name: Crowdin sync

on:
  schedule:
    - cron: '0 6 * * 1' # every Monday 06:00 UTC
  workflow_dispatch:

jobs:
  sync:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: crowdin/github-action@v2
        with:
          upload_sources: true
          download_translations: true
          create_pull_request: true
          localization_branch_name: chore/crowdin-translations
          commit_message: "chore(i18n): translations from Crowdin"
          pull_request_title: "chore(i18n): translations from Crowdin"
          pull_request_body: "Automated translation update from Crowdin."
          pull_request_base_branch_name: main
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          CROWDIN_PROJECT_ID: ${{ secrets.CROWDIN_PROJECT_ID }}
          CROWDIN_PERSONAL_TOKEN: ${{ secrets.CROWDIN_PERSONAL_TOKEN }}
```

- [ ] **Step 2:** Commit.

---

## Phase 10: CI

### Task 46: PR CI workflow

**Files:**
- Create: `.github/workflows/ci.yml`

- [ ] **Step 1:** Write the workflow:

```yaml
name: CI

on:
  pull_request:
    branches: [main]
  push:
    branches: [main]

jobs:
  build-and-test:
    runs-on: macos-14
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
      - uses: gradle/actions/setup-gradle@v4
      - name: ktlint
        run: cd mobile && ./gradlew ktlintCheck
      - name: Domain tests (JVM)
        run: cd mobile && ./gradlew :domain:test
      - name: Common tests (data + composeApp)
        run: cd mobile && ./gradlew :data:allTests :composeApp:allTests
      - name: Android assemble
        run: cd mobile && ./gradlew :composeApp:assembleDebug
      - name: iOS framework
        run: cd mobile && ./gradlew :composeApp:linkPodDebugFrameworkIosSimulatorArm64
```

- [ ] **Step 2:** Commit.

### Task 47: Release workflow stub

**Files:**
- Create: `.github/workflows/release.yml`

- [ ] **Step 1:** Add a tag-triggered workflow that runs `:composeApp:assembleRelease` and `:composeApp:bundleRelease` for Android, plus an iOS archive step. Fastlane integration is a placeholder until store credentials are set up — initial release lanes are intentionally manual.
- [ ] **Step 2:** Commit.

---

## Phase 11: Polish and hardening

### Task 48: Accessibility audit pass
- Walk every screen with TalkBack and VoiceOver enabled.
- Ensure all interactive elements have `contentDescription`.
- Ensure tap targets ≥ 48dp / 44pt.
- Run automated accessibility lint (Compose's `assertContentDescription`/`assertHasClickAction` in UI tests).
- Commit per screen fix.

### Task 49: End-to-end smoke tests
- Compose UI test: complete a solo session start → 5 questions → summary → share-mock → conclude.
- Compose UI test: group session of 3 participants completing all 5 questions.
- Compose UI test: bookmark mid-Q3, resume from Past Conversations, complete.
- Compose UI test: delete a past conversation.

### Task 50: Manual test on real devices
- Sideload Android APK on at least one device (Pixel + a budget Android phone).
- Run iOS build on a real iPhone via Xcode.
- Verify share intents launch the correct system sheets.
- Verify locale switching works at runtime.
- Capture screenshots for store listings.

### Task 51: Submit to Firebase App Distribution
- Configure Fastlane `firebase_app_distribution` lane for both platforms.
- Distribute to an internal tester group (Cru's mobile devs).

---

## Acceptance check before declaring v1 done

Run through every item in §10 of the spec (`docs/superpowers/specs/2026-05-20-soularium-v2-design.md`). For each criterion that fails, file a follow-up task in this plan.

---

## Notes for the executor (you, in the next phase)

- The Phase 7 screen tasks are intentionally less granular than Phase 0–4. The spec defines the behavior contract; the visual design is your judgment within the established theme. Each screen task must still TDD its primary interaction and commit before moving on.
- Phase 9–10 (Crowdin + CI) can begin in parallel with Phase 6–7 because they touch repo configuration and not feature code. Splitting them across days reduces risk: get CI green early.
- Do not skip `ktlintCheck`. Failing lint signals real style drift that compounds fast in a multi-module KMP project.
- Firebase config files (`google-services.json`, `GoogleService-Info.plist`) are gitignored. The executor will need them from Cru's existing Firebase project. The build will work in dev mode with the `example.*` templates as a stub.
