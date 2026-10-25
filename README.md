# Auralis

A dark, gradient-soaked music player for Android that treats **mood as both a look and a sound**. It plays the music already on your phone, keeps playlists and favourites locally, and can pull new audio from YouTube using an **on-device yt-dlp engine** — no backend, no API keys, no accounts.

```
Library ──► Moods ──► Playback  (+ optional YouTube search / link download)
```

## Features

### Library & playback
- Reads everything Android's MediaStore exposes (Android 10+ via `READ_MEDIA_AUDIO`, older devices via `READ_EXTERNAL_STORAGE`).
- Songs, albums, artists, playlists, liked songs, recently added and most-played views.
- One shared ExoPlayer with a `MediaSessionService`, so playback survives the screen turning off and gets notification, lock-screen, headset and Bluetooth controls.
- Shuffle, three repeat modes, speed control, seek, queue reordering, sleep timer with an 8-second fade-out.
- Synced lyrics from a sidecar `.lrc` file next to the audio file (`Song.lrc`).
- Listening stats and history stored in Room.

### Moods (visual + audio)
Ten presets repaint the UI and gently retune playback at the same time. Auto-tune can be switched off in Studio, and the Player's **Tune** sheet gives manual control over speed, bass, warmth, air, stereo space and reverb.

| Mood | Character | Audio profile |
| --- | --- | --- |
| Midnight | Clean, deep, after hours | flat |
| Slow Motion | 0.85×, velvet bass | bass +4.2 dB, soft highs, small room |
| Lo-Fi | Tape-warm, rolled-off highs | warm bass, −7 dB highs, short reverb |
| Neon | Synthwave punch | heavy bass, +3.4 dB air, wide stereo |
| Sunset | Golden hour | gentle low/high lift, 0.96× |
| Forest | Vocal clarity | +3.2 dB mids, calm air |
| Ocean | Wide and weightless | wide stereo, medium hall |
| Vapor | Pastel haze | airy, wide, 1.02× |
| Ember | 1.08×, gym-ready | strong bass, loudness lift |
| Coffee | Warm room, soft top | warm lows, soft highs |

