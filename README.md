# 🚀 mybatis-sql-tuner-ai

<p align="center">
  <img src="https://img.shields.io/badge/License-Apache_2.0-blue.svg" alt="License" />
  <img src="https://img.shields.io/badge/Java-17%2B-orange.svg" alt="Java 17+" />
  <img src="https://img.shields.io/badge/IntelliJ%20Platform-2024.1%2B-blueviolet.svg" alt="IntelliJ Platform" />
  <img src="https://img.shields.io/badge/Code%20Style-Eclipse%20Formatter-brightgreen.svg" alt="Spotless" />
  <img src="https://img.shields.io/badge/Static%20Analysis-SpotBugs-yellow.svg" alt="SpotBugs" />
  <img src="https://img.shields.io/badge/Coverage-100%25%20(Core)-success.svg" alt="Coverage" />
  <img src="https://img.shields.io/badge/AI%20Review-CodeRabbit-purple.svg" alt="CodeRabbit" />
</p>

> **AI-Assisted MyBatis SQL Tuning & Static Analysis Tool for IntelliJ IDEA**
> A developer productivity tool that automatically converts dynamic MyBatis XML mapper queries into
> executable SQL (`fakeSql`), collects the DB execution plan (`EXPLAIN`) and metadata, and streams a
> **senior-DBA-persona AI tuning report in real time**.

---

## 📌 1. Key Features

* 🪄 **Automatic MyBatis dynamic tag stripping & fakeSql conversion**: Parses `<where>`, `<if>`, `<choose>`, `<foreach>`, `<trim>`, `<set>`, `<bind>`, and `<include>` tags and auto-generates a virtual SQL statement that your DB can `EXPLAIN` immediately.
* 📊 **Real-time DB EXPLAIN & metadata collection**: Extracts target-table DDL, column types/sizes, index information, and the transaction isolation level (`TRANSACTION ISOLATION`) with one click.
* 🤖 **Real-time AI streaming analysis (OpenAI / Ollama compatible)**: Composes a senior-DBA-persona optimization prompt and streams a tuning report token by token from a local LLM (Ollama) or a cloud LLM (OpenAI, etc.).
* 🔒 **Per-project independent settings**: DB and AI connection settings are kept per IntelliJ project — non-secret values via `PropertiesComponent`, and the DB password / AI API key via the OS credential store (`PasswordSafe`) — so there's no separate config file to manage.

---

## 🌐 2. Compatibility

### 🗄 Supported Databases
* **MySQL** (5.7 / 8.0+)
* **MariaDB** (10.x / 11.x+)
* **PostgreSQL** (12+)
* **H2 Database** (In-Memory / Test)

