# 🚀 mybatis-sql-tuner-ai

> **AI-Assisted MyBatis SQL Tuning & Static Analysis Tool for IntelliJ IDEA**  
> MyBatis XML 동적 매퍼 쿼리를 실행 가능한 SQL로 자동 변환하고, DB 실행계획(EXPLAIN) 및 메타데이터를 수집하여 AI 기반 튜닝 리포트를 실시간 스트리밍으로 제공하는 개발자 생산성 도구입니다.

---

## 📌 1. 프로젝트 개요 & 오픈소스 비전 (Open Source Vision)

### 💡 왜 이 도구가 필요한가? (Problem Statement)
* **MyBatis의 고질적 한계**: `<if>`, `<foreach>`, `<choose>` 등 동적 XML 태그로 인해 복잡한 쿼리를 실제 DB에서 `EXPLAIN`하거나 인덱스 튜닝을 하려면 개발자가 직접 가상 파라미터를 채워 넣어야 하는 번거로움이 있습니다.
* **개발 생산성 저하**: 쿼리 튜닝 시 DB 스키마 확인, 실행 계획 조회, 프롬프트 작성, AI 질의를 수동으로 반복해야 합니다.
* **보안/컨벤션 위반**: `${}` 직접 치환으로 인한 SQL Injection 취약점이나 필드 주입 등의 안티패턴을 IDE 레벨에서 사전에 차단하기 어렵습니다.

### 🎯 오픈소스 비전 & 제공 형태
외부 개발자들이 손쉽게 도입하고 기여할 수 있도록 다음과 같이 독립 배포 가능한 구조로 제공합니다.
1. **IntelliJ 플러그인 (`mybatis-sql-analyzer-intellij`)**: JetBrains Marketplace 또는 플러그인 zip 설치로 IDE 에디터에서 우클릭/툴윈도우로 바로 사용.
2. **Core 라이브러리 (`mybatis-sql-analyzer-core`)**: CLI나 CI(Gradle/Maven) 파이프라인에서 정적 분석 및 SQL 파서로 재사용 가능.
3. **AI Provider 유연성**: Ollama(로컬 LLM), OpenAI, Anthropic, DeepSeek 등 **OpenAI 호환 API 규격**을 지원하여 누구나 원하는 LLM 모델을 연결해 사용 가능.

---

## 🔍 2. 원본 소스 및 이관 대상 (Source Reference)

| 구분 | 내용 |
| :--- | :--- |
| **원본 저장소** | `wiezonSRC/MYBATIS_SQL_TUNER_AI` (Branch: `main`) |
| **핵심 모듈 1** | `mybatis-sql-analyzer-core/` (XML 파서, AST, JDBC 분석기, 프롬프트 생성기) |
| **핵심 모듈 2** | `mybatis-sql-analyzer-intellij/` (ToolWindow UI, Settings, SSE 스트리밍 클라이언트) |
| **참고 문서** | `docs/guide/`, `docs/superpowers/`, `docs/analysis/` |

---

## 🛠 3. 이관 및 리팩토링 기준 (Refactoring & Sanitization Rules)

새로운 세션에서 소스코드를 옮겨올 때 **반드시 준수해야 하는 기준**입니다.

### ① 사내 기밀 / 내부 정보 완전 배제 (Sanitization)
* **내부 IP 및 엔드포인트 제거**:
  * 문서 및 코드 내 `http://13.124.xxx.xxx:11434` 같은 사내 고정 IP 전면 제거.
  * 기본 AI 엔드포인트는 `http://localhost:11434/v1` (Ollama 기본 규격)로 설정하고, 사용자가 UI에서 URL/API Key/Model을 자유롭게 변경할 수 있도록 유지.
* **사내 전용 모델명 및 계정 정보 제거**:
  * 특정 사내 전용 모델명을 기본값으로 강제하지 않고, `qwen2.5-coder:7b`, `gpt-4o-mini` 등의 범용 모델명을 예시로 제시.

