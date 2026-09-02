# 🚀 mybatis-sql-tuner-ai

> **AI-Assisted MyBatis SQL Tuning & Static Analysis Tool for IntelliJ IDEA**  
> MyBatis XML 동적 매퍼 쿼리를 실행 가능한 SQL로 자동 변환하고, DB 실행계획(EXPLAIN) 및 메타데이터를 수집하여 AI 기반 튜닝 리포트를 실시간 스트리밍으로 제공하는 개발자 생산성 도구입니다.

---

## 📌 1. 주요 기능 (Features)

* 🪄 **MyBatis XML 동적 태그 자동 제거 & fakeSql 변환**: `<where>`, `<if>`, `<choose>`, `<foreach>`, `<trim>`, `<set>`, `<bind>`, `<include>` 태그를 분석하여 DB에서 즉시 `EXPLAIN` 가능한 가상 SQL을 자동 생성합니다.
* 📊 **실시간 DB EXPLAIN & 메타데이터 수집**: 대상 테이블의 DDL, 컬럼 데이터 타입/사이즈, 인덱스 정보 및 트랜잭션 격리수준을 원클릭으로 추출합니다.
* 🤖 **실시간 AI 스트리밍 분석 (OpenAI / Ollama 호환)**: 10년 차 수석 DBA 페르소나 기반 최적화 프롬프트를 생성하고, 로컬 LLM(Ollama) 또는 클라우드 LLM(OpenAI 등)과 연결하여 토큰 단위로 실시간 튜닝 리포트를 스트리밍합니다.
* 🔒 **프로젝트 단위 독립 설정**: DB 및 AI 연결 설정이 IntelliJ 프로젝트 단위로 안전하게 저장되어 설정 파일을 별도 관리할 필요가 없습니다.

---

## 💻 2. 실행 및 개발 방법 (How to Run & Develop)

플러그인이 설치된 IntelliJ IDEA 샌드박스를 로컬에서 실행하여 기능 동작을 바로 테스트할 수 있습니다.

```bash
# 샌드박스 IntelliJ IDEA 실행
./gradlew :mybatis-sql-analyzer-intellij:runIde
```
> 실행된 샌드박스 IDE에서 MyBatis XML 파일을 열고 우클릭 메뉴(`Analyze SQL with AI`) 또는 우측 툴윈도우(`MyBatis SQL Analyzer`)를 통해 즉시 기능을 테스트할 수 있습니다.

---

## 🔨 3. 빌드 및 테스트 방법 (Build & Test)

### 1) 단위 테스트 및 라인 커버리지 100% 검증
모든 단위 테스트를 실행하고 JaCoCo 100% 라인 커버리지 룰을 검증합니다.
```bash
# 전체 단위 테스트 및 커버리지 검증 (JaCoCo 100% Enforcement)
./gradlew check
```

### 2) 배포용 플러그인 Zip 파일 빌드
```bash
# 배포용 플러그인 zip 생성
./gradlew :mybatis-sql-analyzer-intellij:buildPlugin
```
* **생성된 산출물**: `mybatis-sql-analyzer-intellij/build/distributions/mybatis-sql-analyzer-intellij-0.1.0.zip`

---

## 📦 4. 플러그인 적용 및 설치 방법 (Installation)

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

## 📚 5. 개발 및 기여 문서 (Documentation)

* 🏗 **[시스템 아키텍처 가이드 (docs/ARCHITECTURE.md)](docs/ARCHITECTURE.md)**: XML 파싱, AST 제거, fakeSql 생성, SSE 스트리밍 흐름
* 📐 **[코딩 & 커밋 컨벤션 (docs/CONVENTIONS.md)](docs/CONVENTIONS.md)**: Java 코드 스타일, 단위 테스트 작성 원칙, Conventional Commits 규칙
* 🤝 **[기여 가이드 (CONTRIBUTING.md)](CONTRIBUTING.md)**: 개발 환경 설정, 브랜치 전략, PR 제출 가이드
* 🌐 **[Marketplace 배포 가이드 (docs/MARKETPLACE_PUBLISHING.md)](docs/MARKETPLACE_PUBLISHING.md)**: JetBrains Marketplace 등록, 토큰 발급, 플러그인 서명 및 자동 배포
* 📜 **[행동 강령 (CODE_OF_CONDUCT.md)](CODE_OF_CONDUCT.md)**: Contributor Covenant 2.1
* ⚖️ **[오픈소스 라이선스 (LICENSE)](LICENSE)**: Apache License 2.0

---

## 🗺 6. 단계별 로드맵 (Roadmap)

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
- [x] 배포용 Zip 패키징 검증 (`./gradlew buildPlugin`)
- [x] 플러그인 샌드박스 실행 환경 구성 (`./gradlew runIde`)

### 📌 Phase 4: JetBrains Marketplace 공개 배포 준비 (진행 중)
- [x] JetBrains Marketplace 배포용 Gradle 블록(`publishing`, `signing`, `pluginConfiguration`) 구성
- [x] 오픈소스 라이선스 추가 (`LICENSE` - Apache 2.0)
- [x] 오픈소스 문서 표준화 (`CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, `docs/`)
- [ ] JetBrains Marketplace 개발자 계정 토큰 발급 및 서명 키 등록
- [ ] **JetBrains Marketplace 공식 배포 및 저장소 Public 전환**
