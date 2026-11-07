# Code Review — synapseSRC/synapseApp

## Metadata

| Field | Value |
| --- | --- |
| Date | 2026-11-07 (second pass) |
| Branch | `code-review` |
| HEAD reviewed | `b0d89576` — *fix(review): scope cover-URL signing, stop reflecting auth errors, add web error boundaries* |
| Review started at | `2a577731` — *fix(review): close auth deep-link, credential-leak and delivery-retry defects* |
| Scope | Full tracked-file inventory of the branch (1,809 files, 535 directories; 1,311 Kotlin, 112 TSX, 29 TS, 88 Swift, 10 SQLDelight, 55 XML) derived from the Git Trees API, then focused review of the release-critical surface: the four `SecurityCipher` actuals, Android signing/lint/manifest configuration, auth-posture configuration, the `web/` Next.js app (routing, middleware, session, cover media, auth error paths) and the 2026-11-06 delivery/poll/upload changes |
| Status | **CHANGES REQUESTED** — 10 files fixed in this pass; 4 blocking items from the first pass remain untouched by design (they need a maintainer decision, not a code edit) |
| Overall grade | Architecture **B-**, Security **C**, Correctness **B**, Performance **B**, Build hygiene **D** (unchanged — the release-key item dominates this grade) |

**How this pass was run.** The complete file inventory was enumerated through the GitHub Trees API so the coverage claim below is exhaustive rather than sampled. Review then concentrated on files a static pass can actually adjudicate. Where a first-pass finding was re-checked, the result is stated as *verified fixed* or *verified still open* with the exact evidence (line, function, or commit). No Kotlin compilation, Node type-check or Android lint run was performed: this review environment has no network access from the build sandbox and no Android SDK, so nothing in this document should be read as a build result.

---

## Scanned & Fixed Files

### Files fixed in this pass (10) — commit `b0d89576`

| # | File path | Change |
| --- | --- | --- |
| 1 | `web/src/lib/synapse/profile-media.ts` | **Modified** — signed cover URLs are now bound to the owner's uid; path traversal rejected |
| 2 | `web/src/app/(app)/u/[username]/page.tsx` | **Modified** — passes the profile's uid to the signer; raw PostgREST text no longer rendered |
| 3 | `web/src/app/(app)/profile/page.tsx` | **Modified** — passes the session user's uid to the signer |
| 4 | `web/src/app/(app)/profile/edit/page.tsx` | **Modified** — passes the session user's uid to the signer |
| 5 | `web/src/lib/synapse/auth.ts` | **Modified** — added `safeAuthNotice()`, an allow-list mapper for query-string auth text |
| 6 | `web/src/app/auth/callback/route.ts` | **Modified** — no longer forwards `error`/`error_description` verbatim |
| 7 | `web/src/app/(auth)/login/page.tsx` | **Modified** — renders only the mapped notice |
| 8 | `web/src/app/error.tsx` | **Added** — route-level error boundary for the whole `app/` tree |
| 9 | `web/src/app/global-error.tsx` | **Added** — last-resort boundary that replaces the root layout |
| 10 | `app/src/main/AndroidManifest.xml` | **Modified** — removed `WRITE_MEDIA_STORAGE`, a `signature\|privileged` permission that can never be granted |

### Files scanned in this pass

Read in full or line-scanned with targeted queries, at `code-review`:

```
AGENTS.md
REVIEW.md
.agents/.memory/CODE_REVIEW.md            (prior-pass state on this branch)
app/build.gradle
app/src/main/AndroidManifest.xml
app/src/main/kotlin/com/synapse/social/studioasinc/data/remote/services/AuthConfig.kt
app/src/main/kotlin/com/synapse/social/studioasinc/data/call/TurnCredentialRepositoryImpl.kt
shared/src/commonMain/.../shared/security/SecurityCipher.kt
shared/src/androidMain/.../shared/security/AndroidSecurityCipher.kt
shared/src/iosMain/.../shared/security/IosSecurityCipher.kt
shared/src/jvmMain/.../shared/security/SecurityCipher.jvm.kt
shared/src/commonMain/.../shared/domain/usecase/chat/CreatePollUseCase.kt
shared/src/commonMain/.../shared/data/source/remote/SupabaseUploadService.kt
web/next.config.mjs
web/package.json
web/src/middleware.ts
web/src/lib/supabase/config.ts
web/src/lib/supabase/server.ts
web/src/lib/supabase/middleware.ts
web/src/lib/synapse/auth.ts
web/src/lib/synapse/profile-media.ts
web/src/lib/synapse/profile-upload.ts
web/src/lib/synapse/profiles.ts
web/src/components/synapse/app-nav.tsx
web/src/components/synapse/profile-header.tsx
web/src/components/ui/copy-button.tsx
web/src/app/layout.tsx
web/src/app/(app)/layout.tsx
web/src/app/(app)/profile/page.tsx
web/src/app/(app)/profile/edit/page.tsx
web/src/app/(app)/u/[username]/page.tsx
web/src/app/(auth)/login/page.tsx
web/src/app/auth/callback/route.ts
```

Directory inventory was also taken for every path in the repository (535 directories), so the file list above is a deliberate cut of a known-complete set rather than the limit of what was reachable.

---

## Findings & Fixes Applied

### 🔴 Security

**`web/src/lib/synapse/profile-media.ts`**
*Issue identified.* `resolveCoverUrl()` minted a one-hour Supabase **signed URL for any storage path it was handed**, with no ownership check and no traversal guard. The path comes from `users.profile_cover_image`, a column the user writes, and the result is rendered on the public, indexable `/u/[username]` page. Any member could point their own column at `private-media://covers/<other-member-uid>/<file>` and have the server sign someone else's private object into a page they control the URL of — a signing oracle, not just a display bug. `decodeURIComponent` on the URL form additionally let percent-encoded `..` segments through.
*Resolution.* `resolveCoverUrl(reference, ownerUid?)` now normalises the extracted path (strips leading separators, rejects any `.` or `..` segment) and, when the caller supplies an owner, refuses to sign unless the path starts with `${ownerUid}/` — which is the prefix every upload writes. Unmatched references return `null` instead of a signed URL. All three call sites were updated: `(app)/profile/page.tsx` and `(app)/profile/edit/page.tsx` pass `user.id` (session-derived, never client input), `(app)/u/[username]/page.tsx` passes `profile.uid`.

**`web/src/app/auth/callback/route.ts`, `web/src/app/(auth)/login/page.tsx`, `web/src/lib/synapse/auth.ts`**
*Issue identified.* The callback read `error_description` (or `error`) straight off the query string and redirected it into `/login?error=…`, and the login page rendered that string verbatim inside the app's own `Failure` alert. React escapes it, so this is not XSS, but it is text spoofing: a crafted link renders attacker-authored copy inside a component that users read as the application speaking. The same route also echoed the raw `exchangeCodeForSession` message, which can carry backend wording.
*Resolution.* Added `safeAuthNotice()` — an ordered allow-list mapping the known Supabase/GoTrue phrases (expired/invalid link, `access_denied`, unconfirmed email, rate limit, server error) onto fixed copy, with a generic fallback for anything else. The callback now prefers the machine-readable `error` code, passes both parameters through the mapper, and uses fixed text for the exchange failure; the login page renders only the mapped notice.

### 🟠 Correctness