### ② 패키지 및 프로젝트 네이밍 표준화
* 기존 패키지명(`com.example.sqlanalyzer` 등)을 공식 오픈소스 네이밍으로 일원화:
  * **Target Base Package**: `io.github.sweetpark.sqlanalyzer`
  * Core: `io.github.sweetpark.sqlanalyzer.core`
  * Plugin: `io.github.sweetpark.sqlanalyzer.intellij`

### ③ 개발자 경험(DX) 강화 및 범용성 확보
* **Mock / H2 테스트 슈트 제공**: 실제 DB 서버가 없어도 단위 테스트 및 플러그인 로직 검증이 가능하도록 샘플 MyBatis XML 및 In-Memory DB 테스트 케이스 포함.
* **OpenAI SSE 호환 표준 준수**: `POST /v1/chat/completions` 규격을 엄격히 준수하여 Ollama, vLLM, OpenAI, OpenRouter 등 다양한 백엔드와 완벽 호환.

---

## 🗺 4. 단계별 로드맵 (Roadmap to Public Release)

```mermaid
graph LR
    P1["Phase 1<br/>레포 초기화 & 설계"] --> P2["Phase 2<br/>소스 이관 & 리팩토링"]
    P2 --> P3["Phase 3<br/>검증 & 플러그인 빌드"]
    P3 --> P4["Phase 4<br/>Public 오픈소스 전환"]
    style P1 fill:#238636,stroke:#fff,stroke-width:2px,color:#fff
    style P2 fill:#1f6feb,stroke:#fff,stroke-width:2px,color:#fff
    style P3 fill:#8957e5,stroke:#fff,stroke-width:2px,color:#fff
    style P4 fill:#d29922,stroke:#fff,stroke-width:2px,color:#fff
```

### 📌 Phase 1: Private 레포 생성 및 청사진 수립 (✅ 현재 단계)
- [x] 오픈소스 지향 저장소(`mybatis-sql-tuner-ai`) 생성 (Private)
- [x] 이관 가이드, 리팩토링 원칙, 아키텍처 비전이 담긴 README 작성

### 📌 Phase 2: 소스코드 이관 및 클렌징 (Next Session)
- [ ] `wiezonSRC/MYBATIS_SQL_TUNER_AI`에서 `core` 및 `intellij` 모듈 코드 이관
- [ ] 패키지명 변경 (`io.github.sweetpark.sqlanalyzer`)
- [ ] 사내 IP/설정값 클렌징 및 범용 환경 설정(Properties/Settings) 적용
- [ ] Gradle 빌드 스크립트 최적화 (IntelliJ Platform Gradle Plugin 2.x 설정)

### 📌 Phase 3: 테스트 및 플러그인 빌드 검증
- [ ] Core 모듈 단위 테스트 통과 (`./gradlew test`)
- [ ] 플러그인 샌드박스 실행 검증 (`./gradlew runIde`)
- [ ] 배포용 Zip 패키징 검증 (`./gradlew buildPlugin`)
- [ ] Ollama 및 외부 AI API와의 실시간 SSE 스트리밍 통신 검증

### 📌 Phase 4: Public 오픈소스 전환 준비
- [ ] 오픈소스 라이선스 확정 (Apache License 2.0 또는 MIT)
- [ ] `LICENSE`, `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md` 추가
- [ ] GitHub Actions CI 파이프라인 구축 (PR 빌드 검증, `verifyPlugin`, Release 자동화)
- [ ] README 최종 갱신 (데모 GIF, 기능 소개, 아키텍처 다이어그램, 설치/기여 가이드)
- [ ] **저장소 Public 전환 (공개 오픈소스화)**

---

## 📋 다음 세션 작업자를 위한 체크리스트 (Action Items for Next Session)
1. `wiezonSRC/MYBATIS_SQL_TUNER_AI` 저장소 클론 및 소스 파일 복사
2. `mybatis-sql-analyzer-core` 및 `mybatis-sql-analyzer-intellij`의 패키지 경로를 `io.github.sweetpark.sqlanalyzer`로 리팩토링
3. IP 주소, 사내 전용 설정값 하드코딩 여부 grep 검색 및 제거
4. `./gradlew buildPlugin` 실행 후 정상 빌드 확인
