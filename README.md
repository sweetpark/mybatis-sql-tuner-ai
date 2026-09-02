# 🚀 mybatis-sql-tuner-ai

<p align="center">
  <img src="https://img.shields.io/badge/License-Apache_2.0-blue.svg" alt="License" />
  <img src="https://img.shields.io/badge/Java-17%2B-orange.svg" alt="Java 17+" />
  <img src="https://img.shields.io/badge/IntelliJ%20Platform-2024.1%2B-blueviolet.svg" alt="IntelliJ Platform" />
  <img src="https://img.shields.io/badge/Code%20Style-Google%20Java%20Format-brightgreen.svg" alt="Spotless" />
  <img src="https://img.shields.io/badge/Static%20Analysis-SpotBugs-yellow.svg" alt="SpotBugs" />
  <img src="https://img.shields.io/badge/Coverage-100%25%20(Core)-success.svg" alt="Coverage" />
  <img src="https://img.shields.io/badge/AI%20Review-CodeRabbit-purple.svg" alt="CodeRabbit" />
</p>

> **AI-Assisted MyBatis SQL Tuning & Static Analysis Tool for IntelliJ IDEA**  
> MyBatis XML 동적 매퍼 쿼리를 실행 가능한 SQL(`fakeSql`)로 자동 변환하고, DB 실행계획(`EXPLAIN`) 및 메타데이터를 수집하여 **10년 차 수석 DBA 페르소나 기반 AI 튜닝 리포트를 실시간 스트리밍**으로 제공하는 개발자 생산성 도구입니다.

---

## 📌 1. 주요 기능 (Key Features)

* 🪄 **MyBatis XML 동적 태그 자동 제거 & fakeSql 변환**: `<where>`, `<if>`, `<choose>`, `<foreach>`, `<trim>`, `<set>`, `<bind>`, `<include>` 태그를 분석하여 DB에서 즉시 `EXPLAIN` 가능한 가상 SQL을 자동 생성합니다.
* 📊 **실시간 DB EXPLAIN & 메타데이터 수집**: 대상 테이블의 DDL, 컬럼 데이터 타입/사이즈, 인덱스 정보 및 트랜잭션 격리수준(`TRANSACTION ISOLATION`)을 원클릭으로 추출합니다.
* 🤖 **실시간 AI 스트리밍 분석 (OpenAI / Ollama 호환)**: 수석 DBA 페르소나 최적화 프롬프트를 조합하고, 로컬 LLM(Ollama) 또는 클라우드 LLM(OpenAI 등)과 연결하여 토큰 단위로 실시간 튜닝 리포트를 스트리밍합니다.
* 🔒 **프로젝트 단위 독립 설정**: DB 및 AI 연결 설정이 IntelliJ 프로젝트 단위(`PropertiesComponent`)로 안전하게 저장되어 설정 파일을 별도 관리할 필요가 없습니다.

---

## 🌐 2. 호환성 및 지원 환경 (Compatibility)

### 🗄 지원 데이터베이스 (Supported Databases)
* **MySQL** (5.7 / 8.0+)
* **MariaDB** (10.x / 11.x+)
* **PostgreSQL** (12+)
* **H2 Database** (In-Memory / Test)