**`web/src/app/error.tsx`, `web/src/app/global-error.tsx` (added)**
*Issue identified.* `web/` had no `error.tsx` and no `global-error.tsx` — only `not-found.tsx`. `(app)/layout.tsx` awaits `getProfileByUid()`, and `profiles.ts` converts any PostgREST error into a thrown `Error`. A single transient database failure therefore escaped the render tree and took down every signed-in page with the framework's default overlay instead of a recoverable boundary.
*Resolution.* Added a route-level boundary for the `app/` subtree (generic copy, `reset()` retry, error logged via `console.error` so the connected monitor still sees it) and a `global-error.tsx` for failures in the root layout, written without Tailwind because it replaces `app/layout.tsx` and so does not inherit `globals.css`.

**`web/src/app/(app)/u/[username]/page.tsx`**
*Issue identified.* The public profile page interpolated `posts.error` — the raw PostgREST message — into visible copy on an indexable page (`robots: "index, follow"`), leaking backend table and constraint wording.
*Resolution.* Replaced with fixed copy; the branch still renders a distinct error state, but the message is no longer surfaced to the visitor.

### 🟡 Build hygiene

**`app/src/main/AndroidManifest.xml`**
*Issue identified.* The manifest declared `android.permission.WRITE_MEDIA_STORAGE`, which is `signature|privileged`: the platform can never grant it to a normal app, so the declaration is inert while implying a capability the app does not have.
*Resolution.* Removed the declaration (one line, no other manifest change).

---

## Verification Performed (and not performed)

* **Applied.** Commit `b0d89576` was pushed to `refs/heads/code-review` on top of `2a577731`, and the GitHub commit API reports exactly the ten paths above with the expected deltas (`+36`/−0 and `+43`/−0 for the two new boundary files, `+29`/−6 for the signer, `+0`/−1 for the manifest). `web/src/app/error.tsx` was also re-fetched from the branch to confirm it is served.
* **Re-checked from the first pass.** `TurnCredentialRepositoryImpl.kt` now logs a cause *type* only (`e.message ?: e.javaClass.simpleName`, with the comment "This message reaches logcat, CrashActivity and Sentry") — credential redaction holds. `SupabaseUploadService.kt` sanitises the object key and refuses the public-bucket fallback for user-scoped buckets — holds. `CreatePollUseCase.kt` rejects a blank `chatId` — holds. `auth/callback/route.ts` now **awaits** `ensureProfileRow`, so a callback can no longer silently leave an account with no `users` row. `copy-button.tsx` resolves its visual state from the clipboard promise. `app-nav.tsx` resets its pending state in a `finally`.
* **Not performed.** No `tsc`, `npm ci`, `./gradlew` or Android lint run — this environment has no package-manager or Android SDK access, and `AGENTS.md` forbids agents from running the repository's test tasks. Every change here is deliberately local and non-signature-affecting, but a maintainer-side `npm ci && tsc --noEmit` and `./gradlew compileDebugKotlin` remain the gate before merge.

---

## Findings Still Open

These were verified **still present at `2a577731`** during this pass and are restated with the evidence that was re-read. They are unchanged by choice, not by oversight.

| # | Severity | Finding | Evidence re-read this pass |
| --- | --- | --- | --- |
| 1 | 🔴 BLOCK | **Release signing key is published in the repository**, with its credentials hardcoded next to it. Anyone who can read the repo can build a same-signature APK. `keystore.properties` is loaded and then never used. | `app/build.gradle:49-52` — `storeFile file("release_key.jks")`, `storePassword "1111"`, `keyAlias "ashik"`, `keyPassword "1111"`; `app/release_key.jks` is tracked |
| 2 | 🔴 BLOCK | **Four divergent `SecurityCipher` implementations, three of them fail-open.** | `SecurityCipher.jvm.kt` still does `Cipher.getInstance("AES")` (defaults to ECB, no IV/MAC) with a `KeyGenerator` key created in `init`, so previously written ciphertext is undecryptable after a restart; `AndroidSecurityCipher.kt` still `return plainText` on encrypt failure and `return cipherText` on decrypt failure; `IosSecurityCipher.kt` still returns the input on failure at lines 105, 138, 147 and 178 with `kCCOptionPKCS7Padding` and no MAC |
| 3 | 🔴 BLOCK | **Fail-open auth posture driven by device-writable storage.** `AuthConfig` reads `development_mode`/`require_email_verification` from prefs at runtime and every catch falls back to a permissive value; `isDebugLoggingEnabled()` can be switched on in a release build by a persisted flag. | `AuthConfig.kt` — `prefs.getBoolean(KEY_DEVELOPMENT_MODE, BuildConfig.DEBUG)` (:101), `prefs.getBoolean(KEY_REQUIRE_EMAIL_VERIFICATION, false)` (:118), `shouldBypassEmailVerification() = developmentMode && !requireEmailVerification` (:231) |
| 4 | 🔴 BLOCK | **No migrations, RLS policies or storage policies are committed**, so the application's access control cannot be reviewed from the client side at all. | `supabase/` contains `config.toml` and one Edge Function; no `supabase/migrations` path exists in the tree inventory |
| 5 | 🟠 High | **`web/` still ships no security headers.** `next.config.mjs` defines `redirects()` only — no CSP, `frame-ancestors`, `Referrer-Policy` or `X-Content-Type-Options` on a site that renders user-authored text and avatar URLs. | `web/next.config.mjs` read in full |
| 6 | 🟠 High | **Four of five primary navigation targets do not exist.** `NAV_LINKS` points at `/search`, `/notifications`, `/inbox` and `/settings`; the only routes under `web/src/app/(app)/` are `home`, `profile`, `profile/edit` and `u/[username]`. `PROTECTED_ROUTES` in the Supabase middleware guards the same four dead paths. | `app-nav.tsx` and the full `web/src/app/**` inventory compared; `updateSession()` `PROTECTED_ROUTES` |
| 7 | 🟡 Medium | **`web/` route protection trusts `getSession()`.** `updateSession()` decodes the session cookie locally rather than verifying it with `getUser()`; data reads stay RLS-protected, so the impact is limited to rendering authenticated shells, but the guard is weaker than it reads. | `web/src/lib/supabase/middleware.ts` — `await supabase.auth.getSession()` |
| 8 | 🟡 Medium | **Uploads still trust the browser-supplied MIME type.** `validateProfileImage` accepts only `file.type`, and the stored extension *and* the served `contentType` are derived from it. | `web/src/lib/synapse/profile-upload.ts` — `if (!EXTENSIONS[file.type])`, `contentType: file.type` |
| 9 | 🟡 Medium | **Every release lint check is disabled.** `abortOnError = false` *and* `checkReleaseBuilds = false`; several findings in this report are exactly what those detectors exist to catch. | `app/build.gradle:181-183` |
| 10 | 🟡 Medium | **`versionCode` is derived from `git rev-list --count HEAD`**, which regresses on any rebase or shallow checkout and makes a newer APK fail to install over an older one. | `app/build.gradle:22` `getGitCommitCount`, :66 `versionCode = getGitCommitCount()` |

Everything else from the first pass — the ~27 empty catch blocks, the unbounded caches, the N+1 read paths, the 60 files whose `package` no longer matches their directory, the dead `SyncWorker`/`ActionQueue`/`app/.../S3Signer.kt`, the `FLAG_SECURE` gap, the Edge Function's authenticate-last ordering and unbounded fan-out loop — was **not** re-verified in this pass and should be treated as still open, with the first pass as the reference.

---

## Recommendations

