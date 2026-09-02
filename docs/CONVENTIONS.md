# 📐 코딩 및 커밋 컨벤션 가이드 (Conventions)

본 문서는 `mybatis-sql-tuner-ai` 프로젝트의 코드 품질, 테스트 표준 및 협업 규칙을 정의합니다.

---

## 1. Java 코딩 표준

* **언어 버전**: Java 17을 최소 타깃으로 하며, Java 17 ~ 25+ 전 버전에서 경고/에러 없이 빌드되어야 합니다.
* **로깅 (Logging)**:
  - Lombok(`@Slf4j`)을 사용하지 않고 `org.slf4j.LoggerFactory.getLogger(ClassName.class)`를 명시적으로 선언합니다.
  - 최신 JDK에서 롬복 어노테이션 프로세서 간섭으로 인한 빌드 이슈를 원천 차단하기 위함입니다.
* **예외 처리**:
  - 예외를 무시(`catch (...) {}`)하지 않고 적절한 로그(`log.warn(...)` / `log.error(...)`)를 남기거나 명확한 도메인 예외를 던집니다.
* **스레드 분리 (IntelliJ)**:
  - 파일 I/O 및 네트워크 통신은 반드시 `Task.Backgroundable`을 통해 백그라운드 스레드에서 실행하고, UI 갱신만 `ApplicationManager.getApplication().invokeLater(...)`로 EDT에 위임합니다.
* **코드 포맷터 & 정적 분석 (Spotless & SpotBugs)**:
  - Spotless 포맷팅 검증(`spotlessCheck`) 및 SpotBugs 바이트코드 정적 분석(`spotbugsMain`)이 `./gradlew check`에 연동되어 있습니다.
  - 코드 작성 후 `./gradlew spotlessApply`를 실행하여 포맷을 자동 정렬합니다.

---

## 2. 단위 테스트 및 커버리지 규칙

* **테스트 프레임워크**: JUnit 5 (`org.junit.jupiter.api.*`)를 사용합니다.
* **`@DisplayName` 필수**: 모든 테스트 메서드는 검증 목적을 명확히 설명하는 `@DisplayName`을 부여합니다.
* **ByteBuddy/CGLIB 대신 Dynamic Proxy 지향**:
  - 최신 JDK(Java 21~25+)에서 바이트코드 조작 기반 Mock 라이브러리가 실패할 수 있으므로, 가능한 Java 표준 `Proxy.newProxyInstance(...)` 또는 In-Memory Mock 구현체를 우선 사용합니다.
* **Core 모듈 라인 커버리지 100% 필수**:
  - `mybatis-sql-analyzer-core`에 새로 추가되는 모든 클래스와 메서드는 분기/라인 커버리지 100%를 달성해야 하며, 커버리지가 미달할 경우 `./gradlew check` 빌드가 실패합니다.

---

## 3. Git 커밋 메시지 컨벤션 (Conventional Commits)

커밋 메시지는 다음 규격을 따릅니다:

```
<type>(<scope>): <subject>

<body> (선택 사항)
```

### Type 목록
| Type | 설명 | 예시 |
|---|---|---|
| `feat` | 새로운 기능 추가 | `feat(core): PostgreSQL EXPLAIN 지원 추가` |
| `fix` | 버그 수정 | `fix(intellij): SSE 스트리밍 청크 누락 버그 수정` |
| `docs` | 문서 추가 및 수정 | `docs: 아키텍처 다이어그램 및 컨벤션 가이드 추가` |
| `refactor` | 기능 변경 없는 코드 구조 개선 | `refactor(core): Lombok 제거 및 SLF4J 로거 표준화` |
| `test` | 테스트 코드 추가 및 보강 | `test(core): XmlParser 동적 태그 100% 라인 커버리지 테스트 추가` |
| `chore` | 빌드 스크립트, 의존성, 패키지 설정 수정 | `chore: JaCoCo 100% 커버리지 검증 룰 추가` |
| `perf` | 성능 최적화 | `perf: 매퍼 파일 재귀 탐색 캐싱 적용` |

---

## 4. 브랜치 전략 및 PR 규칙

* **`main`**: 상시 배포 가능한 안정 상태를 유지하는 보호 브랜치입니다.
* **기능 개발 브랜치**: `feature/<feature-name>`
* **버그 수정 브랜치**: `fix/<bug-name>`
* **문서 수정 브랜치**: `docs/<doc-name>`
* 모든 PR은 CI 파이프라인(`check`)의 테스트 및 커버리지 검증을 통과해야 머지할 수 있습니다.
