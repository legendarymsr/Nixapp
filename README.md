# NixDoc Browser

An Android documentation browser for **NixOS**, **Nixpkgs**, **GNU Guix**, and the **Guix Cookbook**.

## Features

- Main screen listing all 4 documentation sources
- **Read Online** – opens the live docs in a built-in WebView
- **Download Offline** – saves the docs locally for offline reading
- Dark theme UI throughout
- WebView with automatic dark mode CSS injection
- **Find in Page** search bar with prev/next navigation
- Back / Forward / Refresh toolbar controls
- Background download service with status notifications

## Download APK

Pre-built debug APK is available in [`releases/NixDocBrowser-v1.0-debug.apk`](releases/NixDocBrowser-v1.0-debug.apk).

Requires Android 7.0+ (API 24).

## Build from Source

```bash
# Requirements: JDK 11+, Android SDK (API 34 + build-tools 34.0.0)
export ANDROID_HOME=/path/to/android-sdk
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

## Docs Included

| Name | URL |
|------|-----|
| NixOS Manual | https://nixos.org/manual/nixos/stable/ |
| Nixpkgs Manual | https://nixos.org/manual/nixpkgs/stable/ |
| GNU Guix Manual | https://guix.gnu.org/manual/en/html_node/ |
| Guix Cookbook | https://guix.gnu.org/cookbook/en/html_node/ |