1. **Rotate the signing key and untrack `app/release_key.jks`.** This is the only item on the list that no code change can compensate for, and it is the reason the build-hygiene grade is a D.
2. **Commit `supabase/migrations/`** before any further client review. Until the policies are in the repository, every "the server enforces it" comment in the Kotlin data layer stays unverifiable, including the cover-scoping change made here.
3. **Make the two cheap gates mandatory:** `npm ci && tsc --noEmit` for `web/` and `./gradlew compileDebugKotlin` for the Kotlin modules. The edits in this pass are small and local, but they still deserve a compiler before a merge.
4. **Fix the web shell before the next feature:** add the security headers from finding 5, then either implement the four missing routes or remove them from `NAV_LINKS` and `PROTECTED_ROUTES` — shipping navigation that 404s is worse than shipping fewer links.
5. **Finish the auth-posture item (finding 3).** `AuthConfig` already loads `keystore.properties`-style build values; making `BuildConfig` the only source in release variants, and every catch fall back to *closed*, removes a class of defects in one change.
6. **Treat `SecurityCipher` as one contract with a migration.** One AES-GCM implementation, a per-record IV prefixed to the ciphertext, a persisted non-exportable key per platform, and no plaintext fallback — with a documented migration path for values already written under ECB/CBC/XTEA.
7. **Adopt `getUser()` in the `web/` middleware** (finding 7) once the routes it guards actually exist, so the guard and the data layer agree on what "signed in" means.

### Positive observations worth preserving (this pass)

* The **release-critical fixes from the first pass are genuinely landed**, not just described: credential redaction in the TURN path, object-key sanitisation, the `chatId` guard in the new poll flow, and the awaited `ensureProfileRow` in the OAuth callback. The repository is not accumulating review debt on paper.
* **`supabase/server.ts` and `supabase/middleware.ts` are carefully written** — the cookie adapter documents exactly why it swallows writes from a Server Component, and the middleware explains why `.well-known/` is excluded from the matcher (passkey Digital Asset Links). That is the level of comment quality the rest of the web layer should match.
* **`safeNextPath()` already rejects protocol-relative redirects**, so the open-redirect class is closed on the web side.
* **The new `profile-upload.ts` writes uid-scoped cover paths** (`${uid}/cover-<ts>.ext`), which is precisely the invariant the signer fix now enforces — the data model and the check agree, so no legacy rows were invalidated by the change beyond the intended owner check.

---

## Appendix — First-Pass Review (2026-11-07, HEAD `cc97e14`; its 18 fixes landed in `2a577731`)

Retained verbatim: it is the deep reference for every area this second pass only re-checked. Line numbers in it refer to `cc97e14`.

# Code Review — synapseSRC/synapseApp

## Metadata

| Field | Value |
| --- | --- |
| Date | 2026-11-07 |
| Branch | `code-review` |
| HEAD reviewed | `cc97e14` — *feat(chat): add poll creation and improve message delivery reliability* |
| Scope | Whole repository: 1,808 tracked files (`:app` Android, `:shared` KMP, `:web` Next.js, `:desktop`, `:iosApp`, `supabase/`, `.github/`); 1,311 Kotlin files, ~155.7k Kotlin LOC |
| Status | **CHANGES REQUESTED** — 17 fixes applied across 18 files, 4 blocking items require maintainer action, 70+ findings open |
| Overall grade | Architecture **B-**, Security **C**, Correctness **B**, Performance **B**, Build hygiene **D** |

**Verification performed.** `web/` was type-checked with the project's own `tsc --noEmit` under the repo's `strict: true` config — **0 errors** after fixes. `web/package-lock.json` was re-synced and `npm ci` now succeeds. `AndroidManifest.xml` parsed as well-formed XML. All edited Kotlin files pass brace/paren balance checks. **Kotlin compilation was NOT verified**: no Android SDK is available in the review environment, and `AGENTS.md` forbids running test tasks. Every Kotlin change is therefore deliberately local and non-signature-affecting; `./gradlew compileDebugKotlin` on a developer machine remains a required pre-merge gate.

---

## Scanned & Fixed Files

### Files modified in this review (18)

| # | File path | Change |
| --- | --- | --- |
| 1 | `app/src/main/AndroidManifest.xml` | **Modified** — added `autoVerify` to 2 unverified HTTPS auth filters |
| 2 | `app/src/main/kotlin/com/synapse/social/studioasinc/data/call/TurnCredentialRepositoryImpl.kt` | **Modified** — stopped TURN credentials leaking into logs/UI errors |
| 3 | `app/src/main/kotlin/com/synapse/social/studioasinc/core/NotificationExtensionService.kt` | **Modified** — bitmap alias-recycle fix |
| 4 | `app/src/main/kotlin/com/synapse/social/studioasinc/core/util/FileUtils.kt` | **Modified** — path validation now returns the canonicalized value |
| 5 | `app/src/main/kotlin/com/synapse/social/studioasinc/core/util/UserProfileManager.kt` | **Modified** — removed full user-row PII dump from logcat |
| 6 | `app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/ChatAiDelegate.kt` | **Modified** — job-capture race + 4 public backing flows made private |
| 7 | `app/src/main/kotlin/com/synapse/social/studioasinc/feature/inbox/inbox/ChatSubscriptionDelegate.kt` | **Modified** — encapsulated `_typingStatus` |
| 8 | `app/src/main/kotlin/com/synapse/social/studioasinc/feature/settings/legacy/ApiKeySettingsService.kt` | **Modified** — removed unused `SupabaseClient` injection + dead imports |
| 9 | `shared/src/commonMain/.../shared/data/repository/SearchRepositoryImpl.kt` | **Modified** — `searchPosts` wildcard escaping made consistent |
| 10 | `shared/src/commonMain/.../shared/data/source/remote/SupabaseUploadService.kt` | **Modified** — object-key path traversal sanitized |
| 11 | `shared/src/commonMain/.../shared/data/sync/SupabaseMessageDeliveryCoordinator.kt` | **Modified** — poison-row infinite retry fixed |
| 12 | `shared/src/commonMain/.../shared/domain/usecase/chat/CreatePollUseCase.kt` | **Modified** — missing `chatId` validation |
| 13 | `app/src/main/kotlin/com/synapse/social/studioasinc/core/di/BackendModule_add_imports.txt` | **Deleted** — scratch file inside the Kotlin source tree |
| 14 | `app/proguard-rules.pro` | **Modified** — retained `SourceFile,LineNumberTable` for readable crash stacks |
| 15 | `web/package-lock.json` | **Modified** — re-synced with `package.json` so `npm ci` works |
| 16 | `web/src/components/synapse/app-nav.tsx` | **Modified** — sign-out can no longer permanently disable itself |
| 17 | `web/src/components/ui/copy-button.tsx` | **Modified** — clipboard success now reflects reality |
| 18 | `.gitignore` | **Modified** — added `keystore.properties`; removed a leaked assistant-style comment |

### Coverage by area

Every tracked file was included in the automated pattern scans (secrets, SQL/RPC string building, `GlobalScope`/`runBlocking`, empty catches, `!!`, package/path drift, Compose state patterns, platform imports in `commonMain`). Deep manual reading concentrated on the following, ranked by risk:

