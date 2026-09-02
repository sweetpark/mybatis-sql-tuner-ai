# 🤝 Contributing to MyBatis SQL Tuner AI

`MyBatis SQL Tuner AI` 오픈소스 프로젝트에 관심을 가져주셔서 감사합니다!  
버그 제보, 문서 개선, 신규 기능 제안 등 모든 형태의 기여를 환영합니다.

---

## 📋 목차
1. [행동 강령 (Code of Conduct)](#1-행동-강령-code-of-conduct)
2. [시작하기 (Getting Started)](#2-시작하기-getting-started)
3. [개발 및 테스트 워크플로우](#3-개발-및-테스트-워크플로우)
4. [품질 및 커버리지 규칙 (100% Line Coverage Enforcement)](#4-품질-및-커버리지-규칙-100-line-coverage-enforcement)
5. [Pull Request 가이드](#5-pull-request-가이드)
6. [컨벤션 참고 문서](#6-컨벤션-참고-문서)

---

## 1. 행동 강령 (Code of Conduct)
모든 기여자는 서로를 존중하며 건강한 오픈소스 생태계를 유지하기 위해 [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)를 준수해야 합니다.

---

## 2. 시작하기 (Getting Started)

### 요구 사양
* **JDK**: Java 17 이상 (Java 17 ~ 25+ 호환)
* **IDE**: IntelliJ IDEA (Community 또는 Ultimate)
* **Git**

### 저장소 포크 및 복제
1. GitHub에서 본인 계정으로 저장소를 **Fork**합니다.
2. 로컬 머신에 복제합니다:
   ```bash
   git clone https://github.com/<your-username>/mybatis-sql-tuner-ai.git
   cd mybatis-sql-tuner-ai
   ```
3. 작업용 브랜치를 생성합니다:
   ```bash
   git checkout -b feature/awesome-feature
   # 또는
   git checkout -b fix/bug-description
   ```

---

## 3. 개발 및 테스트 워크플로우

### 1) 빌드 및 테스트 실행
```bash
# 전체 단위 테스트 및 커버리지 검증
./gradlew check
```

### 2) 샌드박스 IDE 실행 (플러그인 로컬 동작 검증)
```bash
# 플러그인이 적용된 IntelliJ IDEA 샌드박스 실행
./gradlew :mybatis-sql-analyzer-intellij:runIde
```

### 3) 배포용 플러그인 Zip 생성
```bash
./gradlew :mybatis-sql-analyzer-intellij:buildPlugin
```

---

## 4. 품질 및 커버리지 규칙 (100% Line Coverage Enforcement)

본 프로젝트의 핵심 파서/분석기 모듈인 `mybatis-sql-analyzer-core`는 **라인 커버리지 100%** 유지를 빌드 게이트(`jacocoTestCoverageVerification`)로 강제하고 있습니다.

* 신규 로직 추가 또는 버그 수정 시 반드시 해당 분기를 검증하는 단위 테스트를 함께 작성해야 합니다.
* Core 모듈 커버리지 검증:
  ```bash
  ./gradlew :mybatis-sql-analyzer-core:check
  ```

---

## 5. Pull Request 가이드

1. **커밋 메시지 규칙**: [Conventional Commits](docs/CONVENTIONS.md) 형식을 준수합니다 (`feat:`, `fix:`, `refactor:`, `docs:`, `test:`, `chore:`).
2. **테스트 확인**: PR 제출 전 `./gradlew check`를 실행하여 모든 테스트와 커버리지 검증을 통과하는지 확인합니다.
3. **PR 본문 작성**: 변경 이유, 수정 내용, 테스트 방법을 명확히 작성합니다.

---

## 6. 컨벤션 참고 문서
* 🏗 **[아키텍처 가이드 (docs/ARCHITECTURE.md)](docs/ARCHITECTURE.md)**: 모듈 구조, AST 파싱 및 AI 스트리밍 파이프라인
* 📐 **[코딩 & 커밋 컨벤션 (docs/CONVENTIONS.md)](docs/CONVENTIONS.md)**: Java 코드 스타일, 단위 테스트 작성 원칙, 커밋 메시지 규칙
* 🌐 **[Marketplace 배포 가이드 (docs/MARKETPLACE_PUBLISHING.md)](docs/MARKETPLACE_PUBLISHING.md)**: 공식 마켓플레이스 배포 프로세스
