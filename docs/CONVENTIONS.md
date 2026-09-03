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
  - Every class and method newly added to `mybatis-sql-analyzer-core` must reach 100% branch/line coverage; the `./gradlew check` build fails if coverage falls short.

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

---

## 4. Branch Strategy & PR Rules

* **`main`**: The protected branch that always stays in a deployable, stable state.
* **Feature branches**: `feature/<feature-name>`
* **Bugfix branches**: `fix/<bug-name>`
* **Docs branches**: `docs/<doc-name>`
* Every PR must pass the CI pipeline's (`check`) tests and coverage verification before it can be merged.
