# AccessChecker — Decompiled APK Source

This folder contains the full Java source code decompiled from `AccessChecker-v1.4-debug.apk` using [jadx](https://github.com/skylot/jadx).

## Why this exists

AccessChecker requests elevated permissions (Shizuku / root) on your device. You should be able to verify exactly what it does before trusting it. This folder lets anyone read every class file directly on GitHub without downloading or installing anything.

## App source (our code)

The AccessChecker application classes are at:

```
sources/com/accesschecker/
  MainActivity.java        — UI, scoring logic, animations
  RootChecker.java         — su binary checks, root manager detection
  BootloaderChecker.java   — bootloader lock state, verified boot, dm-verity
  ShizukuChecker.java      — Shizuku availability, permission, run mode
  R.java                   — generated resource IDs
```

## Library code

Everything else under `sources/` is decompiled third-party library code bundled into the APK:

- `androidx/` — AndroidX / AppCompat / Material
- `rikka/` — Shizuku API (dev.rikka.shizuku)
- `kotlin/` — Kotlin standard library (used by AndroidX internally)

## Reproducibility

The APK was built with `minifyEnabled false` and the following ProGuard rules, which prevent any obfuscation or shrinking:

```
-dontobfuscate
-dontshrink
-dontoptimize
-keepnames class ** { *; }
```

This means class names, method names, and field names in the decompiled output are identical to the original source.

## How to decompile yourself

```bash
# Download jadx from https://github.com/skylot/jadx/releases
jadx -d apk-java-source AccessChecker-v1.4-debug.apk --no-res
```
