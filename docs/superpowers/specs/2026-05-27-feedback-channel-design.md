# Feedback Channel — Design Spec

**Date:** 2026-05-27
**Target version:** v0.2.3 (`versionCode 5`)
**Status:** Approved by user, ready for implementation planning

## Goal

Add a lightweight in-app feedback channel so users can rate the app and send written feedback. This is the first item from the "1순위 — 신뢰/리텐션" backlog (the other two — backup/restore and Crashlytics — were deferred or replaced by free Play Console Vitals).

## Scope

**In scope:**
- Settings screen: new "도움말" section with two rows — "별점 남기기" and "문의/제안하기"
- "별점 남기기": triggers Google Play In-App Review API; falls back to Play Store listing if Google doesn't show the modal
- "문의/제안하기": opens user's mail app via `ACTION_SENDTO` with prefilled recipient, subject, and body (device + app version)
- Automatic In-App Review request: triggered when cumulative "오늘 했어요" count reaches 7, on next app foreground, max once per `versionCode`

**Out of scope (explicit YAGNI):**
- In-app feedback form with categories or anonymous submission (no backend exists)
- Tracking how many reviews / what star rating users actually leave (Play Console already shows this)
- Custom rate-limiting beyond `lastReviewedVersion` (Google's API has its own quota)
- Crashlytics or any error reporting (Play Console Vitals covers it for free)
- Backup/restore (separate spec)

## Decisions made during brainstorming

| Question | Decision | Reasoning |
|---|---|---|
| Scope of feedback | Mailto + In-App Review API (option B) | Mailto-only misses star ratings; full in-app form is overkill for v0.2.x |
| Auto-trigger threshold | After 7 "오늘 했어요" actions | Real engagement signal; matches ~1 week of active use |
| Trigger timing | Next foreground after threshold crossed | Contextual; works for both UI and notification-action paths |
| Email destination | `hyunsung1987@gmail.com` (existing privacy policy address) | Single user, low volume expected, easy to change later |
| Version target | v0.2.3 (`versionCode 5`) | Isolates this change from v0.2.2 (already built); cleaner release notes |

## UX

### Settings screen — new "도움말" section

Placed above the existing "앱 정보" section:

```
─── 도움말 ──────────────────
별점 남기기                    >
문의/제안하기                  >
─────────────────────────────
```

Each row uses the existing `ClickableRow` composable (consistent with the rest of the settings screen). The trailing chevron matches the existing pattern; the value column is empty (purely action-oriented).

### "별점 남기기" behavior

1. Tap → call `ReviewLauncher.requestInAppReview(activity)`
2. If Google Play services available AND quota not exceeded: Google's small star-rating modal appears in-place
3. Otherwise: fallback to opening the Play Store listing for `com.lastdone` (`market://details?id=com.lastdone`)
4. If even Play Store fallback fails (no Play app, web URL works): use `https://play.google.com/store/apps/details?id=com.lastdone`

Result: user always sees *something* — no silent failure.

### "문의/제안하기" behavior

1. Tap → build `Intent.ACTION_SENDTO` with prefilled fields
2. Launch via `startActivity(Intent.createChooser(intent, "메일 앱 선택"))`
3. If no mail app handles it: catch `ActivityNotFoundException`, show Toast `"메일 앱이 설치돼 있지 않습니다"`

Prefilled email content:

- **To:** `hyunsung1987@gmail.com`
- **Subject:** `[언제했지 v0.2.3] 문의/제안`
- **Body:**
  ```
  ─────────────
  앱 버전: 0.2.3 (build 5)
  기기: <Build.MANUFACTURER> <Build.MODEL>
  Android: <Build.VERSION.RELEASE>
  ─────────────

  (여기에 내용을 작성해 주세요)
  ```

### Auto In-App Review trigger flow

1. User taps "오늘 했어요" anywhere (UI or notification action)
2. `SettingsRepository.incrementDoneCount()` runs (DataStore atomic increment)
3. `HomeRoute` is observing `AppSettings` via Flow; the change re-emits state
4. `LaunchedEffect(doneCount, lastReviewedVersion)` checks `ReviewPromptManager.shouldRequest(...)`
5. If true: call `requestInAppReview(activity)`, then `SettingsRepository.markReviewed(BuildConfig.VERSION_CODE)`
6. Result: at most one Google review modal per `versionCode`

The trigger happens on the next foreground if the increment came from `MarkDoneReceiver` while the app was in the background. The `LaunchedEffect` re-runs when the observed state changes after process restart / resume.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  UI Layer (Activity context)                                 │
│  ┌──────────────────┐  ┌──────────────────┐                 │
│  │ SettingsScreen   │  │ HomeRoute        │                 │
│  │ "별점 남기기"     │  │ LaunchedEffect   │                 │
│  │ "문의/제안하기"   │  │ → auto review    │                 │
│  └────────┬─────────┘  └────────┬─────────┘                 │
│           │                     │                            │
└───────────┼─────────────────────┼────────────────────────────┘
            ▼                     ▼
┌─────────────────────────────────────────────────────────────┐
│  Helper Layer (top-level functions, no class state)          │
│  - FeedbackMailIntent.buildFeedbackMailIntent(ctx): Intent   │
│  - ReviewLauncher.requestInAppReview(activity)               │
│  - ReviewLauncher.openPlayStoreListing(ctx)                  │
└─────────────────────────────────────────────────────────────┘
            ▲                              ▲
            │                              │
┌───────────┴─────────────┐  ┌─────────────┴──────────────┐
│ ItemDetailViewModel     │  │ MarkDoneReceiver           │
│  .markDoneToday()       │  │  .onReceive()              │
│  → settingsRepo         │  │  → settingsRepo            │
│    .incrementDoneCount()│  │    .incrementDoneCount()   │
└─────────┬───────────────┘  └────────────┬───────────────┘
          │                                │
          └────────────────┬───────────────┘
                           ▼
            ┌──────────────────────────────┐
            │ SettingsRepository (DataStore)│
            │  + doneCount: Int             │
            │  + lastReviewedVersion: Int?  │
            │  + incrementDoneCount()       │
            │  + markReviewed(v: Int)       │
            └──────────────────────────────┘
                           ▲
                           │ pure decision function
            ┌──────────────────────────────┐
            │ ReviewPromptManager           │
            │  shouldRequest(count, lastV,  │
            │    currentV): Boolean         │
            │  REVIEW_THRESHOLD = 7         │
            └──────────────────────────────┘
```

### Separation of concerns

- **Activity-required helpers** (`ReviewLauncher`, settings-screen tap handlers) live in the UI layer
- **Pure logic** (`ReviewPromptManager`, `FeedbackMailIntent`) — no Android dependencies beyond `Context`/`Intent` types; easy to unit test
- **Data** (`SettingsRepository`) — already exists, extended with two keys

## Data model

### `AppSettings` (data class) — add two fields

```kotlin
data class AppSettings(
    // existing fields unchanged
    val doneCount: Int = 0,
    val lastReviewedVersion: Int? = null
)
```

### `SettingsRepository.Keys` — add two keys

```kotlin
val DoneCount = intPreferencesKey("done_count")
val LastReviewedVersion = intPreferencesKey("last_reviewed_version")
```

### `SettingsRepository` — add two methods

```kotlin
suspend fun incrementDoneCount() {
    store.edit { it[Keys.DoneCount] = (it[Keys.DoneCount] ?: 0) + 1 }
}

suspend fun markReviewed(versionCode: Int) {
    store.edit { it[Keys.LastReviewedVersion] = versionCode }
}
```

### Decision logic

```kotlin
object ReviewPromptManager {
    const val REVIEW_THRESHOLD = 7
    fun shouldRequest(doneCount: Int, lastReviewedVersion: Int?, currentVersion: Int): Boolean =
        doneCount >= REVIEW_THRESHOLD && lastReviewedVersion != currentVersion
}
```

### Migration / compatibility

- DataStore Preferences — missing keys read as default values, no migration needed
- Existing users update to v0.2.3 with `doneCount = 0`; past usage doesn't count (intentional — no historical data to mine)
- `lastReviewedVersion` is null for upgraders, so threshold-met users trigger review on next foreground (intentional)
- Future versions (v0.2.4+) increment `versionCode`, naturally re-enabling one review request per release

## Files affected

### New files (3)

1. `app/src/main/java/com/lastdone/app/feedback/ReviewPromptManager.kt` — pure decision object
2. `app/src/main/java/com/lastdone/app/feedback/ReviewLauncher.kt` — Play In-App Review wrapper + Play Store fallback
3. `app/src/main/java/com/lastdone/app/feedback/FeedbackMailIntent.kt` — mailto Intent builder

### Modified files (8)

4. `gradle/libs.versions.toml` — add `playServicesReview = "2.0.2"`, library alias `play-services-review`
5. `app/build.gradle.kts` — `versionCode = 5`, `versionName = "0.2.3"`, `implementation(libs.play.services.review)`
6. `app/src/main/java/com/lastdone/app/data/settings/AppSettings.kt` — two fields
7. `app/src/main/java/com/lastdone/app/data/settings/SettingsRepository.kt` — two keys, two methods, flow mapping
8. `app/src/main/java/com/lastdone/app/ui/itemdetail/ItemDetailViewModel.kt` — inject `SettingsRepository`, call `incrementDoneCount()` inside `markDoneToday()`
9. `app/src/main/java/com/lastdone/app/notification/MarkDoneReceiver.kt` — call `app.settingsRepository.incrementDoneCount()` inside existing coroutine block
10. `app/src/main/java/com/lastdone/app/ui/home/HomeScreen.kt` — `LaunchedEffect` watching `doneCount`/`lastReviewedVersion`, triggers `ReviewLauncher.requestInAppReview` + `markReviewed`
11. `app/src/main/java/com/lastdone/app/ui/settings/SettingsScreen.kt` — new "도움말" section with two `ClickableRow`s above the "앱 정보" section

### Unchanged

- `AndroidManifest.xml` — no new permissions or activities (Play In-App Review uses Play services already on device)
- `docs/PRIVACY.md` — In-App Review is a Google Play service governed by Play Store policies; no additional user data is collected by the app; mailto is user-initiated and uses their own mail client
- Notification, alarm, ad, widget, database logic — unrelated

## Error handling

| Failure | Handling |
|---|---|
| Mail app not installed | Catch `ActivityNotFoundException`, show Toast `"메일 앱이 설치돼 있지 않습니다"` |
| In-App Review API request fails (no Play services, offline, quota) | `OnFailureListener` → open Play Store listing via `market://` or `https://` |
| Play Store listing also unavailable | Final fallback: silent failure on auto-trigger (still call `markReviewed()` so we don't loop); for manual tap, Toast `"Play 스토어를 열 수 없습니다"` |
| `LocalContext.current` not an `Activity` (defensive only — shouldn't happen) | `as? Activity` → null → skip silently |
| `LaunchedEffect` re-fires after `markReviewed()` | `shouldRequest()` returns false on next emission (idempotent) |
| Counter increment failure (e.g., receiver killed mid-write) | Acceptable: at most one lost increment per kill event; threshold is fuzzy by design |

## Testing

### Unit tests (JUnit, no mocking framework needed)

1. **`ReviewPromptManagerTest.kt`**
   - `shouldRequest(6, null, 5)` → false
   - `shouldRequest(7, null, 5)` → true
   - `shouldRequest(100, 5, 5)` → false (already prompted this version)
   - `shouldRequest(100, 4, 5)` → true (newer version, re-enable)
   - `shouldRequest(7, 5, 5)` → false (boundary)

2. **`FeedbackMailIntentTest.kt`** (uses `Context` from `RuntimeEnvironment` or `mock`)
   - Returned Intent's action is `ACTION_SENDTO`
   - `data.scheme` is `"mailto"`
   - `EXTRA_EMAIL` contains exactly `"hyunsung1987@gmail.com"`
   - `EXTRA_SUBJECT` contains `BuildConfig.VERSION_NAME`
   - `EXTRA_TEXT` contains `Build.MANUFACTURER`, `Build.MODEL`, `Build.VERSION.RELEASE`

### Manual verification (release-debug AAB on emulator or physical device)

1. **Mail link**: Settings → "문의/제안하기" → mail app opens with prefilled fields
2. **Manual rate menu**: Settings → "별점 남기기" → either Google modal OR Play Store opens
3. **Auto trigger**: temporarily set `REVIEW_THRESHOLD = 2` in `BuildConfig.DEBUG`, mark 2 items done, reopen home → modal/fallback appears; restore `REVIEW_THRESHOLD = 7`

### Out of scope for tests (YAGNI)

- Google In-App Review API internal behavior — Google's responsibility
- Integration test for `MarkDoneReceiver` + DataStore — setup cost too high; manual smoke test covers it
- Play Store fallback Intent verification — Android system responsibility

## Implementation sequence (locked order)

1. Gradle: `versionCode 5`, `versionName "0.2.3"`, add `play-services-review` library
2. `AppSettings` — add two fields
3. `SettingsRepository` — add two keys, two methods, flow mapping + unit-testable behavior verified manually
4. `ReviewPromptManager` + unit test
5. `FeedbackMailIntent` + unit test
6. `ReviewLauncher` (suspend + fallback to Play Store)
7. `ItemDetailViewModel` — inject repo, call `incrementDoneCount` in `markDoneToday`
8. `MarkDoneReceiver` — call `incrementDoneCount` inside existing coroutine
9. `HomeRoute`/`HomeViewModel` — observe settings state, `LaunchedEffect` triggers `requestInAppReview` + `markReviewed`
10. `SettingsScreen` — new "도움말" section with two `ClickableRow`s
11. Build: `:app:assembleDebug` and `:app:bundleRelease`
12. Manual verification (mail, manual rate, auto trigger with DEBUG threshold=2 → restore 7)
13. Update `lastdone-v0.2.3-release-2026-05-27.aab` (overwrite or new dated filename)

## Open questions

None — all decisions locked above.
