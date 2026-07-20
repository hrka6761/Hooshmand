# `:core:download_manager`

Standalone Android library for reliable single-file and multipart HTTP downloads via
**OkHttp** + **WorkManager**. No Hooshmand-specific dependencies — drop it into any app.

## Package layout

```
ir.hrka.download.manager/
├── api/                 Public façade (DownloadManager, listeners, permissions, factory)
├── model/               Public config / status types
├── error/               Typed DownloadError + DownloadException hierarchy
├── PublicModels.kt      Typealiases for root-package imports (compat)
└── internal/
    ├── transfer/        OkHttp engine + shared client
    ├── storage/         Internal / External / Public file providers
    ├── work/            WorkManager worker + control + progress
    ├── listener/        WorkInfo → DownloadListener bridge
    └── notification/    Foreground notification UI
```

## Setup (host app)

1. Depend on the module.
2. Register `DownloadWorkerFactory` via `Configuration.Provider` (see `DownloadManagerHostRequirements`).
3. Request runtime permissions with `DownloadManagerPermissions` before starting downloads.

## Usage

### Single-file

```kotlin
DownloadManager.Builder(context)
    .setSingleItemDownloadData(
        SingleItemDownloadData(url = url, fileName = "file.bin", fileSize = size),
    )
    .setFileLocation(DownloadStorageLocation.Internal)
    .setDownloadListener(listener)
    .build()
    .startDownload()
```

### Multipart (parts appended in order into one file)

```kotlin
DownloadManager.Builder(context)
    .setMultiPartsDownloadData(
        MultipartItemDownloadData(
            itemParts = listOf(
                SingleItemDownloadData(url = part1, fileName = "file.bin", fileSize = s1),
                SingleItemDownloadData(url = part2, fileName = "file.bin", fileSize = s2),
            ),
            totalFileSize = s1 + s2,
        ),
    )
    .setDownloadListener(listener)
    .build()
    .startDownload()
```

## Errors

Failures are published as [DownloadError] with a stable `code` and user-facing `userMessage`.
Override `DownloadListener.onDownloadFailed(error: DownloadError)` to branch on `error.code`,
or use the `String?` overload for simple UI messages.

## Independence

- Package: `ir.hrka.download.manager` (not app-specific)
- Dependencies: AndroidX Work, OkHttp, Coroutines only
- Host wires storage location, permissions, and WorkManager factory