* **`:shared` crypto & transport** — `security/SecurityCipher` (all 4 `actual`s), `ChatEncryptionHelper.kt`, `GroupMetadataCipher.kt`, `SignalProtocolManager`, `SupabaseClient.kt`, `ChatRealtimeDataSource.kt`, `SupabaseChatRepository.kt`, `SupabaseUploadService.kt`, `SupabaseMessageDeliveryCoordinator.kt`, `ChatMessageDataSource.kt`, `SyncManager.kt`
* **`:shared` domain** — `SendMessageUseCase.kt`, `CreatePollUseCase.kt`, `UploadMediaUseCase.kt` (both), `GetSharedContentUseCase.kt`, `PlaySoundEffectUseCase.kt`, `ResetE2EUseCase.kt`, `MintTurnCredentialsUseCase.kt`, `ReelRepository.kt`, `SearchRepositoryImpl.kt`, `FollowRepositoryImpl.kt`, `SqlDelightCachedMessageDao.kt`
* **`:app` core/data** — `AndroidManifest.xml`, `TurnCredentialRepositoryImpl.kt`, `NotificationExtensionService.kt`, `AuthConfig.kt`, `SupabaseAuthenticationService.kt`, `UserProfileManager.kt`, `FileUtils.kt`, `UriUtils.kt`, `MediaCache.kt`, `MediaCacheCleanupWorker.kt`, `DownloadManager.kt`, `UploadProgressManager.kt`, `ActionQueue.kt`, `SyncWorker.kt`, `S3Signer.kt`, `ChatLockManager.kt`, `BiometricChecker.kt`, `ErrorHandler.kt`, `ImageLoader.kt`, `SynapseApplication.kt`, `DefaultWebRtcCallEngine.kt`, `SupabaseDatabaseService.kt`, `SettingsPreferences.kt`, DI modules
* **`:app` presentation** — `MainActivity.kt`, `ChatScreen.kt`, `ChatViewModel.kt`, `InboxScreen.kt`, `GroupInfoScreen.kt`, `MessageBubble.kt`, `AppNavigation.kt`, `AppLockScreen.kt`, `PrivacySecurityViewModel.kt`, `MediaContent.kt`, `SearchScreen.kt`, `FeedScreen.kt`, `VoiceRecordingController.kt`, `AuthViewModelExtensions.kt`, `SettingsNavHost.kt`, `ApiKeySettingsService.kt`
* **`web/` + `supabase/`** — `send-push-notification/index.ts`, `supabase/config.toml`, `profile-media.ts`, `profile-upload.ts`, `auth.ts`, `profiles.ts`, `posts.ts`, `middleware.ts`, `app-nav.tsx`, `login/page.tsx`, `auth/callback/route.ts`, `layout.tsx`, `u/[username]/page.tsx`, `next.config.mjs`, `copy-button.tsx`
* **Build/CI** — `app/build.gradle`, `build.gradle`, `gradle.properties`, `settings.gradle`, `.github/workflows/build.yml`, `desktop-build.yml`, `create-keystore.sh`, `notify_telegram.py`, `app/proguard-rules.pro`, `AGENTS.md`, `REVIEW.md`

---

## Findings & Fixes Applied

### 🔴 Security — fixed

**File:** `app/src/main/AndroidManifest.xml:217-223`, `:357-362`
**Issue:** Two HTTPS `intent-filter`s lacked `android:autoVerify`. Android only binds an unverified HTTPS filter to a domain if *some* app verifies it; without `autoVerify` **any installed app can declare the same host/path and be offered the link**. One of them is the OAuth / magic-link auth callback (`www.synapse.com/auth`, commented as a "Fallback for misconfigured Supabase redirect"), the other carries group invite codes (`synapse.chat/g/`). That is a credential-interception and account-takeover path available to any app on the device.
**Resolution:** Added `android:autoVerify="true"` to both filters and documented why. **Follow-up required:** publish `/.well-known/assetlinks.json` for both hosts, or delete the `www.synapse.com` filter outright if the project does not own that domain (`SynapseConfig` points at `socialsynapse.vercel.app`).

**File:** `app/src/main/kotlin/.../data/call/TurnCredentialRepositoryImpl.kt:101-110`
**Issue:** The catch-all logged and re-wrapped `e.message` into `CallBackendException`, which reaches logcat, `CrashActivity` and Sentry. When `json.decodeFromString(TurnCredentialsDto…)` fails, `kotlinx.serialization` quotes the offending slice of the response body — for this payload, the **TURN username and password**.
**Resolution:** Decode failures (`SerializationException`) now contribute only the exception type; transport errors keep their message. Added the import.

