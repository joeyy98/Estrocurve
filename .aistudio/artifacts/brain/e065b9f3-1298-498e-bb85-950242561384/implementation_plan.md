# Remove Network and Nearby Devices Permissions

Strip all network (`INTERNET`, `ACCESS_NETWORK_STATE`, `READ_GSERVICES`) and nearby devices (`BLUETOOTH_*`, `NEARBY_WIFI_DEVICES`) permissions to guarantee zero network telemetry, total device-local privacy, and minimum system permission overhead in Android Settings.

## User Review & Critical Decisions

> [!IMPORTANT]
> The user confirmed the following:
> - **Permission Elimination Strategy**: Remove unused network libraries (`firebase-ai`, `appcheck-recaptcha`, `retrofit`, `okhttp`, `moshi`) and explicitly block network and nearby devices permissions via `tools:node="remove"` in `AndroidManifest.xml`.
> - **Privacy Assurance**: The app operates 100% offline. Zero network calls or wireless discovery protocols are needed or permitted.

- **Confirmed Decision 1**: Comment out unused template dependencies in `app/build.gradle.kts` (`firebase.ai`, `firebase.appcheck.recaptcha`, `firebase.appcheck.debug`, `retrofit`, `okhttp`, `logging.interceptor`, `converter.moshi`, `moshi.kotlin`, and KSP moshi codegen).
- **Confirmed Decision 2**: In `app/src/main/AndroidManifest.xml`, declare `tools:node="remove"` for `INTERNET`, `ACCESS_NETWORK_STATE`, `READ_GSERVICES`, and all Bluetooth/Nearby Devices permission identifiers (`BLUETOOTH`, `BLUETOOTH_ADMIN`, `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE`, `NEARBY_WIFI_DEVICES`).
- **Confirmed Decision 3**: Retain only essential local system permissions strictly necessary for local notifications and exact alarm rescheduling: `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, and `VIBRATE`.

---

### 1. Overview & Core Concept

- **What It Does**: EstroCurve is an entirely offline, on-device pharmacokinetics tracker and simulator. Previously, transitive dependencies from the initial AI Studio template (Firebase AI Logic SDK, reCAPTCHA Enterprise, Retrofit, and OkHttp) automatically injected `ACCESS_NETWORK_STATE` and `INTERNET` into the merged manifest. Additionally, system settings in some OEM Android versions categorize certain broad SDK tags under "Nearby devices". This change eliminates all unused network libraries and strictly purges these permissions from the final application package.
- **Target Audience / Persona**: Users managing sensitive personal hormone tracking data who require verifiable, zero-network on-device privacy.
- **Key Value**: The app will request and hold **zero network permissions** and **zero nearby device permissions** in Android system settings and Google Play privacy dashboards.

---

### 2. User Experience & Visual Design

#### Android App Info & Permissions Screen
In Android Settings > Apps > EstroCurve > Permissions:
```
┌────────────────────────────────────────────────────────┐
│ EstroCurve                                             │
│ App info                                               │
├────────────────────────────────────────────────────────┤
│ Permissions                                            │
│   ALLOWED:                                             │
│   • Notifications (for local dose alarms)              │
│                                                        │
│   NOT REQUESTED / NOT ALLOWED:                         │
│   • Network / Internet           [None]                │
│   • Nearby devices / Bluetooth   [None]                │
│   • Location                     [None]                │
│   • Contacts / Photos / Storage  [None]                │
└────────────────────────────────────────────────────────┘
```

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Dual-Layer Removal (Dependency Removal + Manifest Removal)**:
  - *Chosen Approach*: Clean up `app/build.gradle.kts` by commenting out unused network libraries AND declare explicit `tools:node="remove"` in `AndroidManifest.xml`.
  - *Why*: Commenting out dependencies stops transitive inclusion at the Gradle level and reduces APK size and compilation time. Adding `tools:node="remove"` in `AndroidManifest.xml` acts as an absolute guardrail preventing any future dependency from silently injecting network or Bluetooth permissions into the merged APK manifest.
- **Decision 2: Retained Normal Permissions**:
  - *Chosen Approach*: Retain `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, and `VIBRATE`.
  - *Why*: These are essential for scheduling offline local dose alarms and vibrating the phone when an injection is due.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│                   app/build.gradle.kts                 │
│                                                        │
│  Comment out unused network/SDK dependencies:          │
│   • // implementation(libs.firebase.ai)                │
│   • // implementation(libs.firebase.appcheck.recaptcha)│
│   • // implementation(libs.firebase.appcheck.debug)    │
│   • // implementation(libs.retrofit)                   │
│   • // implementation(libs.okhttp)                     │
│   • // implementation(libs.logging.interceptor)        │
│   • // implementation(libs.converter.moshi)            │
│   • // implementation(libs.moshi.kotlin)               │
│   • // "ksp"(libs.moshi.kotlin.codegen)                │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│                 AndroidManifest.xml                    │
│                                                        │
│  Explicit removal rules via tools:node="remove":       │
│   • INTERNET                                           │
│   • ACCESS_NETWORK_STATE                               │
│   • READ_GSERVICES                                     │
│   • BLUETOOTH / BLUETOOTH_ADMIN                        │
│   • BLUETOOTH_SCAN / BLUETOOTH_CONNECT                 │
│   • BLUETOOTH_ADVERTISE                                │
│   • NEARBY_WIFI_DEVICES                                │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│                    Merged Manifest                     │
│                                                        │
│  Resulting Permissions:                                │
│   [✓] POST_NOTIFICATIONS (Local alarms)                │
│   [✓] RECEIVE_BOOT_COMPLETED (Alarm restoration)       │
│   [✓] VIBRATE (Alert feedback)                         │
│   [✗] Zero network permissions                         │
│   [✗] Zero nearby device permissions                   │
└────────────────────────────────────────────────────────┘
```

#### Files to Modify:
1. `app/build.gradle.kts`:
   - Comment out `libs.firebase.ai`, `libs.firebase.appcheck.*`, `libs.retrofit`, `libs.okhttp`, `libs.logging.interceptor`, and `libs.moshi.*`.
2. `app/src/main/AndroidManifest.xml`:
   - Add `xmlns:tools="http://schemas.android.com/tools"` (already present).
   - Add `<uses-permission android:name="..." tools:node="remove" />` blocks for all network and nearby device permissions.
3. Unit tests:
   - Add `PermissionAuditTest.kt` verifying that the merged manifest does not contain network or nearby device permissions.

---

### Verification Plan
- Run `compile_applet` to confirm clean compilation and dependency resolution without unused libraries.
- Inspect the generated manifest in `app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml` to verify that `INTERNET`, `ACCESS_NETWORK_STATE`, and nearby device permissions are completely gone.
- Run `gradle :app:testDebugUnitTest` to ensure all existing unit tests and the permission audit test pass.
