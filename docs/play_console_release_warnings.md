# Play Console — common AAB upload warnings

Reference for warnings seen after uploading an Android App Bundle to Google Play (e.g. Internal testing). These are **warnings**, not errors — they do not block rollout.

City Grid citizen app context: `app/build.gradle.kts` has `isMinifyEnabled = false` and depends on LiteRT (`com.google.ai.edge.litert:litert`), which ships native `.so` libraries.

### What to do now (both warnings)

1. **Do not block rollout** — warnings are safe to ignore for internal testing.
2. **Save → Review release → Start rollout to Internal testing.**
3. Share the tester opt-in link and install from Play Store.
4. Defer full fixes until a later release — see **Remedy** sections below.

---

## 1. No deobfuscation file (R8 / ProGuard)

**Play Console message:**

> There is no deobfuscation file associated with this App Bundle. If you use obfuscated code (R8/proguard), uploading a deobfuscation file will make crashes and ANRs easier to analyze and debug. Using R8/proguard can help reduce app size.

### What it means

Play expects a **mapping file** (`mapping.txt`) when release builds use R8/ProGuard to shrink and obfuscate Java/Kotlin code. The mapping turns obfuscated stack traces (e.g. `a.b.c()`) back into real class and method names in **Android vitals** and crash reports.

### City Grid today

- `app/build.gradle.kts` → `release { isMinifyEnabled = false }`
- Code is **not** obfuscated → **no mapping file exists** → **nothing to upload**

### What to do now

| Situation | Action |
|-----------|--------|
| First internal testing upload (current build) | **Ignore the warning** — no mapping file to upload |
| Play Console screen | Add release notes → **Review release** → **Start rollout** |
| Do not | Enable R8 or upload a mapping file unless you have already turned minify on |

You are done with this warning for the current release. See **Remedy** below when you enable obfuscation later.

### Remedy

#### Remedy A — Current setup (recommended for now)

No code or Play Console action required.

1. Confirm `isMinifyEnabled = false` in `app/build.gradle.kts` (already set).
2. **Proceed with rollout** — the warning is informational only.
3. Java/Kotlin crash stack traces in Play Console will already show real class names (nothing is obfuscated).

#### Remedy B — Enable shrinking later and clear the warning properly

Use this before a production release when you want a smaller APK and readable obfuscated crashes.

**Step 1 — Enable R8 in Gradle** (`app/build.gradle.kts`):

```kotlin
release {
    isMinifyEnabled = true
    isShrinkResources = true   // optional: removes unused resources
    proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
    )
}
```

**Step 2 — Add keep rules** in `app/proguard-rules.pro` for libraries that break when minified (test after each change):

```proguard
# Kotlin serialization / Supabase / Compose — add keeps as needed after testing
-keepattributes *Annotation*
-keep class kotlinx.serialization.** { *; }
-keep class io.github.jan.supabase.** { *; }
```

Run a full smoke test on a release build (sign-in, report submit, map, ML) before uploading.

**Step 3 — Build release bundle:**

```powershell
cd C:\Users\priye\AndroidStudioProjects\potholereport
.\gradlew bundleRelease
```

Or Android Studio → **Build → Generate Signed Bundle / APK**.

**Step 4 — Upload mapping file to Play Console:**

| Item | Location |
|------|----------|
| File on disk | `app/build/outputs/mapping/release/mapping.txt` |
| When to upload | Immediately after each new `versionCode` upload |

Play Console path (UI may vary slightly):

1. **Test and release** → your track (Internal / Production)
2. Open the release that contains the new AAB
3. Under the uploaded bundle → **Upload deobfuscation file** / **ProGuard mapping file**
4. Select `mapping.txt` for that exact `versionCode`

Alternative: **Test and release → App bundle explorer** → select version → **Downloads** → upload mapping.

**Step 5 — Archive mapping files offline**

Store `mapping.txt` in secure backup labeled by version, e.g. `mapping-v1.0-versionCode-1.txt`. Without it, you cannot decode crashes for that build.

---

## 2. Native code — no debug symbols

**Play Console message:**

