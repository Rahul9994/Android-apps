# 🧠 Aura — the phone that talks back

An AI companion that notices when you're doom-scrolling, talks back out loud over the app you're in, and locks apps on request. See the [main README](../README.md) for screenshots and details.

**Open** this folder in Android Studio, or run `./gradlew installRelease` with a phone connected.

| Path | What's there |
|---|---|
| `service/AuraWatchService.kt` | Accessibility service: foreground tracking, nudge rules, app locks |
| `service/OverlayController.kt` | Compose overlays drawn above other apps |
| `service/Voice.kt` | Text-to-speech with audio ducking |
| `ai/` | Groq / OpenRouter client with auto model selection, prompts & offline fallbacks |
| `ui/components/AuraOrb.kt` | The living orb |
| `ui/screens/` | Onboarding, Today, Talk, Apps, You |