**File:** `shared/src/commonMain/.../data/source/remote/SupabaseUploadService.kt:37,94`
**Issue:** `fileName` was concatenated straight into the object key (`"${nowMillis()}_$fileName"`) and, for user-scoped buckets, into `"$userId/$objectName"`. A name containing `/`, `\` or `..` moves the write outside the caller's own prefix, defeating the per-user storage scoping — and no storage policies are committed to prove the server enforces it.
**Resolution:** Added `safeFileName` (basename only, `..` neutralized, control characters stripped, blank-guarded) used at both key-construction sites and in the log line.

**File:** `app/src/main/kotlin/.../core/util/FileUtils.kt:169-199`
**Issue:** `validateAndCleanPath` computed `canonicalPath`, validated against the policy, then **returned `cleanPath`** — the unresolved string. Symlinks and `..` segments survive into the caller, so the check certifies a path nobody uses (TOCTOU).
**Resolution:** Now returns `canonicalPath`, the value that was actually validated.

**File:** `app/src/main/kotlin/.../core/util/UserProfileManager.kt:22`
**Issue:** `android.util.Log.d(…, "Database result for uid $uid: $result")` dumped the **entire user row** — email, bio, linked-account fields — to logcat on every profile load, ungated by `BuildConfig.DEBUG`. Sentry's logcat breadcrumb module ships it with the next crash report.
**Resolution:** Reduced to a hit/miss signal; the row is no longer serialized to logs.

### 🟠 Correctness — fixed

**File:** `shared/src/commonMain/.../data/sync/SupabaseMessageDeliveryCoordinator.kt:121`
**Issue:** `pending.map { checkNotNull(it.id) { … } }` threw inside the acknowledgment loop. One ID-less row aborted the whole batch, so **no** pending delivery got acknowledged, and the realtime retry loop (`while (isActive …)`, capped only at 60s backoff) re-fetched the same page forever — permanent battery/network churn with deliveries never completing. Verified `acknowledgeReceivedMessages` additionally `require(ids.isNotEmpty())`, so an empty batch would throw too.
**Resolution:** `mapNotNull` + `if (ids.isEmpty()) break`. Confirmed `getPendingDeliveries` uses `limit(100)`, matching the receiver's `ids.size <= 100` precondition.

**File:** `app/src/main/kotlin/.../core/NotificationExtensionService.kt:88-110`
**Issue:** `Bitmap.createBitmap(src, x, y, side, side)` returns **the source object itself** when the rect covers the whole bitmap — which is every square avatar. `squared.recycle()` then recycled a bitmap Coil's application-wide singleton loader still holds in its memory cache, blanking avatars elsewhere in the app.
**Resolution:** Guarded with `if (squared !== source)`.

**File:** `shared/src/commonMain/.../data/repository/SearchRepositoryImpl.kt:74-75`
**Issue:** `searchPosts` used `query.trim().take(100)` with an inline comment claiming `%`/`_` need no escaping "as they are not wildcards in FTS". The very next line calls `ilike("post_text", "%$sanitizedQuery%")` — a `LIKE` pattern, not full-text search. A user typing `%` matches every post; a leading unescaped wildcard forces an unindexed scan. The three sibling lookups in the same file (`searchNews:147`, `searchHashtags:113`, `:202`) already call `sanitizeSearchQuery` against the identical operator, so this was an inconsistency, not a design.
**Resolution:** Switched to `sanitizeSearchQuery(query)` and corrected the comment. Import was already present.

**File:** `app/src/main/kotlin/.../feature/inbox/inbox/ChatAiDelegate.kt:79-80`
**Issue:** `activeJobs[smartRepliesJob!!] = Unit` then `smartRepliesJob!!.invokeOnCompletion { activeJobs.remove(smartRepliesJob!!) }` re-read the **field** at completion. `cancelPendingSmartReplies()` nulls it (NPE in the completion handler) or a newer generation replaces it (removes the wrong key, leaking the finished job into `activeJobs` permanently). Every other method in the same class already uses the correct `val job = launch { … }` pattern.
**Resolution:** Aligned `generateSmartReplies` with the existing pattern; the job is captured once and used in both places.

**File:** `shared/src/commonMain/.../domain/usecase/chat/CreatePollUseCase.kt:26`
**Issue:** Every input was validated except `chatId`; a blank value passed straight through to `SendMessageUseCase`.
**Resolution:** Added a blank check returning `Result.failure(IllegalArgumentException(...))`, consistent with the surrounding validation style and with the "never thrown" contract in its own KDoc.

### 🟡 Architecture & encapsulation — fixed

**File:** `app/src/main/kotlin/.../feature/settings/legacy/ApiKeySettingsService.kt:8-9,42`
**Issue:** Injected `SupabaseClient` into a presentation-layer `@Singleton` and never used it — both dead code and a direct `AGENTS.md` violation ("Direct backend SDK calls or provider imports in UI/ViewModels are strictly prohibited"), plus an unused `io.github.jan.supabase.auth.auth` import.
**Resolution:** Removed the constructor parameter and both imports. Verified there is no manual construction anywhere (Hilt-only), so no call site breaks.

**File:** `app/src/main/kotlin/.../feature/inbox/inbox/ChatAiDelegate.kt:30-39`, `ChatSubscriptionDelegate.kt:57`
**Issue:** Public `val _smartReplies/_chatSummary/_messageSummary/_isSummarizingMessage/_typingStatus = MutableStateFlow(...)`, exposing writable state to any caller — a `REVIEW.md` §3 unidirectional-flow violation.
**Resolution:** Made all five `private`; the public `StateFlow` views remain. Verified by grep that no file outside each class references the backing fields. Repo-wide only 5 such exposures existed, so this closes the pattern.
**Deliberately not changed:** `ChatMessagingDelegate.kt:48` `val _messages` has the same defect but is written from `ChatE2EDelegate.kt` and `ChatViewModel.kt` (10+ sites). Making it private requires adding a mutation method to the delegate — a cross-file refactor left for a compile-verified change.

### 🟣 Build & release integrity — fixed

**File:** `web/package-lock.json`
**Issue:** `package.json` and the lockfile were out of sync (`npm ci` failed: lock at `@supabase/supabase-js@2.118.0`, manifest requires `2.119.0`, across 5 packages). Any CI job or clean checkout using `npm ci` — the correct command for reproducible installs — **could not install**.
**Resolution:** Re-synced the lockfile via `npm install`; diff is contained (28 insertions / 70 deletions). `npm ci` now completes and installs 482 packages.

**File:** `app/proguard-rules.pro:41`
**Issue:** `-keepattributes` omitted `SourceFile,LineNumberTable`, and no CI step uploads `mapping.txt` to Sentry. Production crash reports are therefore anonymous `a/b/c` chains that cannot be triaged.
**Resolution:** Added `-keepattributes SourceFile,LineNumberTable` and `-renamesourcefileattribute SourceFile`. Purely additive to the existing rule.

**File:** `app/src/main/kotlin/.../core/di/BackendModule_add_imports.txt`
**Issue:** A 40-line scratch dump of import statements committed inside the Kotlin source tree, referenced by nothing.
**Resolution:** Deleted (`git rm`).

**File:** `.gitignore:66-68`
**Issue:** The comment `# I see 'app/release_key.jks' in your file listing, this MUST be excluded.` is a pasted assistant message committed to the repo. `keystore.properties` — the file `app/build.gradle:12-16` tries to load — was also not ignored.
**Resolution:** Removed the stray comment; added `keystore.properties`.

### ⚪ Quality — fixed

**File:** `web/src/components/ui/copy-button.tsx:20-29`
**Issue:** `setIsCopied(true)` ran *before* `navigator.clipboard.writeText(text)`, whose promise had no `.catch()`. `writeText` rejects outside a secure context or when permission is denied — the UI showed a success checkmark for a copy that never happened and left an unhandled rejection.
**Resolution:** State and the reset timer now resolve inside `.then()`; `.catch()` surfaces a failure toast. `tsc --noEmit` clean.

**File:** `web/src/components/synapse/app-nav.tsx:33-38`
**Issue:** `await …auth.signOut()` had no `try/catch/finally`. On a network failure the promise rejects, `setPending(false)` never runs, and the button stays disabled and spinning — the only sign-out control is permanently dead until reload.
**Resolution:** Wrapped in `try/finally` so `pending` always resets. `tsc --noEmit` clean.

---

## Findings Requiring Maintainer Action (not auto-fixed)

These are blocking, but a mechanical fix would break the release pipeline or user data. Each needs a deliberate decision.

**1. 🔴 BLOCK — Release signing key is published in this repository.**
`app/release_key.jks` is tracked (committed in the initial commit; `*.jks` was added to `.gitignore` later, which does not untrack it). `app/build.gradle:49-52` hardcodes its credentials alongside it:

```groovy
release {
    storeFile file("release_key.jks")
    storePassword "1111"
    keyAlias "ashik"
    keyPassword "1111"
}
```

`buildTypes.release.signingConfig = signingConfigs.release` means CI signs **every APK that gets uploaded and Telegram-distributed** with this key. Anyone who can read the repo can produce a same-signature APK that replaces users' installs. Aggravating factors: `app/build.gradle:12-16` already loads a `keystore.properties` that is never referenced, and `.github/workflows/create-keystore.sh` exists but is never invoked by any workflow.
**Why not auto-fixed:** untracking the key or rotating it invalidates the update signature for existing installations — a release-management decision with direct user impact, not a code edit.
**Required:** treat the key as compromised; move release signing to Google Play App Signing or a new key held only in CI secrets; wire the already-loaded `keystoreProperties` into `signingConfigs.release`; `git rm --cached app/release_key.jks`; use a throwaway key for CI-distributed test APKs.

**2. 🔴 BLOCK — Four divergent `SecurityCipher` implementations for one `expect` contract.**
| Platform | Reality |
| --- | --- |
| `jvmMain/SecurityCipher.jvm.kt:18,25` | `Cipher.getInstance("AES")` resolves to JCA's default **`AES/ECB/PKCS5Padding`** — no IV, no MAC. Worse, the key is regenerated by `KeyGenerator` in `init` on every instantiation, so previously written ciphertext is undecryptable after a restart. |
| `wasmJsMain/SecurityCipher.wasmJs.kt:11` | Hand-rolled **XTEA** with an embedded 128-bit key (`0x7F2B4A1D…`) shipped in the browser bundle, ECB-style per 8-byte block. The code comment concedes it only deters "casual inspection". |
| `iosMain/IosSecurityCipher.kt:19,105,133,138` | AES-**CBC** with PKCS7 and **no HMAC** (padding-oracle exposure), and every error path `return plainText`. |
| `androidMain/AndroidSecurityCipher.kt:52` | Correct AES-GCM + Keystore, but the catch block **falls back to writing plaintext**. |
**Why not auto-fixed:** changing a cipher's mode or key handling rewrites the on-disk format, and `IosSecurityCipher` is bound as the Koin `single<SecurityCipher>` for Keychain-adjacent storage. Fixing it blind means stranding existing encrypted data on real devices.
**Required:** one AES-GCM contract with a per-record IV prefixed to the ciphertext, a persisted non-exportable key per platform, **no plaintext fallback** (surface the failure instead), and a migration path for already-written values.

