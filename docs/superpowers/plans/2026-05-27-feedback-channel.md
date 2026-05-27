# Feedback Channel Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a "도움말" section to the Settings screen with "별점 남기기" (Google In-App Review) and "문의/제안하기" (mailto) actions, plus an automatic review prompt after 7 cumulative "오늘 했어요" actions, shipped as v0.2.3 (versionCode 5).

**Architecture:** Three new helper files (`ReviewPromptManager`, `ReviewLauncher`, `FeedbackMailIntent`) under `feedback/`; two new DataStore keys in the existing `SettingsRepository`; two hook points (one in `ItemDetailViewModel.markDoneToday()`, one in `MarkDoneReceiver.onReceive()`); two new UI rows in `SettingsScreen`; one `LaunchedEffect` in `HomeRoute` for the automatic trigger.

**Tech Stack:** Kotlin, Jetpack Compose, DataStore Preferences, `com.google.android.play:review-ktx:2.0.2`, JUnit 4 (for the single pure-logic unit test).

**Spec:** `docs/superpowers/specs/2026-05-27-feedback-channel-design.md`

---

## Pre-task notes (read once before starting)

- The project has **no existing test infrastructure**. Task 2 sets up minimal JUnit-only tests (no Robolectric, no Android-instrumented tests). Only the pure-logic helper is unit-tested; the rest is verified by manual smoke test in Task 12.
- `ItemDetailViewModel` already takes `SettingsRepository` via constructor (line 39 of the existing file). No DI change needed there.
- `MarkDoneReceiver` already accesses `LastDoneApplication` via `context.applicationContext as LastDoneApplication`. The `settingsRepository` is exposed on the application instance.
- Do not commit `app/google-services.json` if it sneaks in (unrelated to this plan; user is still deciding on Firebase).
- All path conventions use Windows backslashes elsewhere in the repo but the source roots inside `app/src/main/java/...` use forward slashes per JVM convention. Use whatever the tool prefers.

## File structure

| File | Status | Responsibility |
|---|---|---|
| `gradle/libs.versions.toml` | Modify | Add `playServicesReview` version + library alias |
| `app/build.gradle.kts` | Modify | `versionCode 5`, `versionName "0.2.3"`, add `play-services-review` dependency |
| `app/src/main/java/com/lastdone/app/feedback/ReviewPromptManager.kt` | Create | Pure decision function `shouldRequest(...)`, `REVIEW_THRESHOLD = 7` |
| `app/src/test/java/com/lastdone/app/feedback/ReviewPromptManagerTest.kt` | Create | 5 JUnit test cases covering threshold + version logic |
| `app/src/main/java/com/lastdone/app/feedback/FeedbackMailIntent.kt` | Create | Top-level `buildFeedbackMailIntent(Context): Intent` |
| `app/src/main/java/com/lastdone/app/feedback/ReviewLauncher.kt` | Create | `requestInAppReview(Activity)` suspend fn + `openPlayStoreListing(Context)` fallback |
| `app/src/main/java/com/lastdone/app/data/settings/AppSettings.kt` | Modify | Add `doneCount: Int = 0`, `lastReviewedVersion: Int? = null` |
| `app/src/main/java/com/lastdone/app/data/settings/SettingsRepository.kt` | Modify | Add two `Keys`, two methods, extend `settings` Flow mapping |
| `app/src/main/java/com/lastdone/app/ui/itemdetail/ItemDetailViewModel.kt` | Modify | Call `settingsRepository.incrementDoneCount()` inside `markDoneToday()` |
| `app/src/main/java/com/lastdone/app/notification/MarkDoneReceiver.kt` | Modify | Call `app.settingsRepository.incrementDoneCount()` inside existing coroutine block |
| `app/src/main/java/com/lastdone/app/ui/settings/SettingsScreen.kt` | Modify | New "도움말" section with two `ClickableRow`s above "앱 정보" |
| `app/src/main/java/com/lastdone/app/ui/home/HomeScreen.kt` | Modify | New `LaunchedEffect` watching settings state, triggers auto review |

---