### 🧠 Supported AI Providers
* **Local LLM**: [Ollama](https://ollama.com/) (`qwen2.5-coder`, `llama3`, `deepseek-coder`, etc.)
* **Cloud LLM**: OpenAI (`gpt-4o`, `gpt-4o-mini`), OpenRouter, vLLM, and **any model compatible with the OpenAI chat-completions spec (`POST /v1/chat/completions`)**

---

## 🛡 3. Code Quality & Automation Pipeline

This project runs a strict automation pipeline to keep code quality and reliability high.

| Tool | Role | Verification |
| :--- | :--- | :--- |
| **Spotless** | Enforces a unified code style via the Eclipse formatter | `./gradlew spotlessCheck` |
| **SpotBugs** | Static analysis for potential bugs/anti-patterns at the Java bytecode level | `./gradlew spotbugsMain` |
| **JaCoCo** | Enforces **100% line coverage** on the core parser module | `./gradlew jacocoTestCoverageVerification` |
| **CodeRabbit AI** | Automatic AI code review of the diff on every PR | Auto-linked via GitHub PR webhook |
| **GitHub Actions** | Automatic build, test, and Step Summary report on push/PR | `.github/workflows/ci.yml` |

---

## 💻 4. How to Run & Develop

You can run a local IntelliJ IDEA sandbox with the plugin installed to try the feature immediately.

```bash
# Launch the sandbox IntelliJ IDEA instance
./gradlew :mybatis-sql-analyzer-intellij:runIde
```
> In the sandbox IDE, open a MyBatis mapper XML file and try the feature right away via the right-click menu (`Analyze SQL with AI`) or the tool window on the right (`MyBatis SQL Analyzer`).

---

## 🔨 5. Build & Test

### 1) Full verification: unit tests, Spotless, SpotBugs, 100% coverage
```bash
# Run all unit tests and every quality gate
./gradlew check
```

### 2) Auto-format code (Spotless Apply)
```bash
# Auto-format using the Eclipse formatter style
./gradlew spotlessApply
```

### 3) Build the distributable plugin zip
```bash
# Produce the distributable plugin zip
./gradlew :mybatis-sql-analyzer-intellij:buildPlugin
```
* **Output artifact**: `mybatis-sql-analyzer-intellij/build/distributions/mybatis-sql-analyzer-intellij-0.1.1.zip`

---

## 📦 6. Installation

### Option A: Install from the JetBrains Marketplace (once the official listing is live)
1. Open IntelliJ IDEA, go to `Settings` (`Ctrl+Alt+S` or `Cmd+,`) → **Plugins**
2. Search for `MyBatis SQL Analyzer` in the **Marketplace** tab
3. Click **Install**, then restart the IDE

### Option B: Manual install from a zip file (local build)
1. Open IntelliJ IDEA, go to `Settings` → **Plugins**
2. Click the gear icon (⚙️) at the top → **Install Plugin from Disk...**
3. Select the built `mybatis-sql-analyzer-intellij-0.1.1.zip` file
4. Restart the IDE and confirm the **MyBatis SQL Analyzer** tool window on the right sidebar

---

## 📚 7. Development & Contribution Docs

* 🏗 **[Architecture Guide (docs/ARCHITECTURE.md)](docs/ARCHITECTURE.md)**: XML parsing, AST stripping, fakeSql generation, SSE streaming flow
* 📐 **[Coding & Commit Conventions (docs/CONVENTIONS.md)](docs/CONVENTIONS.md)**: Java code style, unit-testing principles, Conventional Commits rules
* 🤝 **[Contributing Guide (CONTRIBUTING.md)](CONTRIBUTING.md)**: Dev environment setup, branch strategy, PR submission guide
* 📘 **[Open Source Migration Playbook (docs/OPEN_SOURCE_MIGRATION_PLAYBOOK.md)](docs/OPEN_SOURCE_MIGRATION_PLAYBOOK.md)**: Shared migration & quality-gate setup procedure for sweetpark org projects
* 🌐 **[Marketplace Publishing Guide (docs/MARKETPLACE_PUBLISHING.md)](docs/MARKETPLACE_PUBLISHING.md)**: JetBrains Marketplace registration, token issuance, plugin signing, and automated publishing
* 📜 **[Code of Conduct (CODE_OF_CONDUCT.md)](CODE_OF_CONDUCT.md)**: Contributor Covenant 2.1
* ⚖️ **[License (LICENSE)](LICENSE)**: Apache License 2.0

---

## 🗺 8. Roadmap

```mermaid
graph LR
    P1["Phase 1<br/>Repo init & design"] --> P2["Phase 2<br/>Source migration & refactor"]
    P2 --> P3["Phase 3<br/>Verification & plugin build"]
    P3 --> P4["Phase 4<br/>Open-source Marketplace release"]
    style P1 fill:#238636,stroke:#fff,stroke-width:2px,color:#fff
    style P2 fill:#238636,stroke:#fff,stroke-width:2px,color:#fff
    style P3 fill:#238636,stroke:#fff,stroke-width:2px,color:#fff
    style P4 fill:#1f6feb,stroke:#fff,stroke-width:2px,color:#fff
```

### 📌 Phase 1: Create private repo & establish the blueprint (✅ Done)
- [x] Create an open-source-oriented repository (`mybatis-sql-tuner-ai`) (Private)
- [x] Write a README covering the migration guide, refactoring principles, and architecture vision

### 📌 Phase 2: Migrate & sanitize the source code (✅ Done)
- [x] Migrate `core` and `intellij` module code from the original repository
- [x] Unify the package name (`io.github.sweetpark.sqlanalyzer`)
- [x] Sanitize internal IPs/config values and switch to generic environment configuration (Properties/Settings)
- [x] Optimize Gradle build scripts (IntelliJ Platform Gradle Plugin 2.x setup)

### 📌 Phase 3: Verify tests & plugin build (✅ Done)
- [x] Reach 100% line coverage on the core module and enforce it via JaCoCo (`./gradlew check`)
- [x] Pass the full IntelliJ plugin unit-test suite
- [x] Integrate Spotless (Eclipse Formatter) and SpotBugs static analysis
- [x] Verify the distributable zip packaging (`./gradlew buildPlugin`)
- [x] Set up the plugin sandbox run environment (`./gradlew runIde`)

### 📌 Phase 4: Prepare for JetBrains Marketplace release (In progress)
- [x] Configure the Gradle blocks for JetBrains Marketplace publishing (`publishing`, `signing`, `pluginConfiguration`)
- [x] Add an open-source license (`LICENSE` - Apache 2.0)
- [x] Standardize open-source docs (`CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, `docs/`)
- [x] Wire up CodeRabbit AI auto-review and the GitHub Actions CI workflow
- [ ] Issue a JetBrains Marketplace developer account token and register the signing key
- [ ] **Officially release on the JetBrains Marketplace and switch the repository to Public**
