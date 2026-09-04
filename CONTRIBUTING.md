# 🤝 Contributing to MyBatis SQL Tuner AI

Thanks for your interest in the `MyBatis SQL Tuner AI` open-source project!
We welcome all forms of contribution — bug reports, documentation improvements, new feature proposals, and more.

---

## 📋 Table of Contents
1. [Code of Conduct](#1-code-of-conduct)
2. [Getting Started](#2-getting-started)
3. [Development & Test Workflow](#3-development--test-workflow)
4. [Quality & Coverage Rules (100% Line Coverage Enforcement)](#4-quality--coverage-rules-100-line-coverage-enforcement)
5. [Pull Request Guide](#5-pull-request-guide)
6. [Convention References](#6-convention-references)

---

## 1. Code of Conduct
All contributors must follow [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) to keep this a respectful, healthy open-source community.

---

## 2. Getting Started

### Requirements
* **JDK**: Java 17 or later (compatible with Java 17 ~ 25+)
* **IDE**: IntelliJ IDEA (Community or Ultimate)
* **Git**

### Fork & clone the repository
1. **Fork** the repository to your own GitHub account.
2. Clone it locally:
   ```bash
   git clone https://github.com/<your-username>/mybatis-sql-tuner-ai.git
   cd mybatis-sql-tuner-ai
   ```
3. Create a working branch:
   ```bash
   git checkout -b feature/awesome-feature
   # or
   git checkout -b fix/bug-description
   ```

---

## 3. Development & Test Workflow

### 1) Build and run tests
```bash
# Run all unit tests and verify coverage
./gradlew check
```

### 2) Run the sandbox IDE (verify the plugin locally)
```bash
# Launch an IntelliJ IDEA sandbox with the plugin installed
./gradlew :mybatis-sql-tuner-intellij:runIde
```

### 3) Build the distributable plugin zip
```bash
./gradlew :mybatis-sql-tuner-intellij:buildPlugin
```

---

## 4. Quality & Coverage Rules (100% Line Coverage Enforcement)

`mybatis-sql-tuner-core`, the project's core parser/analyzer module, enforces **100% line coverage**
as a build gate (`jacocoTestCoverageVerification`).

* Any new logic or bug fix must be accompanied by unit tests covering the relevant branches.
* Verify core module coverage with:
  ```bash
  ./gradlew :mybatis-sql-tuner-core:check
  ```

---

## 5. Pull Request Guide

1. **Commit message format**: Follow [Conventional Commits](docs/CONVENTIONS.md) (`feat:`, `fix:`, `refactor:`, `docs:`, `test:`, `chore:`).
2. **Verify tests**: Run `./gradlew check` before submitting a PR to confirm all tests and coverage checks pass.
3. **PR description**: Clearly explain the reason for the change, what was modified, and how it was tested.

---

## 6. Convention References
* 🏗 **[Architecture Guide (docs/ARCHITECTURE.md)](docs/ARCHITECTURE.md)**: Module structure, AST parsing, and the AI streaming pipeline
* 📐 **[Coding & Commit Conventions (docs/CONVENTIONS.md)](docs/CONVENTIONS.md)**: Java code style, unit-testing principles, commit message rules