### Task 1: Bump version and add Play In-App Review dependency

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts:26-27` (versionCode/Name), and the `dependencies` block

- [ ] **Step 1: Edit `gradle/libs.versions.toml`**

Under `[versions]` add:
```toml
playServicesReview = "2.0.2"
```

Under `[libraries]` add:
```toml
play-services-review = { group = "com.google.android.play", name = "review-ktx", version.ref = "playServicesReview" }
```

- [ ] **Step 2: Edit `app/build.gradle.kts`**

Change `versionCode = 4` → `versionCode = 5` and `versionName = "0.2.2"` → `versionName = "0.2.3"` (lines 26-27).

In the `dependencies` block, after the line `implementation(libs.play.services.ads)`, add:
```kotlin
implementation(libs.play.services.review)
```

- [ ] **Step 3: Verify Gradle sync**

Run: `./gradlew :app:dependencies --configuration releaseRuntimeClasspath | grep play-services-review`

Expected: a line like `+--- com.google.android.play:review-ktx:2.0.2`

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "Bump to v0.2.3 and add Play In-App Review dependency"
```

---

### Task 2: Pure decision logic `ReviewPromptManager` (TDD)

**Files:**
- Create: `app/src/main/java/com/lastdone/app/feedback/ReviewPromptManager.kt`
- Create: `app/src/test/java/com/lastdone/app/feedback/ReviewPromptManagerTest.kt`

The project has no `src/test` directory yet. This task creates it and adds JUnit 4 (already transitively available; if not, add it).

- [ ] **Step 1: Ensure JUnit is available**

Run: `./gradlew :app:dependencies --configuration testRuntimeClasspath | grep -E "junit" | head -3`

Expected: a line like `+--- junit:junit:4.13.2`. If empty, add to `app/build.gradle.kts` `dependencies`:
```kotlin
testImplementation("junit:junit:4.13.2")
```

- [ ] **Step 2: Write the failing test**

Create `app/src/test/java/com/lastdone/app/feedback/ReviewPromptManagerTest.kt`:
```kotlin
package com.lastdone.app.feedback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPromptManagerTest {

    @Test
    fun `under threshold returns false`() {
        assertFalse(ReviewPromptManager.shouldRequest(doneCount = 6, lastReviewedVersion = null, currentVersion = 5))
    }

    @Test
    fun `at threshold with never-prompted returns true`() {
        assertTrue(ReviewPromptManager.shouldRequest(doneCount = 7, lastReviewedVersion = null, currentVersion = 5))
    }

    @Test
    fun `already prompted same version returns false`() {
        assertFalse(ReviewPromptManager.shouldRequest(doneCount = 100, lastReviewedVersion = 5, currentVersion = 5))
    }

    @Test
    fun `prompted older version returns true`() {
        assertTrue(ReviewPromptManager.shouldRequest(doneCount = 100, lastReviewedVersion = 4, currentVersion = 5))
    }

    @Test
    fun `boundary same version different count returns false`() {
        assertFalse(ReviewPromptManager.shouldRequest(doneCount = 7, lastReviewedVersion = 5, currentVersion = 5))
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.lastdone.app.feedback.ReviewPromptManagerTest"`

Expected: COMPILATION FAILURE — `ReviewPromptManager` does not exist.

- [ ] **Step 4: Implement minimal `ReviewPromptManager`**

Create `app/src/main/java/com/lastdone/app/feedback/ReviewPromptManager.kt`:
```kotlin
package com.lastdone.app.feedback

object ReviewPromptManager {
    const val REVIEW_THRESHOLD = 7

    fun shouldRequest(doneCount: Int, lastReviewedVersion: Int?, currentVersion: Int): Boolean =
        doneCount >= REVIEW_THRESHOLD && lastReviewedVersion != currentVersion
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.lastdone.app.feedback.ReviewPromptManagerTest"`