**3. 🔴 BLOCK — Fail-open security posture decided by device-writable storage.**
`data/remote/services/AuthConfig.kt:110-122,194-214` reads `require_email_verification` and `development_mode` from on-device encrypted prefs at runtime; `enableDevelopmentMode()` persists `requireEmailVerification = false` permanently, and `shouldBypassEmailVerification()` then returns true **in a release build**. `isDebugLoggingEnabled()` (`:103-108`) likewise lets a persisted flag switch PII logging on in production.
**Required:** in release variants, `BuildConfig` must be the only source for auth posture; gate `enableDevelopmentMode()` behind `BuildConfig.DEBUG`; make every catch fallback fail **closed**.

**4. 🔴 BLOCK — No SQL migrations, RLS policies, grants or storage policies are committed anywhere.**
`git ls-files supabase` returns only `config.toml` and one Edge Function. The whole client trusts RLS for every authorization decision — including the assumption that the public anon key is harmless — while the Edge Function reads every table with `service_role`, bypassing RLS entirely. **This review cannot verify the application's access control at all**, and every "the server enforces it" note in the Kotlin data layer is unverifiable.
**Required:** commit `supabase/migrations/*.sql` covering `users`, `posts`, `chats`, `chat_participants`, `notifications`, `notification_preferences` plus bucket policies, and review them as code.

---

## Recommendations

### Security — open

* **No commit contains a real API secret**, which is good news: no `sk-*`, `AIza*`, `AKIA*`, service-role key or private-key PEM anywhere in tracked source, and `web/.env.example` holds placeholders only. Remaining exposures are the public Supabase project ref + anon JWT in `gradle.properties:23-24` and duplicated as a hardcoded fallback in `SynapseConfig.jvm.kt:44-50` (defeats `isConfigured()`; delete the fallbacks and fail fast) and `GOOGLE_WEB_CLIENT_ID` in `gradle.properties` (public by design for the OAuth flow, but it should come from CI secrets for consistency with `build.yml:94`).
* **`supabase/functions/send-push-notification` is the highest-privilege component in the system and authenticates last.** `config.toml` sets `verify_jwt = false`; `index.ts:514-518` opens a Postgres connection and runs a Vault query **before** the secret comparison at `:518`, so every unauthenticated hit costs a DB connect — an amplification vector. `Access-Control-Allow-Origin: *` (`:6`) makes it reachable from any web page. Set `verify_jwt = true`, require `Authorization: Bearer <service_role>`, and move the check above the `postgres()` call.
* **Two content-disclosure paths in the same function.** `:563` fetches `posts.post_text` by an ID taken from the `notifications.data` blob and checks only `is_deleted` before putting it on a lock screen — the recipient's visibility rights are never verified. `:400` forwards the entire unvalidated `notifications.data` JSON to the device, where `buildNavigationPayload:321` uses it for in-app navigation; copy an allow-list of keys instead.
* **`index.ts:467` has an unbounded `while (true)` fan-out loop** (100 participants per page, batches of 8 sequential OneSignal calls, no iteration or deadline cap). On a large group it is killed at the Edge wall-clock limit and reports partial fan-out as success. Cap iterations and return a cursor.
* **E2EE degrades to plaintext silently.** `SendMessageUseCase.kt:96-98,101-117`: metadata serialization failures are swallowed, a null `SignalProtocolManager` sends **plaintext** with only a `Napier.w` unless `requireE2EE` is explicitly passed, and self-encryption failure is discarded so a user's other devices can never decrypt. Default `requireE2EE = true` and make these failures fatal.
* **`MainActivity.kt:79-120,126-130` enforces the biometric app lock only in `onCreate`** — a process resumed from background reopens unlocked, with no `onStart`/`onResume` re-check. `isAppLockEnabledBlocking()` is a stub that hardcodes `return true`; note this fails *secure* (it delays content until the real check resolves), so it is a maintainability problem rather than a bypass. `AppLockScreen.kt:41-44` also no-ops silently when the context is not a `FragmentActivity` or biometrics are unavailable: the switch flips, nothing persists, no message appears.
* **`FLAG_SECURE` / `setSensitive` appears zero times** across `app/` and `shared/`. End-to-end encrypted chats, BYOK API keys, passkeys and account-request data are all visible in screenshots and in the Recents thumbnail.
* **Manifest hardening:** `HomeActivity` (`:135`) and `PostDetailActivity` (`:151`) are `exported="true"` with no intent-filter and no permission; `WRITE_MEDIA_STORAGE` (`:16`) is `signature|privileged` and can never be granted; five `synapse://` **custom-scheme** filters carry `autoVerify="true"`, which only applies to HTTP(S) and so documents a protection the app does not have — treat `synapse://` input as untrusted.
* **`UploadProgressManager.kt:14,18,38,44`** uses a plain `mutableMapOf` inside a `@Singleton`, mutated concurrently by upload coroutines (HashMap resize race) and never bounded. **`MediaCache.kt:85,242-248`** mutates a `Collections.synchronizedMap` *outside* its lock while `evictLRU` iterates it with `iterator.remove()` → `ConcurrentModificationException`. **`SynapseApplication.kt:193-227`** does volatile read-modify-write on `isTracking` from both Main and IO.
* **Two live `MediaCache` instances over the same directory** (`MediaCacheCleanupWorker.kt:74` and `ManageStorageViewModel.kt:26`), each rebuilding `currentCacheSize` by a full `listFiles()` scan; the nightly worker evicts against counters the running app does not share and can delete files it still considers cached. Provide it as a Hilt `@Singleton`.
* **Web layer has no security headers at all.** `next.config.mjs` defines only `redirects()`: no CSP, no `frame-ancestors`/`X-Frame-Options`, no `Referrer-Policy`, no `X-Content-Type-Options`, on a site rendering user-authored post text, bios and avatar URLs.
* **`web/src/lib/synapse/profile-media.ts:7-25` is a signed-URL oracle.** `resolveCoverUrl` mints a 1-hour signed URL for *any* path passed in `users.profile_cover_image` (`private-media://covers/<arbitrary-path>`, plus `decodeURIComponent`), and the result is rendered on the **public** `/u/[username]` page. Require the path to begin with `${profile.uid}/`.
* **Reflected content in UI.** `web/src/app/(auth)/login/page.tsx:14` renders `searchParams.error` verbatim inside a system-styled error alert, and `auth/callback/route.ts:14` forwards `error_description` straight through — attacker-supplied text can impersonate app messaging. `describeAuthError` (`lib/synapse/auth.ts:31`) falls through to `return message`, and `u/[username]/page.tsx:74` prints raw PostgREST errors on an indexable public page. Route all of these through an error-code allow-list map.
* **`build.yml:181-189`** caches `bot_session.session` — a persistent Telethon login credential — under the guessable static key `telegram-session-${{ runner.os }}-bot_session`. Same-repo PRs can restore that key and exfiltrate the session. Authenticate with `BOT_TOKEN` and drop the cache.
* **`data/remote/services/SupabaseDatabaseService.kt:152,170,191`** passes caller strings verbatim into `Columns.raw(...)` and accepts an untyped `table` name across insert/update/delete/select — one unguarded gateway to every table in the project, safe only while RLS holds and no caller forwards user input. It is currently reachable from `UserProfileManager` in the presentation layer.
* **`core/storage/providers/S3Signer.kt` is both dead and wrong** — unreferenced (the live copy is `shared/.../S3Signer.kt`, used by `R2UploadService.kt:67`), with `canonicalQuery = ""` hardcoded and `Content-Type` *guessed* from `conn.getRequestProperty` rather than read from the headers actually sent. Delete it.

