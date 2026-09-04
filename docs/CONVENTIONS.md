# 📐 Coding & Commit Conventions

This document defines the code quality, testing standards, and collaboration rules for the
`mybatis-sql-tuner-ai` project.

---

## 1. Java Coding Standards

* **Language version**: Targets Java 17 at minimum, and must build without warnings or errors on Java 17 through 25+.
* **Logging**:
  - Do not use Lombok (`@Slf4j`); explicitly declare `org.slf4j.LoggerFactory.getLogger(ClassName.class)`.
  - This avoids build issues caused by Lombok's annotation processor interfering on recent JDKs.
* **Exception handling**:
  - Never silently swallow exceptions (`catch (...) {}`) — log them appropriately (`log.warn(...)` / `log.error(...)`) or throw a clear domain exception.
* **Thread separation (IntelliJ)**:
  - File I/O and network calls must run on a background thread via `Task.Backgroundable`; only UI updates are delegated to the EDT via `ApplicationManager.getApplication().invokeLater(...)`.
* **Code formatter & static analysis (Spotless & SpotBugs)**:
  - Spotless format verification (`spotlessCheck`) and SpotBugs bytecode static analysis (`spotbugsMain`) are wired into `./gradlew check`.
  - After writing code, run `./gradlew spotlessApply` to auto-format it.

---

## 2. Unit Testing & Coverage Rules

* **Test framework**: JUnit 5 (`org.junit.jupiter.api.*`).
* **`@DisplayName` is required**: Every test method must have a `@DisplayName` that clearly states what it verifies.
* **Prefer Dynamic Proxy over ByteBuddy/CGLIB**:
  - Bytecode-manipulation-based mocking libraries can break on recent JDKs (Java 21~25+), so prefer the standard Java `Proxy.newProxyInstance(...)` or an in-memory mock implementation wherever possible.
* **100% line coverage is required for the core module**:
  - Every class and method newly added to `mybatis-sql-tuner-core` must reach 100% branch/line coverage; the `./gradlew check` build fails if coverage falls short.

---

## 3. Git Commit Message Convention (Conventional Commits)

Commit messages must follow this format:

```
<type>(<scope>): <subject>

<body> (optional)
```

### Type list
| Type | Description | Example |
|---|---|---|
| `feat` | Add a new feature | `feat(core): add PostgreSQL EXPLAIN support` |
| `fix` | Fix a bug | `fix(intellij): fix missing SSE streaming chunk` |
| `docs` | Add or update documentation | `docs: add architecture diagram and conventions guide` |
| `refactor` | Restructure code with no behavior change | `refactor(core): remove Lombok, standardize on SLF4J logger` |
| `test` | Add or strengthen test code | `test(core): add 100% line coverage tests for XmlParser dynamic tags` |
| `chore` | Build script, dependency, or package config changes | `chore: add JaCoCo 100% coverage verification rule` |
| `perf` | Performance optimization | `perf: cache recursive mapper-file directory walk` |

### Why this matters: commits drive the release automation

This repository uses [release-please](https://github.com/googleapis/release-please) (`.github/workflows/release-please.yml`)
to read commit messages on `main` and automatically open a release PR that bumps the version and
updates `CHANGELOG.md`. The commit `type` is not just documentation — it directly determines the
version bump:

| Commit pattern | Version bump | Shows up in `CHANGELOG.md` |
|---|---|---|
| `fix:` / `perf:` | patch (`0.1.x`) | Yes |
| `feat:` | minor (`0.x.0`) | Yes |
| `<type>!:` or a `BREAKING CHANGE:` footer | major (`x.0.0`) | Yes |
| `docs:`, `refactor:`, `test:`, `chore:`, `style:`, `ci:`, `build:` | none | No (silently omitted) |

Breaking-change example (note the `!` right after the scope, and the footer explaining the break):
```
feat(core)!: drop support for MyBatis 2.x mapper XML

BREAKING CHANGE: MyBatis 2.x XML mappers are no longer parsed; migrate to MyBatis 3.x syntax.
```

Practical implications:
* If a change should be visible in the changelog and trigger a release, it **must** use `fix`,
  `feat`, or a breaking-change marker — wrapping a real bug fix in `chore:` means release-please
  won't cut a release for it.
* Conversely, don't mark a non-functional change (docs, formatting, test-only) as `fix`/`feat` —
  it will bump the version for no user-facing reason.
* `release-please-config.json` lists `mybatis-sql-tuner-intellij/build.gradle` under `extra-files`,
  so whenever a release PR bumps the version, that file's version string is updated automatically
  in the same PR — no manual edit needed.

---

## 4. Branch Strategy & PR Rules

* **`main`**: The protected branch that always stays in a deployable, stable state.
* **Feature branches**: `feature/<feature-name>`
* **Bugfix branches**: `fix/<bug-name>`
* **Docs branches**: `docs/<doc-name>`
* Every PR must pass the CI pipeline's (`check`) tests and coverage verification before it can be merged.