Expected: BUILD SUCCESSFUL, 5 tests passed.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/lastdone/app/feedback/ReviewPromptManager.kt app/src/test/java/com/lastdone/app/feedback/ReviewPromptManagerTest.kt
git commit -m "Add ReviewPromptManager with threshold logic and tests"
```

---

### Task 3: Extend `AppSettings` data class

**Files:**
- Modify: `app/src/main/java/com/lastdone/app/data/settings/AppSettings.kt`

- [ ] **Step 1: Add two fields**

In `AppSettings` data class, after `globalRepeatIntervalMinutes`, add:
```kotlin
val doneCount: Int = 0,
val lastReviewedVersion: Int? = null
```

Final shape:
```kotlin
data class AppSettings(
    val impendingThresholdDays: Int = DEFAULT_IMPENDING_THRESHOLD,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val sortMode: SortMode = SortMode.STATUS,
    val notifyTime: LocalTime = DEFAULT_NOTIFY_TIME,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: LocalTime = DEFAULT_QUIET_START,
    val quietHoursEnd: LocalTime = DEFAULT_QUIET_END,
    val globalRepeatIntervalMinutes: Int = DEFAULT_GLOBAL_REPEAT_MINUTES,
    val doneCount: Int = 0,
    val lastReviewedVersion: Int? = null
) { /* companion unchanged */ }
```

- [ ] **Step 2: Verify compile**

Run: `./gradlew :app:compileDebugKotlin`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lastdone/app/data/settings/AppSettings.kt
git commit -m "Add doneCount and lastReviewedVersion to AppSettings"
```

---

### Task 4: Extend `SettingsRepository` with two keys + two methods

**Files:**
- Modify: `app/src/main/java/com/lastdone/app/data/settings/SettingsRepository.kt`

- [ ] **Step 1: Add two keys to the `Keys` object**

Inside the `private object Keys` block (lines 91-100), after `GlobalRepeatIntervalMinutes`, add:
```kotlin
val DoneCount = intPreferencesKey("done_count")
val LastReviewedVersion = intPreferencesKey("last_reviewed_version")
```

- [ ] **Step 2: Extend the `settings` Flow mapping**

In the `settings: Flow<AppSettings>` mapping block (lines 22-44), inside the `AppSettings(...)` constructor call, after the `globalRepeatIntervalMinutes = ...` line, add:
```kotlin
doneCount = prefs[Keys.DoneCount] ?: 0,
lastReviewedVersion = prefs[Keys.LastReviewedVersion]
```

- [ ] **Step 3: Add two methods**

After `setGlobalRepeatIntervalMinutes(...)` (before the `private object Keys` block), add:
```kotlin
suspend fun incrementDoneCount() {
    store.edit { prefs ->
        prefs[Keys.DoneCount] = (prefs[Keys.DoneCount] ?: 0) + 1
    }
}

suspend fun markReviewed(versionCode: Int) {
    store.edit { it[Keys.LastReviewedVersion] = versionCode }
}
```

- [ ] **Step 4: Verify compile**

Run: `./gradlew :app:compileDebugKotlin`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lastdone/app/data/settings/SettingsRepository.kt
git commit -m "Add doneCount and review-prompt persistence to SettingsRepository"
```

---

### Task 5: Mail intent builder `FeedbackMailIntent`

**Files:**
- Create: `app/src/main/java/com/lastdone/app/feedback/FeedbackMailIntent.kt`

No unit test (Android `Build`/`Uri`/`Intent` dependencies require Robolectric, which isn't set up; verified manually in Task 12).

- [ ] **Step 1: Create the file**

Create `app/src/main/java/com/lastdone/app/feedback/FeedbackMailIntent.kt`:
```kotlin
package com.lastdone.app.feedback

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.lastdone.app.BuildConfig

private const val FEEDBACK_EMAIL = "hyunsung1987@gmail.com"

fun buildFeedbackMailIntent(@Suppress("UNUSED_PARAMETER") context: Context): Intent {
    val body = """
        ─────────────
        앱 버전: ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})
        기기: ${Build.MANUFACTURER} ${Build.MODEL}
        Android: ${Build.VERSION.RELEASE}
        ─────────────

        (여기에 내용을 작성해 주세요)

    """.trimIndent()
    return Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, "[언제했지 v${BuildConfig.VERSION_NAME}] 문의/제안")
        putExtra(Intent.EXTRA_TEXT, body)
    }
}
```

- [ ] **Step 2: Verify compile**

Run: `./gradlew :app:compileDebugKotlin`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lastdone/app/feedback/FeedbackMailIntent.kt
git commit -m "Add FeedbackMailIntent builder with prefilled device info"
```

---

### Task 6: In-App Review launcher with Play Store fallback

