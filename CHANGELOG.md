# SpeakIDE Changelog

## [Unreleased]
- Fixed STT provider cache not invalidating when Base URL or model name changes in settings.
- Fixed redundant settings object being passed to `SttProviderFactory` (settings are a singleton).

## [0.1.2]
- Added **Whisper Local** backend — runs a ggml Whisper model on-device via whisper.cpp JNI, no internet required.
- Improved audio resampling quality: switched from linear interpolation to a sinc-based algorithm (TarsosDSP) for better transcription accuracy with Whisper Local.
- Fixed hallucination filter for Whisper Local — added missing Russian subtitle phrases (`субтитры создавал`, `продолжение следует`, `dimatorzok`).
- Refactored offline STT providers: extracted shared lazy-loading logic into `OfflineSttProvider` base class.

## [0.1.0]
- Initial release of SpeakIDE!
- Added two STT backends: OpenAI Whisper (Cloud) and Vosk (Offline).
- Introduced auto-stop on silence detection.
- Added seamless integration with AI Assistant / Junie chat interfaces.
- Added clipboard fallback when no editor is focused.