> This App Bundle contains native code, and you've not uploaded debug symbols. We recommend you upload a symbol file to make your crashes and ANRs easier to analyze and debug.

### What it means

The AAB includes native libraries (`.so` files). Without debug symbols, native crashes show only memory addresses, which are hard to diagnose in Play Console.

### Why City Grid sees this

The app bundles **prebuilt** native code from dependencies (notably **LiteRT** for on-device ML). Play detects `.so` files in the bundle even if you do not write C/C++ yourself.

### What to do now

| Situation | Action |
|-----------|--------|
| First internal testing upload (current build) | **Ignore the warning** — does not block install or testing |
| Play Console screen | Continue rollout on the same release as warning 1 |
| Do not | Rebuild or re-upload solely to clear this warning before testers install |

Optional on a **future** `versionCode`: add `ndk { debugSymbolLevel = "SYMBOL_TABLE" }` (see **Remedy B**). The warning may still show for LiteRT’s prebuilt libraries.

### Remedy

#### Remedy A — Current setup (recommended for now)

No action required for internal testing.

1. **Proceed with rollout** — warning does not block install or testing.
2. Native crashes from third-party `.so` files (LiteRT) are rare in normal use; if they occur, stack traces may show addresses only until symbols are provided.

#### Remedy B — Bundle native symbols on future uploads (your own NDK code)

When you compile C/C++ with the Android NDK, embed symbol tables in the AAB so Play extracts them automatically.

**Step 1 — Add to `release` in `app/build.gradle.kts`:**

```kotlin
release {
    isMinifyEnabled = false
    ndk {
        debugSymbolLevel = "SYMBOL_TABLE"   // recommended: smaller, enough for Play vitals
        // debugSymbolLevel = "FULL"        // use if you need line-level native debugging
    }
    proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
    )
}
```

**Step 2 — Rebuild and re-upload:**

```powershell
.\gradlew bundleRelease
```

Upload the new `app-release.aab` with an incremented `versionCode`.

**Step 3 — Verify in Play Console:**

1. **Test and release → App bundle explorer**
2. Select the new version
3. **Downloads** tab → confirm **Native debug symbols** are present

Play reads symbols from the bundle; you usually do **not** upload a separate file when `debugSymbolLevel` is set.

#### Remedy C — Manual symbol upload (if Play still asks)

Only needed if you build native code separately or use a CI artifact:

| Item | Location |
|------|----------|
| AGP output (typical) | `app/build/outputs/native-debug-symbols/release/native-debug-symbols.zip` |

Play Console:

1. **Test and release → App bundle explorer** → select version
2. **Downloads** → **Native debug symbols** → **Upload**
3. Upload the `.zip` matching that `versionCode`

#### Remedy D — Third-party native libs (LiteRT) only

If the app has **no** project-owned NDK code and only uses prebuilt Maven AARs:

- Apply **Remedy B** anyway on the next release (harmless; helps any symbols AGP can attach).
- The warning **may still appear** — Google does not ship public symbol files for LiteRT’s prebuilt `.so` files.
- **Acceptable for production** unless you maintain your own native layer; monitor crashes under **Android vitals**.

---

## Quick decision table

| Warning | Blocks release? | What to do now | Full remedy later |
|---------|-----------------|----------------|-------------------|
| Deobfuscation file | No | Ignore — minify is off; start rollout | **Remedy B** — enable R8 + upload `mapping.txt` |
| Native debug symbols | No | Ignore — proceed with same rollout | **Remedy B/C** — `debugSymbolLevel` + rebuild; **Remedy D** if only vendor `.so` |

**After upload:** Save → Review release → Start rollout (Internal testing or other track).

---

## Related docs

- [release_and_versioning_guide.md](release_and_versioning_guide.md) — §7 Citizen app release, §10 Play Store upload
- `app/build.gradle.kts` — `applicationId`, `versionCode`, `versionName`, release build type
- `app/proguard-rules.pro` — keep rules when enabling R8

---

*First noted on internal testing upload for `in.citygrid.citizen` v1.0 (versionCode 1).*