**Files:**
- Create: `app/src/main/java/com/lastdone/app/feedback/ReviewLauncher.kt`

- [ ] **Step 1: Create the file**

Create `app/src/main/java/com/lastdone/app/feedback/ReviewLauncher.kt`:
```kotlin
package com.lastdone.app.feedback

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val PACKAGE_NAME = "com.lastdone"
private const val PLAY_STORE_MARKET_URI = "market://details?id=$PACKAGE_NAME"
private const val PLAY_STORE_WEB_URL = "https://play.google.com/store/apps/details?id=$PACKAGE_NAME"

suspend fun requestInAppReview(activity: Activity) {
    val manager = ReviewManagerFactory.create(activity.applicationContext)
    val reviewInfo = suspendCancellableCoroutine<Any?> { cont ->
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) cont.resume(task.result) else cont.resume(null)
        }
    } ?: run {
        openPlayStoreListing(activity)
        return
    }
    suspendCancellableCoroutine<Unit> { cont ->
        manager.launchReviewFlow(activity, reviewInfo as com.google.android.play.core.review.ReviewInfo)
            .addOnCompleteListener { cont.resume(Unit) }
    }
}

fun openPlayStoreListing(context: Context) {
    val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_MARKET_URI)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(marketIntent)
        return
    } catch (_: ActivityNotFoundException) { /* fall through */ }

    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_WEB_URL)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(webIntent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Play 스토어를 열 수 없습니다", Toast.LENGTH_SHORT).show()
    }
}
```

- [ ] **Step 2: Verify compile**

Run: `./gradlew :app:compileDebugKotlin`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lastdone/app/feedback/ReviewLauncher.kt
git commit -m "Add ReviewLauncher with Play Store fallback"
```

---

### Task 7: Hook `ItemDetailViewModel.markDoneToday()`

**Files:**
- Modify: `app/src/main/java/com/lastdone/app/ui/itemdetail/ItemDetailViewModel.kt`

Constructor already takes `settingsRepository` (line 39). We need to keep a reference (drop the `val` removal if any) and call it.

- [ ] **Step 1: Promote the constructor parameter to a property**

Change line 39 from `settingsRepository: SettingsRepository` to `private val settingsRepository: SettingsRepository`. (If it's already `val`, skip this step. Verify by reading the file.)

- [ ] **Step 2: Increment counter inside `markDoneToday()`**

In `markDoneToday()` (lines 89-106), inside the `viewModelScope.launch { ... }` block, after `LastDoneWidgetProvider.requestUpdate(appContext)` and before `val nextDate = ...`, add:
```kotlin
settingsRepository.incrementDoneCount()
```

- [ ] **Step 3: Verify compile**

Run: `./gradlew :app:compileDebugKotlin`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lastdone/app/ui/itemdetail/ItemDetailViewModel.kt
git commit -m "Increment done count when marking item done from detail screen"
```

---

### Task 8: Hook `MarkDoneReceiver.onReceive()`

**Files:**
- Modify: `app/src/main/java/com/lastdone/app/notification/MarkDoneReceiver.kt`

- [ ] **Step 1: Add the increment call**

Inside the `scope.launch { try { ... } }` block, after `LastDoneWidgetProvider.requestUpdate(context)` (line 37) and before `} finally {`, add:
```kotlin
app.settingsRepository.incrementDoneCount()
```

- [ ] **Step 2: Verify compile**

Run: `./gradlew :app:compileDebugKotlin`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lastdone/app/notification/MarkDoneReceiver.kt
git commit -m "Increment done count when marking item done from notification"
```

---

### Task 9: Add "도움말" section to `SettingsScreen`

**Files:**
- Modify: `app/src/main/java/com/lastdone/app/ui/settings/SettingsScreen.kt`

- [ ] **Step 1: Add tap handlers at the top of `SettingsScreen`**

Just before the `Scaffold(...)` (around line 117, after the existing `LaunchedEffect` block), inside the composable but in the `SettingsScreen` function body, add a `LocalContext.current as? Activity` and use a `rememberCoroutineScope` to launch the suspend `requestInAppReview`. The file already imports `LocalContext`. Add these imports at the top:
```kotlin
import android.app.Activity
import androidx.compose.runtime.rememberCoroutineScope
import com.lastdone.app.feedback.buildFeedbackMailIntent
import com.lastdone.app.feedback.openPlayStoreListing
import com.lastdone.app.feedback.requestInAppReview
import android.content.ActivityNotFoundException
import android.widget.Toast
import kotlinx.coroutines.launch
```

Below `var showQuietEndPicker by remember { mutableStateOf(false) }`, add:
```kotlin
val coroutineScope = rememberCoroutineScope()
val activity = context as? Activity

