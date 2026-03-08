# Repository Guidelines

## Project Structure & Module Organization
This repository is a Paper plugin written in Kotlin.
- Core source: `src/main/kotlin/dev/star122o/tethersCore`
- Managers: `.../manager` (`DatabaseManager`, `PowerManager`)
- Power types: `.../powers`
- Plugin metadata: `src/main/resources/plugin.yml`
- Build tooling: `build.gradle.kts`, `settings.gradle.kts`, `gradle/`

There is currently no `src/test` directory. Add tests under `src/test/kotlin` when introducing test coverage.

## Build, Test, and Development Commands
Use the Gradle wrapper from the repo root:
- `./gradlew build` (Windows: `./gradlew.bat build`): compiles and packages the plugin.
- `./gradlew shadowJar`: builds the shaded plugin JAR.
- `./gradlew runServer`: starts a local Paper server for manual testing.
- `./gradlew clean`: removes build outputs.

If Gradle wrapper download is blocked, set a local cache: `GRADLE_USER_HOME=.gradle-user`.

## Coding Style & Naming Conventions
- Follow Kotlin defaults: 4-space indentation, no tabs.
- Use `PascalCase` for classes/objects, `camelCase` for methods/variables, and `UPPER_SNAKE_CASE` for enum constants.
- Keep package names lowercase (`dev.star122o.tethersCore...`).
- Prefer small, single-purpose manager methods and clear domain names (`LinkType`, `PlayerLink`).
- Keep plugin-facing strings concise and actionable.

## Testing Guidelines
No automated test framework is configured yet. For now:
- Validate behavior with `runServer` and in-game command flows.
- Smoke-test DB-linked features (SQLite file creation, insert/update paths).
- When adding tests, use JUnit under `src/test/kotlin` and name files `*Test.kt`.

## Commit & Pull Request Guidelines
Recent history uses short subject lines with optional prefixes, e.g. `(feat) power management and database system`.
- Prefer: `(feat|fix|refactor|chore) short imperative summary`.
- Keep commits scoped to one change.
- PRs should include: purpose, key files changed, manual test steps, and any config/plugin.yml impact.
- Link related issues/tasks when available.
