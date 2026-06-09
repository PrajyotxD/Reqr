# Reqr SDK

A lightweight Android network inspection SDK written in Java. Reqr hooks into OkHttp via a custom interceptor, stores all traffic in a local Room database, and surfaces it through a native floating overlay UI — no PC, no USB, no proxy required.

---

## Features

- **Zero-config integration** — two lines of code, nothing else to configure
- **OkHttp interceptor** — captures method, URL, headers, request/response bodies, status codes, and timing
- **Floating bubble** — draggable overlay badge always visible over your app; tap to open the inspector
- **Live log list** — real-time stats for 2xx, 3xx, and errors with per-request timing
- **Request detail view** — full headers and body for both request and response
- **Binary-safe** — skips binary payloads (images, files) and enforces a 1 MB body cap to prevent OOM
- **Exception capture** — network failures (timeouts, DNS errors) are logged with `responseCode = 0`
- **Production safe** — the SDK is a strict no-op in release builds; zero overhead when shipped

---

## Requirements

| Item | Value |
|---|---|
| Min SDK | 29 (Android 10) |
| Language | Java 11 |
| Dependency | OkHttp 4.x |

---

## Integration

### 1. Add the module

In your root `settings.gradle`, confirm the module is included:

```gradle
include ':app', ':reqr-sdk'
```

In your `app/build.gradle`:

```gradle
dependencies {
    implementation project(':reqr-sdk')
}
```

### 2. Initialize the SDK

Call `NetInspect.init()` in your Activity's `onCreate` **and** `onResume`. The `onResume` call is needed so the floating bubble starts immediately after the user grants the overlay permission — without it you'd need to relaunch the app.

```java
import xyz.log.reqr_sdk.NetInspect;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    NetInspect.init(this);
}

@Override
protected void onResume() {
    super.onResume();
    NetInspect.init(this);
}
```

On first launch, if the "Display over other apps" permission hasn't been granted, `init()` automatically opens the system settings screen for you.

### 3. Attach the interceptor

Add `NetInspectInterceptor` to your `OkHttpClient` builder:

```java
import xyz.log.reqr_sdk.interceptor.NetInspectInterceptor;

OkHttpClient client = new OkHttpClient.Builder()
        .addInterceptor(new NetInspectInterceptor(this))
        .build();
```

That's it. Every request made through this client will now appear in the Reqr overlay.

> **Tip:** If you want to inspect raw 3xx redirect responses rather than the final resolved URL, add `.followRedirects(false)` to the builder.

---

## How it works

```
OkHttpClient  ──►  NetInspectInterceptor
                        ├─ captures request (method, URL, headers, body)
                        ├─ chain.proceed()  ──►  actual network call
                        ├─ captures response (code, headers, body, duration)
                        └─ async insert ──►  Room DB (network_logs)
                                                  └─ LiveData ──►  LogListActivity
                                                                        └─ LogDetailActivity

FloatingBubbleService  (WindowManager overlay)
    └─ tap  ──►  LogListActivity
```

The interceptor runs the database write on a background thread — it never touches the main thread and won't affect your app's network performance.

---

## Permissions

The SDK declares these permissions in its own `AndroidManifest.xml`. They are merged into your app automatically — you don't need to add them manually.

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

`INTERNET` is required by your app for network calls and should already be present in your app's manifest.

---

## What gets captured

| Field | Description |
|---|---|
| Method | GET, POST, PUT, DELETE, etc. |
| URL | Full URL including query params |
| Host / Path | Split for display in the log list |
| Request headers | All headers as key: value pairs |
| Request body | Plain text only; binary skipped |
| Response code | HTTP status, or `0` on network failure |
| Response headers | All headers as key: value pairs |
| Response body | Plain text only; capped at 1 MB |
| Duration | Round-trip time in milliseconds |
| Timestamp | Unix epoch ms, used for ordering |

---

## Demo app

The `app` module is a minimal integration example. It includes four buttons that exercise all visual states in the inspector:

| Button | Endpoint | What it tests |
|---|---|---|
| Test 200 (Success) | `jsonplaceholder.typicode.com/users/1` | Green / 2xx path |
| Test 301 (Warning) | `httpbingo.org/status/301` | Amber / 3xx path |
| Test 500 (Error) | `httpbingo.org/status/500` | Red / 5xx path |
| Test Exception | Invalid host | Red / `responseCode = 0` (IOException) |

---

## Technical stack

| Component | Library |
|---|---|
| HTTP interception | OkHttp 4.12.0 |
| Local storage | Room 2.6.1 |
| Reactive UI updates | LiveData (Lifecycle 2.6.2) |
| Overlay UI | WindowManager (`TYPE_APPLICATION_OVERLAY`) |
| UI components | AppCompat + Material 1.14.0 |
| Build language | Java 11 |

---

## Known limitations

- Logs are **session-scoped** — they persist across app restarts until you tap the clear button in the inspector. There is no automatic cap on log count; for apps with very high request volume, clear periodically.
- Only **OkHttp** is supported. Retrofit works automatically since it uses OkHttp under the hood. Other HTTP clients (HttpURLConnection, Ktor, Volley) are not intercepted.
- The overlay requires the **"Display over other apps"** permission. On Android 10+ this must be granted by the user via system settings; `NetInspect.init()` handles the prompt automatically.
- The SDK is **Java-only**. It is compatible with Kotlin projects — just call the same two lines from your Kotlin Activity.