### Architecture — open

* **The domain layer is not pure**, and not via platform imports — there are **zero** `android.*`/`java.*`/`androidx.*` imports in `shared/commonMain`, verified repo-wide. The boundary is crossed three other ways: domain→data (`SendMessageUseCase.kt:6-7`, `ResetE2EUseCase.kt:3`, `RegisterDeviceUseCase.kt:3`), domain→Ktor (`GetSharedContentUseCase.kt:15`, `domain/repository/FileUploader.kt:3` — `REVIEW.md` §1 explicitly forbids this), and domain models carrying `@SerialName("user_id")` so they *are* the wire contract (`ReelInteraction.kt:6-13`). The last one is the root cause of every "DTOs never reach Domain" violation, so it is one bug, not many.
* **UseCase contract violations against `AGENTS.md`:** `UploadMediaUseCase.kt:19,32` declares two public `operator fun invoke` overloads *and* a second same-named class exists at `domain/usecase/chat/UploadMediaUseCase.kt:11`; `PlaySoundEffectUseCase.kt:27,43-45` holds mutable state in an `invoke`; `SendMessageUseCase.kt:31` exposes extra public methods. ~11 UseCases throw or `getOrThrow()` raw exceptions instead of returning a sealed failure type.
* **Presentation has no state contract.** Screens take ViewModels as composable parameters rather than an immutable state object plus an `onAction` lambda (97 sites); `ChatScreen.kt:102-151` collects **41 separate `StateFlow`s** with no atomic snapshot; `MainActivity.kt:100` and `SettingsNavHost.kt:49` build repository singletons inside the composition; `AuthViewModelExtensions.kt:15-50` mutates `MutableStateFlow.value` from free functions outside the ViewModel.
* **60 Kotlin files declare a `package` that no longer matches their directory path** (verified programmatically) — `feature/settings/legacy/ApiKeySettingsService.kt` declares `com.synapse.social.studioasinc.settings`, `feature/createpost/createpost/*` declare `...ui.createpost`, `feature/profile/editprofile/*` declare `...presentation.editprofile`. Legal in Kotlin, so it compiles silently, but it defeats IDE navigation, refactoring tools and any package-based architecture test. A one-off reorganization commit.
* **Service-locator vs Hilt, unresolved.** `AuthHelper` is an `object` building `SupabaseAuthRepository()` with `by lazy` (`AuthHelper.kt:8-15`), `UserProfileManager`/`AutoDownloadManager`/`SupabaseApplication` use `getInstance(context)`, and `SupabaseAuthenticationService.kt:40-44` throws at runtime when not initialized — two live instances of auth state depending on call order, with no compile-time guarantee.
* **God files.** `SupabaseChatRepository.kt` (1,130 lines / 60 suspend functions) owns messages, conversations, groups, reactions, media, E2EE, receipts and presence; `GroupInfoScreen.kt` (1,391), `ChatViewModel.kt` (1,283), `MessageBubble.kt` (1,177), `ChatScreen.kt` (1,024), `InboxScreen.kt` (877). The N+1 patterns, unbounded caches and swallowed exceptions cluster here precisely because no unit is small enough to reason about.
* **`expect/actual` over-use:** four `SecurityCipher` actuals and four `PlatformUtils` actuals (all four still hashing with SHA-1) let each platform silently under-implement one contract at four different security levels. `REVIEW.md` §2 already says to prefer injected interfaces.

### Quality — open

* **27 empty catch blocks** (`catch (… : Exception) { }`) across 10 files, concentrated in `ChatRealtimeDataSource.kt` (`:356,357,460,561,577,600,615,629,630,634,659`), `ChatScreen.kt:607-618` (7 around `uriHandler.openUri`, silently killing failed navigations), `VoiceRecordingController.kt:296,322,333,597` (failed mic release leaks the recorder), `ChatEncryptionHelper.kt:127,149,159`, `SynapseApplication.kt:382`. Several are genuinely intentional and commented; the `realtime.connect()` one at `:379` is not — a failed socket connect is discarded and subscription proceeds, so the inbox silently stops updating.
* **`collectAsState()` is used 201 times vs `collectAsStateWithLifecycle()` 66.** Backgrounded flows keep collecting, draining battery on a chat app with realtime subscriptions open.
* **Lazy lists without keys:** `SearchScreen.kt:317,329,342`, `FollowingSection.kt:75`, `FollowListScreen.kt:140`, `SelectRegionScreen.kt:108,147` have no `key =`; `SearchScreen.kt:389,412,434,460` use `"${it.id}_$index"` keys, which every list mutation invalidates. State classes carrying `List`/`Map` fields are almost never annotated `@Immutable` (7 files in scope, vs 10+ state classes), so recomposition leaks.
* **~30 sites discard results**: `runCatching { … }` with the value dropped (`PostDetailViewModel.kt:98,106,110,131,133`), `onFailure = { }` double-swallowed with an empty catch (`AvatarViewModel.kt:47,51-53`), and `ensureProfileRow()` discarded in `auth/callback/route.ts:23` and `auth-forms.tsx:347` — producing an account with **no `users` row** that dead-ends at `profile/page.tsx:27` with no diagnostics.
* **`!!` on nullable constructor parameters**: `ChatEncryptionHelper.kt:318` (`signalProtocolManager!!` while lines 319/322/326 use the same nullable property relying on a prior check), `MediaContent.kt:119,175` (`post: Post? = null` default followed by `val safePost = post!!` — a guaranteed NPE for any caller omitting it), `GroupMetadataCipher.kt:110,140`, `SupabaseChatRepository.kt:895`, `uiState.error!!` at `EchoLobbyContent.kt:95` / `GroupInviteScreen.kt:87` / `ChatEncryptionSheet.kt:366`.
* **Unbounded in-memory caches, no eviction**: `ChatEncryptionHelper.kt:42` `decryptedMessageCache`, `SupabaseChatRepository.kt:89,99,147-153` `chatCacheMutexes`/`chatLastSyncedAt`, `UploadProgressManager.kt:14`. On a long-lived chat process these grow with total message count.
* **Concurrency primitives absent where they matter most.** `SyncManager.kt:59-61,76` uses plain `mutableMapOf` registries written by a non-suspend `registerSyncer` and read from IO coroutines — a hard crash on Kotlin/Native (iOS), exactly what `REVIEW.md` §2 warns about. `SyncManager.kt:65` leaves a `MutableSharedFlow` on the default `SUSPEND` overflow while `emit` is called from the sync loop, so syncing stalls with no collector attached. `ChatMessageDataSource.kt:195` builds an unchunked `isIn(...)` unlike the correct `chunked(50)` at `:70`. Notably there is **not a single `.catch {}` on any Flow** in `shared/` — any exception inside the many `map`/`flatMapLatest` operators that touch network or DB tears the collector down.
* **Performance.** N+1 everywhere in the hot read path: `SqlDelightCachedMessageDao.kt:58,79,100` issues one reaction query per message inside `.map`, three times over, with no transaction; `ReelRepository.kt:61-64` awaits a signed-URL HTTP call per reel and again per thumbnail (a 20-reel page ≈ 40 round-trips); `SendMessageUseCase.kt:53-58` loops `repository.ensureSession(recipientId)` serially for group sends while the encryption loop directly below already uses `async`/`awaitAll` correctly; `ChatEncryptionHelper.kt:318-326` generates fresh pre-keys on every bundle build. `markRead` (`SqlDelightCachedMessageDao.kt:191-206`) hard-caps at 500 messages so long chats never fully mark read, and stores `read_by` as a comma-joined string.
* **Dead code shipped as if live:** `SyncWorker.kt` (114 lines) is never enqueued anywhere and its logic is wrong even if wired — per-action failures bump `retryCount` but the worker still returns `Result.success()`, and `else -> true` at `:80` deletes any unrecognized action type *unapplied*. `ActionQueue.kt` (135 lines) has no references outside itself, and `proguard-rules.pro:43-47` keeps `PendingAction` fields solely for that dead class. `ImageLoader.kt:24-72` is a private, uncalled ~50-line retry helper.
* **Error text shown to users is the raw exception message.** `ErrorHandler.kt:143-144` returns `exception.message` (backend constraint names, table names, URLs) and `:159` writes `MessageId` + `UserId` + the full exception to logcat unconditionally. `web/src/app/(app)/u/[username]/page.tsx:74` and `profile/page.tsx:70` do the same on public pages.
* **Unvalidated client-side trust:** `profile-upload.ts:15` accepts only the browser-supplied `file.type` (no magic-byte sniffing) and derives both the stored extension and served `contentType` from it; `UriUtils.kt:79-84` takes `ext` from a provider-controlled `DISPLAY_NAME` and passes it to `File.createTempFile`, which throws `IllegalArgumentException` on a separator-bearing suffix that the `catch` at `:91` does not cover; `DownloadManager.kt:99-105` accepts a remote `fileUrl` with no scheme/host allow-list or size cap and dispatches callbacks from `Dispatchers.IO` into UI; `AppSettingsManager`/`DraftRepositoryImpl`/`PermissionOnboardingStore` and two more keep plaintext `MODE_PRIVATE` prefs (draft post bodies included) while three other stores use EncryptedSharedPreferences with no documented rule.
* **Navigation surface is broken on web:** `app-nav.tsx:22-26` links to `/search`, `/notifications`, `/inbox`, `/settings` — none exist under `web/src/app/(app)/` (only `home`, `profile`, `profile/edit`, `u/[username]`), yet `middleware.ts:6` treats them as protected. 4 of 5 primary nav items 404.
* **No `error.tsx` or `global-error.tsx` anywhere in `web/`.** `getProfileByUid()` throws from `profiles.ts:30` and is uncaught in `layout.tsx:12`, so one transient DB error 500s every signed-in page.
* **Build config:** `lint.checkReleaseBuilds = false` **and** `abortOnError = false` (`app/build.gradle:182-183`) disable every release lint check — the exported-component, crypto and `StringSet` detectors would have caught several findings in this report on their own. `versionCode = git rev-list --count HEAD` (`:22-27`) **regresses** on any rebase or shallow history, making a new APK fail to install. `android.enableR8.fullMode=false` (`gradle.properties:7`) weakens obfuscation. `proguard-rules.pro:3-5` sets blanket `-ignorewarnings`/`-dontwarn`/`-dontnote`, removing R8's only signal for missing classes. `build.yml:88` skips `-x :app:checkReleaseAarMetadata`. `pip install telethon` is unpinned with no hash. `CoreModule.kt:118-130` builds a Ktor client with no `expectSuccess` and no timeouts, which makes `GitHubReleaseApi.kt:47-53`'s `ReleaseRateLimitedException` **unreachable** — a 429 body gets parsed as a release list.
* **`supabase/` has no `service_role` guard**: `verify_jwt = false` plus the DB-work-before-auth ordering above is the single most exposed endpoint in the system.