val onRateClick: () -> Unit = {
    val act = activity
    if (act != null) {
        coroutineScope.launch { requestInAppReview(act) }
    } else {
        openPlayStoreListing(context)
    }
}

val onFeedbackClick: () -> Unit = {
    try {
        context.startActivity(buildFeedbackMailIntent(context))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "메일 앱이 설치돼 있지 않습니다", Toast.LENGTH_SHORT).show()
    }
}
```

- [ ] **Step 2: Add the "도움말" section in the `Column`**

In the `Column { ... }` body, after the `if (BuildConfig.DEBUG) { ... }` block (which ends around line 205) and **before** `Spacer(Modifier.height(24.dp)); SectionHeader("앱 정보")` (line 207-208), insert:
```kotlin
Spacer(Modifier.height(24.dp))
SectionHeader("도움말")
ClickableRow(label = "별점 남기기", value = "", onClick = onRateClick)
HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
ClickableRow(label = "문의/제안하기", value = "", onClick = onFeedbackClick)
```

- [ ] **Step 3: Verify compile**

Run: `./gradlew :app:compileDebugKotlin`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Smoke-build a debug APK to catch resource issues**

Run: `./gradlew :app:assembleDebug`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lastdone/app/ui/settings/SettingsScreen.kt
git commit -m "Add 도움말 section with 별점 남기기 and 문의/제안하기"
```

---

### Task 10: Auto-trigger review in `HomeRoute`

**Files:**
- Modify: `app/src/main/java/com/lastdone/app/ui/home/HomeScreen.kt`

The existing `HomeScreen` composable receives `viewModel: HomeViewModel`. We need access to the settings state. Two options:
- (a) Inject `SettingsRepository.settings` flow through `HomeViewModel` and expose a slim subset
- (b) Read directly from the `LastDoneApplication` settings flow in the composable

Option (a) is cleaner architecturally; option (b) is simpler. The codebase already uses option (b) pattern in `ui/ads/AdBanner.kt` (line 24: `val app = context.applicationContext as LastDoneApplication`). Use the same pattern here.

- [ ] **Step 1: Add the LaunchedEffect to `HomeScreen`**

Add these imports at the top of `HomeScreen.kt`:
```kotlin
import android.app.Activity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.lastdone.app.BuildConfig
import com.lastdone.app.LastDoneApplication
import com.lastdone.app.feedback.ReviewPromptManager
import com.lastdone.app.feedback.requestInAppReview
import kotlinx.coroutines.flow.map
```

Inside `HomeScreen(...)` composable body, right after the existing `val state by viewModel.uiState.collectAsState()` line (around line 63), add:
```kotlin
val context = LocalContext.current
val app = context.applicationContext as LastDoneApplication
val settingsState by app.settingsRepository.settings.collectAsState(initial = null)
LaunchedEffect(settingsState?.doneCount, settingsState?.lastReviewedVersion) {
    val s = settingsState ?: return@LaunchedEffect
    if (ReviewPromptManager.shouldRequest(
            doneCount = s.doneCount,
            lastReviewedVersion = s.lastReviewedVersion,
            currentVersion = BuildConfig.VERSION_CODE
        )
    ) {
        val activity = context as? Activity ?: return@LaunchedEffect
        requestInAppReview(activity)
        app.settingsRepository.markReviewed(BuildConfig.VERSION_CODE)
    }
}
```

- [ ] **Step 2: Verify compile**

Run: `./gradlew :app:compileDebugKotlin`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Smoke-build debug APK**

