# Soularium v2

Cru-internal mobile rebuild of the discontinued Soularium and MySoularium
apps, built with Kotlin Multiplatform and Compose Multiplatform (Android + iOS).

## Modules

- `:module:model` — `@Serializable` domain models (`org.cru.soularium.model`); no other
  in-repo dependencies.
- `:module:db` — Room persistence plus the `SessionRepository` contract
  (`org.cru.soularium.db`); depends on `:module:model`.
- `:shared` — session state machine, Compose UI, Circuit presenters, navigation, Metro DI,
  and DeviceState; depends on `:module:db` and `:module:model`.
- `:androidApp` — the Android application shell; depends on `:shared`. The iOS app
  (`iosApp/`) embeds `:shared` as a framework.
- `build-logic/` — convention plugins (`soularium-kmp.module-conventions`,
  `serialization-conventions`, `metro-conventions`, plus ktlint/kover/paparazzi).

See `.claude/CLAUDE.md` for the full architecture.

## Documentation

Project docs live in `docs/superpowers/`:

- `specs/2026-05-20-soularium-v2-design.md` — design spec
- `plans/2026-05-20-soularium-v2-mobile.md` — implementation plan
- `HANDOFF.md` — current state and what's next

## Building

JDK is pinned via `.tool-versions` (currently `temurin-25.0.3+9.0.LTS`); the
Gradle wrapper (9.x) drives the build. Source/target bytecode is JVM 17.

```bash
./gradlew :androidApp:assembleDebug                          # Android APK
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64        # iOS framework
./gradlew ktlintCheck                                        # lint all modules
./gradlew allTests                                           # tests (all modules, host + iOS sim)
```

## CI and GitHub secrets

GitHub Actions workflows live in `.github/workflows/`:

- `build.yml` — build + test on every PR and push to `main`/`feature/*`/`release/*`
  (Android APK, iOS framework, ktlint, Android lint, Android host tests, iOS
  simulator tests). No secrets needed.
- `crowdin-upload.yml` — pushes source strings to Crowdin on every push to `main`
- `crowdin-download.yml` — weekly pull of translations from Crowdin, opens a PR

The Crowdin workflows require one repository secret:

| Secret | Description |
| --- | --- |
| `CROWDIN_PERSONAL_TOKEN` | Crowdin personal access token with project read/write scope |

The Crowdin project ID is hardcoded in `crowdin.yml`. Until the token secret is
configured the Crowdin workflows are wired but inert.

## Firebase

`google-services.json` (Android) and `GoogleService-Info.plist` (iOS) are
gitignored. Copy the `example.*` templates and fill in Cru's real Firebase
config; analytics and crash reporting are no-op until then.