### Testing

`AGENTS.md`'s testing policy forbids agents from writing or running tests, so none were added here — and the gap is structural rather than incidental. `shared/` has ~460 Kotlin files and the repository contains no common-code test suite that exercises the E2EE decryption ladder, the delivery coordinator's retry loop, or the cipher contracts. Two of the three correctness bugs fixed above (the poison-row retry, the `activeJobs` key mismatch) are exactly the class of defect a single unit test each would have caught. Recommend the maintainers add tests for `SupabaseMessageDeliveryCoordinator.synchronize()`, `ChatEncryptionHelper.decryptIfNecessary()` and one cross-platform `SecurityCipher` contract test, and wire `./gradlew compileDebugKotlin` plus `npm ci && tsc --noEmit` as required status checks — the lockfile drift fixed above would have been caught by the latter on the first push.

### Positive observations worth preserving

Worth stating explicitly, because the codebase is not uniformly weak and some fixes should not be applied mechanically:

* **Zero SQL-injection vectors.** Every PostgREST and RPC call binds parameters (`ChatGroupDataSource`, `ChatMessageDataSource`, `ChatConversationDataSource`); no interpolated SQL string exists in the repository. `.sq` files contain no `${}`.
* **Zero `GlobalScope`.** Coroutine hygiene at that level is clean.
* **`ChatRealtimeDataSource` and `NotificationExtensionService` correctly rethrow `CancellationException`** before their broad catches (`ChatEncryptionHelper.kt:146,159`) — a subtlety most codebases of this size get wrong.
* **`TurnCredentialRepositoryImpl` keeps credentials memory-only with half-TTL rotation**, and `SupabaseStorageImageInterceptor.kt:72-80` pins the bearer token to the configured Supabase host, so the push-avatar fetch is not a token-exfiltration path. `OkHttp` is pinned to 5.3.2, so cross-host redirect header stripping applies. `provider_paths.xml` is properly scoped. `CallNotificationHelper` PendingIntent request codes are correctly per-`(callId, action)`. `SentryInitializer.kt:17-23` is genuinely well configured. `supabase/functions` re-validates group membership server-side (`index.ts:471-478`) rather than trusting the payload's recipient.
* **`REVIEW.md` and `AGENTS.md` are unusually specific**, and the `.gitignore` shows the team already understood the keystore problem — it just never untracked the file.
* Comment quality in the data layer is high and mostly *explanatory* rather than restating the code (the delivery coordinator, the TURN credential path, the WebRTC engine threading notes). Note that two comments in this codebase assert incorrect justifications, and both are load-bearing for a bug: `SearchRepositoryImpl.kt:74` claims a full-text search where the code does `ilike`, and `core/storage/providers/S3Signer.kt` is left in place by a comment describing behavior it does not implement. Treat a confident comment as a claim to verify, not as evidence.

### Suggested sequencing for the next iteration

1. Signing key rotation and `supabase/migrations` — the two items no code change can substitute for.
2. `SecurityCipher` unification with a migration, then remove every plaintext fallback in `AuthConfig` / `SendMessageUseCase` / `AndroidSecurityCipher`.
3. One sealed failure type across `shared/` and a review rule banning `getOrElse { emptyList() }` and plaintext fallbacks; that single convention removes most of the 27 empty catches and the fail-open reporting in `FollowRepositoryImpl` / `SupabaseChatRepository.searchMessages`.
4. Make `./gradlew compileDebugKotlin` and `npm ci && tsc --noEmit` required checks, and re-enable `lint.checkReleaseBuilds` — three of the findings in this report are lint-detectable.
5. Add `error.tsx` + CSP headers to `web/`, implement or remove the 4 dead nav routes, then fix `profile-media.ts` scoping.
6. Package/path drift (60 files) and `ChatMessagingDelegate._messages` encapsulation — both safe once a compile gate exists.