### 🧠 지원 AI 공급자 (Supported AI Providers)
* **로컬 LLM (Local LLM)**: [Ollama](https://ollama.com/) (`qwen2.5-coder`, `llama3`, `deepseek-coder` 등)
* **클라우드 LLM (Cloud LLM)**: OpenAI (`gpt-4o`, `gpt-4o-mini`), OpenRouter, vLLM 등 **OpenAI 호환 규격(`POST /v1/chat/completions`) 전 모델** 지원

---

## 🛡 3. 코드 품질 & 자동화 파이프라인 (Quality Gates)

본 프로젝트는 최상의 코드 품질과 신뢰성을 유지하기 위해 엄격한 자동화 파이프라인을 운영합니다.

| 도구 | 역할 | 검증 방식 |
| :--- | :--- | :--- |
| **Spotless** | Google Java Format 코드 스타일 통일 | `./gradlew spotlessCheck` |
| **SpotBugs** | Java 바이트코드 레벨 잠재적 버그/안티패턴 정적 분석 | `./gradlew spotbugsMain` |
| **JaCoCo** | Core 파서 모듈 **100% 라인 커버리지** 강제화 | `./gradlew jacocoTestCoverageVerification` |
| **CodeRabbit AI** | PR 등록 시 변경 diff 자동 AI 코드 리뷰 | GitHub PR Webhook 자동 연동 |
| **GitHub Actions** | Push/PR 시 자동 빌드, 테스트, Step Summary 리포트 발행 | `.github/workflows/ci.yml` |

---

## 💻 4. 실행 및 개발 방법 (How to Run & Develop)

플러그인이 설치된 IntelliJ IDEA 샌드박스를 로컬에서 실행하여 기능 동작을 바로 테스트할 수 있습니다.

```bash
# 샌드박스 IntelliJ IDEA 실행
./gradlew :mybatis-sql-analyzer-intellij:runIde
```
> 실행된 샌드박스 IDE에서 MyBatis XML 파일을 열고 우클릭 메뉴(`Analyze SQL with AI`) 또는 우측 툴윈도우(`MyBatis SQL Analyzer`)를 통해 즉시 기능을 테스트할 수 있습니다.

---

## 🔨 5. 빌드 및 테스트 방법 (Build & Test)

### 1) 단위 테스트, Spotless, SpotBugs, 100% 커버리지 전체 검증
```bash
# 전체 단위 테스트 및 모든 품질 게이트 검증
./gradlew check
```

### 2) 코드 포맷 자동 정렬 (Spotless Apply)
```bash
# Google Java Format 스타일로 자동 포맷팅
./gradlew spotlessApply
```

### 3) 배포용 플러그인 Zip 파일 빌드
```bash
# 배포용 플러그인 zip 생성
./gradlew :mybatis-sql-analyzer-intellij:buildPlugin
```
* **생성된 산출물**: `mybatis-sql-analyzer-intellij/build/distributions/mybatis-sql-analyzer-intellij-0.1.0.zip`

---

## 📦 6. 플러그인 적용 및 설치 방법 (Installation)

### 방법 A: JetBrains Marketplace에서 설치 (공식 마켓 오픈 후)
1. IntelliJ IDEA 실행 후 `Settings` (`Ctrl+Alt+S` 또는 `Cmd+,`) → **Plugins** 이동
2. **Marketplace** 탭에서 `MyBatis SQL Analyzer` 검색
3. **Install** 버튼 클릭 후 IDE 재시작

### 방법 B: Zip 파일로 수동 설치 (로컬 빌드본 적용)
1. IntelliJ IDEA 실행 후 `Settings` → **Plugins** 이동
2. 상단 톱니바퀴(⚙️) 아이콘 클릭 → **Install Plugin from Disk...** 선택
3. 빌드된 `mybatis-sql-analyzer-intellij-0.1.0.zip` 파일 선택
4. IDE 재시작 후 우측 사이드바의 **MyBatis SQL Analyzer** 툴윈도우 확인

---

## 📚 7. 개발 및 기여 문서 (Documentation)

* 🏗 **[시스템 아키텍처 가이드 (docs/ARCHITECTURE.md)](docs/ARCHITECTURE.md)**: XML 파싱, AST 제거, fakeSql 생성, SSE 스트리밍 흐름
* 📐 **[코딩 & 커밋 컨벤션 (docs/CONVENTIONS.md)](docs/CONVENTIONS.md)**: Java 코드 스타일, 단위 테스트 작성 원칙, Conventional Commits 규칙
* 🤝 **[기여 가이드 (CONTRIBUTING.md)](CONTRIBUTING.md)**: 개발 환경 설정, 브랜치 전략, PR 제출 가이드
* 📘 **[오픈소스 이관 플레이북 (docs/OPEN_SOURCE_MIGRATION_PLAYBOOK.md)](docs/OPEN_SOURCE_MIGRATION_PLAYBOOK.md)**: sweetpark 조직 프로젝트 공통 이관 & 품질 게이트 구축 절차서
* 🌐 **[Marketplace 배포 가이드 (docs/MARKETPLACE_PUBLISHING.md)](docs/MARKETPLACE_PUBLISHING.md)**: JetBrains Marketplace 등록, 토큰 발급, 플러그인 서명 및 자동 배포
* 📜 **[행동 강령 (CODE_OF_CONDUCT.md)](CODE_OF_CONDUCT.md)**: Contributor Covenant 2.1
* ⚖️ **[오픈소스 라이선스 (LICENSE)](LICENSE)**: Apache License 2.0

---

## 🗺 8. 단계별 로드맵 (Roadmap)

```mermaid
graph LR
    P1["Phase 1<br/>레포 초기화 & 설계"] --> P2["Phase 2<br/>소스 이관 & 리팩토링"]
    P2 --> P3["Phase 3<br/>검증 & 플러그인 빌드"]
    P3 --> P4["Phase 4<br/>Marketplace 오픈소스 배포"]
    style P1 fill:#238636,stroke:#fff,stroke-width:2px,color:#fff
    style P2 fill:#238636,stroke:#fff,stroke-width:2px,color:#fff
    style P3 fill:#238636,stroke:#fff,stroke-width:2px,color:#fff
    style P4 fill:#1f6feb,stroke:#fff,stroke-width:2px,color:#fff
```

### 📌 Phase 1: Private 레포 생성 및 청사진 수립 (✅ 완료)
- [x] 오픈소스 지향 저장소(`mybatis-sql-tuner-ai`) 생성 (Private)
- [x] 이관 가이드, 리팩토링 원칙, 아키텍처 비전이 담긴 README 작성

### 📌 Phase 2: 소스코드 이관 및 클렌징 (✅ 완료)
- [x] 원본 저장소에서 `core` 및 `intellij` 모듈 코드 이관
- [x] 패키지명 일원화 (`io.github.sweetpark.sqlanalyzer`)
- [x] 사내 IP/설정값 클렌징 및 범용 환경 설정(Properties/Settings) 적용
- [x] Gradle 빌드 스크립트 최적화 (IntelliJ Platform Gradle Plugin 2.x 설정)

### 📌 Phase 3: 테스트 및 플러그인 빌드 검증 (✅ 완료)
- [x] Core 모듈 라인 커버리지 100% 달성 및 JaCoCo 강제 룰 적용 (`./gradlew check`)
- [x] IntelliJ 플러그인 전수 단위 테스트 통과
- [x] Spotless (Google Java Format) 및 SpotBugs 정적 분석 통합
- [x] 배포용 Zip 패키징 검증 (`./gradlew buildPlugin`)
- [x] 플러그인 샌드박스 실행 환경 구성 (`./gradlew runIde`)

### 📌 Phase 4: JetBrains Marketplace 공개 배포 준비 (진행 중)
- [x] JetBrains Marketplace 배포용 Gradle 블록(`publishing`, `signing`, `pluginConfiguration`) 구성
- [x] 오픈소스 라이선스 추가 (`LICENSE` - Apache 2.0)
- [x] 오픈소스 문서 표준화 (`CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, `docs/`)
- [x] CodeRabbit AI 자동 리뷰 연동 및 GitHub Actions CI 워크플로우 구성
- [ ] JetBrains Marketplace 개발자 계정 토큰 발급 및 서명 키 등록
- [ ] **JetBrains Marketplace 공식 배포 및 저장소 Public 전환**