Run: `./gradlew :app:assembleDebug`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lastdone/app/ui/home/HomeScreen.kt
git commit -m "Trigger In-App Review automatically after 7 done actions"
```

---

### Task 11: Full debug + release build verification

**Files:** none (build only)

- [ ] **Step 1: Clean debug build**

Run: `./gradlew :app:assembleDebug`

Expected: BUILD SUCCESSFUL, `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 2: Run all unit tests**

Run: `./gradlew :app:testDebugUnitTest`

Expected: BUILD SUCCESSFUL, 5 tests passed (ReviewPromptManager).

- [ ] **Step 3: Release AAB build**

Run: `./gradlew :app:bundleRelease`

Expected: BUILD SUCCESSFUL, `app/build/outputs/bundle/release/app-release.aab` exists. Native lib strip warnings for `libandroidx.graphics.path.so` and `libdatastore_shared_counter.so` are expected — ignore.

- [ ] **Step 4: Verify BuildConfig embedded values**

Run: `grep -E "ADMOB|VERSION" app/build/generated/source/buildConfig/release/com/lastdone/app/BuildConfig.java`

Expected output includes:
- `VERSION_CODE = 5`
- `VERSION_NAME = "0.2.3"`
- `ADMOB_APP_ID = "ca-app-pub-6922989701218250~5420361569"`
- `ADMOB_BANNER_AD_UNIT_ID = "ca-app-pub-6922989701218250/7775062167"`

- [ ] **Step 5: Copy AAB to repo root**

```bash
cp app/build/outputs/bundle/release/app-release.aab lastdone-v0.2.3-release-2026-05-27.aab
ls -la lastdone-v0.2.3-release-2026-05-27.aab
```

(Adjust the date in the filename if the actual build date differs.)

---

### Task 12: Manual smoke verification on emulator/device

**Files:** none (manual test)

Install the debug APK on the emulator (already booted from earlier session) or a connected device.

- [ ] **Step 1: Install**

Run: `./gradlew :app:installDebug`

Expected: `Installed on 1 device`.

- [ ] **Step 2: Verify "문의/제안하기"**

Open the app → 설정 → 도움말 → "문의/제안하기" → mail app picker should appear with:
- Recipient: `hyunsung1987@gmail.com`
- Subject: `[언제했지 v0.2.3] 문의/제안`
- Body: prefilled with device info + "(여기에 내용을 작성해 주세요)"

If no mail app is installed on the emulator, expect Toast `"메일 앱이 설치돼 있지 않습니다"`.

- [ ] **Step 3: Verify "별점 남기기"**

Settings → 도움말 → "별점 남기기". On a stock emulator without a logged-in Play account, expect: Play Store opens (or Toast if even that fails). On a real device with Play account, the Google in-place review modal should appear.

- [ ] **Step 4: Verify auto-trigger (with temporary threshold override)**

Temporarily change `REVIEW_THRESHOLD = 7` → `REVIEW_THRESHOLD = 2` in `ReviewPromptManager.kt`. Rebuild & install. Mark 2 items done (UI or notification). Reopen Home → review modal / Play Store should appear once. Restore `REVIEW_THRESHOLD = 7` before committing the next change.

(If you're confident the integration is correct, this step is optional — but skipping it leaves the auto-trigger unverified end-to-end.)

- [ ] **Step 5: Commit any verification fixes (if needed)**

If any test surfaced a bug, fix it, add a regression unit test if the bug is in pure logic, and commit. Otherwise no commit.

---

### Task 13: Update AAB path

**Files:** none (move existing artifact)

- [ ] **Step 1: Remove stale v0.2.2 AAB (optional cleanup)**

```bash
ls lastdone-v0.2.*.aab
```

If only the v0.2.3 build will be uploaded, the v0.2.2 AAB can be removed:
```bash
rm lastdone-v0.2.1-release-2026-05-27.aab
rm lastdone-v0.2.2-release-2026-05-27.aab
```

(Skip this step if you want to keep them as historical artifacts.)

- [ ] **Step 2: Confirm final artifact**

Run: `ls -la lastdone-v0.2.3-release-*.aab`

Expected: a single AAB file in the 7-8 MB range, freshly modified.

---

## Done criteria

All boxes checked, all tests green, manual smoke test passed, `lastdone-v0.2.3-release-<date>.aab` exists at the repo root and is ready for Play Console upload.
