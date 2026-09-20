# NUViA

NUViA is a modern, high-performance Android music player crafted with Jetpack Compose. Built with an AMOLED-first aesthetic, dynamic artwork-driven atmospheric lighting, and fluid liquid-glass interface design, NUViA delivers an immersive, personalized listening experience.

<div align="center">
  <br/>
  <img src="Logo.png" alt="NUViA logo" width="100%" />
  <br/>
</div>

## Features

### Playback & Audio
- **Seamless Music Streaming**: Search, browse, and play millions of tracks, albums, and playlists via YouTube Music.
- **High-Fidelity Audio**: Modular lossless and high-resolution stream sourcing with intelligent fallback.
- **Gapless Playback & Crossfade**: Smooth continuous transitions with configurable crossfade curves (0–12s).
- **Intelligent Audio Analysis**: Advanced on-device beat detection, energy curve mapping, and cue-point detection.
- **Background Media Playback**: Persistent foreground media session with full system lockscreen and notification controls.
- **Local Audio Support**: Scan and play local music files stored directly on your device.
- **Offline Downloads**: Download your favorite tracks with embedded high-resolution tags and album artwork.

### Atmosphere & Design
- **AMOLED-First Interface**: Pure deep black canvas engineered for modern OLED and AMOLED displays.
- **Dynamic Artwork Atmosphere**: Real-time palette extraction reflecting vibrant ambient glows and accents tailored to the active album art.
- **Liquid-Glass Surfaces**: Translucent glass layers, frosted backdrops, and tactile elevation.
- **Synchronized Lyrics**: Rich word-by-word and line-by-line synchronized lyrics with real-time highlighting.
- **Cinematic Motion & Animations**: Fluid layout transitions, smooth swipe gestures, and dynamic visualizer effects.

### Controls & Connectivity
- **Playback Fine-Tuning**: Playback speed adjustment (0.5×–2.0×), silence skipping, and system equalizer integration.
- **Sleep Timer**: Flexible timer options including custom durations and "Stop after current track".
- **Discord Rich Presence**: Live playback status integration displaying currently playing tracks, artist details, and elapsed time.
- **Scrobbling**: Automatic scrobbling to Last.fm and Libre.fm.
- **Audio Stats**: In-depth audio technical metrics including codec, bitrate, sample rate, and cache diagnostics.

## Experience

NUViA is designed around the philosophy that listening to music should be visually engaging and tactile. Every surface reacts dynamically to the music playing — adapting its ambient backlight, specular borders, and color tones to the active track's artwork palette while maintaining crisp legibility and smooth 60/120fps motion.

## Installation

### Prerequisites
- Android device running **Android 8.0 (API 26)** or higher.
- Sideloading enabled ("Install unknown apps" permission for your browser or file manager).

### Building from Source
1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd NUViA
   ```
2. Build the debug APK with Gradle:
   ```bash
   ./gradlew assembleDebug
   ```
3. The compiled APK will be available in `app/build/outputs/apk/debug/app-debug.apk`.

## Downloads

Official builds and release APKs will be available on the [NUViA Releases] page.

> *Note: Release download links will be populated upon official tagged releases.*

## Disclaimer & Legal Notice

NUViA is an independent third-party audio player client. It is **not** affiliated with, authorized, maintained, sponsored, or endorsed by YouTube, Google LLC, or any of their affiliates or subsidiaries.

- **No Media Hosting**: NUViA does not host, store, or distribute copyrighted media files. It functions strictly as a client interface interacting with public or user-authenticated APIs and scanning local device storage.
- **API & Fair Use**: This software is developed for personal research, educational, and fair-use purposes. Users are responsible for ensuring their usage complies with applicable local laws and relevant platform Terms of Service.
- **License**: NUViA is open-source software licensed under the GNU General Public License v3.0 (GPLv3).

## Developer

Developed by **Adhil CLT**.

## License

This project is licensed under the **GNU General Public License v3.0 (GPLv3)**. See the [LICENSE](LICENSE) file for details.