### Discover (online, optional)
- Search YouTube and preview results with title, channel, duration and thumbnail.
- Paste any YouTube link (watch, youtu.be, Shorts, embed) and fetch its metadata.
- Choose **M4A / MP3 / OPUS / FLAC** and a bitrate (best, 320, 256, 192, 128 kbps), see an estimated size, then download.
- Sequential download queue with live progress, ETA, cancel and dismiss.
- Files land in the app's private external music folder, so **no storage permission is needed for downloads**.
- Audio thumbnails are written next to the track and used as artwork.
- The bundled engine is **yt-dlp 2025.11.12** (from [github.com/yt-dlp/yt-dlp](https://github.com/yt-dlp/yt-dlp)). A button in Discover pulls the latest release binary when YouTube changes something.
- Everything degrades gracefully offline: the local library, playlists and moods keep working, and Discover shows an offline banner.

## How the on-device engine works

```
yt-dlp (bundled binary)
  + private Python runtime (unpacked from native libs on first use)
  + ffmpeg (for audio extraction/conversion)
  + QuickJS (for YouTube's JS challenges)
        │
        ▼
runs as a local process — phone ⇄ YouTube directly
```

- Provided by [`io.github.junkfood02.youtubedl-android`](https://github.com/JunkFood02/youtubedl-android) `library` + `ffmpeg` **0.18.1**.
- First use unpacks roughly 40 MB into `noBackupFilesDir`; this is why the first search takes a moment.
- Native libraries must be extracted (`android:extractNativeLibs="true"` + `useLegacyPackaging = true`), which is already configured.
- The engine talks only to YouTube (and GitHub when you press *Update*). No analytics, no telemetry, no server of ours.

## Install

Grab the APK from a GitHub Actions run or a release, then install it:

```bash
adb install -r Auralis-1.0.0-debug.apk
```

Requires Android 7.0 (API 24) or newer. The debug build installs as `dev.auralis.debug`, so it can live beside a release build.

## Build it yourself

You need JDK 17 and the Android SDK (platform 35, build-tools matching AGP 8.7.3).

```bash
git clone <your-fork-url> auralis
cd auralis
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Only ARM ABIs are packaged by default (`arm64-v8a`, `armeabi-v7a`) because the Python/ffmpeg payload is large. To include emulator ABIs:

```bash
./gradlew :app:assembleDebug -Pabis=arm64-v8a,armeabi-v7a,x86_64
```

Release build:

```bash
./gradlew :app:assembleRelease
```

Without a keystore the release build falls back to the debug signing key, so the APK is always installable. For a proper signature, set:

```bash
export AURALIS_KEYSTORE=/absolute/path/auralis.jks
export AURALIS_KEYSTORE_PASSWORD=…
export AURALIS_KEY_ALIAS=…
export AURALIS_KEY_PASSWORD=…
./gradlew :app:assembleRelease
```

## GitHub Actions

`.github/workflows/android.yml` runs on every push to `main`/`master`, on pull requests, and manually via **Actions → Build APK → Run workflow**.

- **Debug APK** job: builds `assembleDebug` and uploads `Auralis-1.0.0-debug.apk` as the `Auralis-debug-apk` artifact.
- **Signed release APK** job: runs on tags beginning with `v` (e.g. `v1.0.0`) or when a GitHub Release is published; uploads `Auralis-release-apk` and attaches the APK to the release.

To sign releases in CI, add these repository secrets (Settings → Secrets and variables → Actions):

| Secret | Meaning |
| --- | --- |
| `AURALIS_KEYSTORE_BASE64` | `base64 -w0 auralis.jks` output |
| `AURALIS_KEYSTORE_PASSWORD` | keystore password |
| `AURALIS_KEY_ALIAS` | key alias |
| `AURALIS_KEY_PASSWORD` | key password |

Create a keystore if you do not have one:

```bash
keytool -genkeypair -v -keystore auralis.jks -alias auralis \
  -keyalg RSA -keysize 4096 -validity 10000
base64 -w0 auralis.jks   # paste as AURALIS_KEYSTORE_BASE64
```

Without the keystore secret, CI still produces an installable debug-signed release APK.

## Permissions

| Permission | Why |
| --- | --- |
| `READ_MEDIA_AUDIO` (33+) / `READ_EXTERNAL_STORAGE` (≤32) | read your existing music |
| `POST_NOTIFICATIONS` (33+) | show the playback notification |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | keep playback alive with the screen off |
| `INTERNET`, `ACCESS_NETWORK_STATE` | optional YouTube search/download |
| `WAKE_LOCK`, `MODIFY_AUDIO_SETTINGS` | playback reliability and audio effects |

Downloads are written to `Android/data/dev.auralis/files/Music/Auralis/` (app-private external storage) — no broad storage access required.

## Project layout

```
app/src/main/java/dev/auralis/
├── AuralisApp.kt          Application wiring (repository, player, engine, downloads)
├── MainActivity.kt        Edge-to-edge Compose host
├── MainViewModel.kt       UI state and actions
├── core/                  Settings, network monitor, formatters, .lrc parser
├── data/                  Room entities/DAO/database, MediaStore scanner, repository
├── online/                yt-dlp engine wrapper + sequential download manager
├── player/                Shared ExoPlayer, audio effects, MediaSessionService
└── ui/                    Theme/moods, screens, sheets, reusable components
```

Stack: Kotlin 2.0.21 · AGP 8.7.3 · Jetpack Compose (BOM 2024.12.01) · Material 3 · Media3 1.5.1 · Room 2.6.1 · DataStore 1.1.1 · Coil 2.7.0 · youtubedl-android 0.18.1.

## Troubleshooting

**Search fails or YouTube says "Sign in to confirm you're not a bot".**
YouTube changes often. Open Discover → *yt-dlp engine* → **Update**. If it still fails, wait for a newer yt-dlp release; the update button always pulls the latest stable binary from the official repository.

**First search is very slow.**
Expected — the Python runtime, ffmpeg and QuickJS are unpacked on first use. Later searches start quickly.

**Downloads finish but do not appear.**
Open Library and pull to rescan, or check Discover's task card for the error. If the format conversion failed, try M4A or MP3.

**No notification controls.**
Allow notifications for Auralis (Android 13+). Battery optimisation can also stop background playback on some OEMs; exempt Auralis from battery restrictions.

**Nothing in the library.**
Grant audio permission, then Studio → *Rescan device music*.

**Emulator build fails to run the engine.**
Rebuild with `-Pabis=arm64-v8a,armeabi-v7a,x86_64` (or your host ABI).

## Legal & licence

- Auralis is free software under the **GNU General Public License v3.0** (see [LICENSE](LICENSE)). This is required because it links against `youtubedl-android`, which is GPL-3.0.
- Auralis is not affiliated with YouTube or yt-dlp. Download only content you have the right to download, and respect YouTube's Terms of Service and your local copyright law.
- yt-dlp itself is distributed under the Unlicense; ffmpeg components carry their own licences (see the upstream projects).
