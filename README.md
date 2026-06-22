# Soularium v2

Cru-internal mobile rebuild of the discontinued Soularium and MySoularium
apps, built with Kotlin Multiplatform and Compose Multiplatform (Android + iOS).

## Documentation

Project docs live in `docs/superpowers/`:

- `specs/2026-05-20-soularium-v2-design.md` — design spec
- `plans/2026-05-20-soularium-v2-mobile.md` — implementation plan
- `HANDOFF.md` — current state and what's next

## Building

Requires JDK 17 (the repo pins `temurin-17.0.19+10` via `.tool-versions`; with
asdf, `export JAVA_HOME=~/.asdf/installs/java/temurin-17.0.19+10` first).

```bash
cd mobile
./gradlew :androidApp:assembleDebug                          # Android APK
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64        # iOS framework
./gradlew ktlintCheck                                        # lint all modules
./gradlew :shared:allTests                                   # tests
```

## CI and GitHub secrets

GitHub Actions workflows live in `.github/workflows/`:

- `ci.yml` — build + test on every PR and push to `main` (no secrets needed)
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
