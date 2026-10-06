# 💪 Protein Pulse — AI protein tracker

Type what you ate, get protein and calories from AI, smart suggestions for your next meal, and an end-of-day report. See the [main README](../README.md) for screenshots and details.

**Open** this folder in Android Studio, or run `./gradlew installRelease` with a phone connected.

| Path | What's there |
|---|---|
| `ai/AiClient.kt` | Groq / OpenRouter client with automatic fallback when a model is retired |
| `ai/OfflineCoach.kt` | Suggestions & reports without an API key |
| `data/LocalFoodEstimator.kt` | Offline protein estimates for ~50 foods |
| `ui/components/ProteinRing.kt` | Animated goal ring |
| `work/DailyReportWorker.kt` | Evening summary notification |
