package io.github.sweetpark.sqlanalyzer.core;

import org.w3c.dom.Node;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.Set;

public class PromptGenerator {

	private PromptGenerator() {
	}

	/**
	 * 쿼리 튜닝 AI 프롬프트를 생성한다.
	 *
	 * <p>
	 * 실행 순서: 1. queryId에 해당하는 DOM Node 추출 2. 원본 동적 쿼리 XML 문자열로 직렬화 3. MyBatis 태그를
	 * 제거한 fakeSql 생성 (EXPLAIN 실행 가능한 형태로 변환) 4. fakeSql로 EXPLAIN 실행 → 실행 계획 텍스트 획득
	 * 5. fakeSql에서 테이블 목록 추출 후 메타데이터(컬럼/인덱스) 조회 6. 수집된 정보를 AI가 바로 사용 가능한 프롬프트 형식으로
	 * 조합
	 *
	 * @param connection
	 *            DB 연결 객체 (호출자가 try-with-resources로 관리해야 함)
	 * @param queryId
	 *            분석할 MyBatis 쿼리 ID (예: "findBadPerformancePayments")
	 * @param mapperPath
	 *            queryId가 포함된 매퍼 XML 파일 경로 (Path 타입으로 통일)
	 * @param mapperPathDir
	 *            전체 매퍼 XML 파일들의 루트 디렉토리 (&lt;sql&gt; 태그 캐싱에 사용)
	 * @return 완성된 AI 프롬프트 (StringBuilder)
	 * @throws Exception
	 *             DB 연결, 파일 파싱, SQL 추출 등 분석 과정의 모든 예외
	 */
	public static StringBuilder generatePrompt(Connection connection, String queryId, Path mapperPath,
			Path mapperPathDir) throws Exception {
		// 1. queryId의 동적 쿼리 Node 추출 (Path 오버로드 메서드 사용)
		Node queryNode = SqlExtractor.getQueryIdDetail(queryId, mapperPath);

		// 2. queryId의 동적쿼리 원본 XML 문자열 추출 (AI 프롬프트에 원본 포함을 위해)
		String originalQuery = SqlExtractor.nodeToString(queryNode);

		// 3. MyBatis 동적 태그를 제거한 fakeSql 생성
		// isForExplain=true: choose 태그에서 첫 번째 when만 선택 → 단일 경로 SQL로 EXPLAIN 실행
		String namespace;
		String fakeSql = null;

		if (queryNode != null) {
			namespace = queryNode.getOwnerDocument().getDocumentElement().getAttribute("namespace");
			fakeSql = SqlExtractor.buildFakeSql(queryNode, true, namespace,
					SqlExtractor.getSqlSnippetRegistry(mapperPathDir));
		}

		String explainInfo = null;
		String metaDataInfo = null;

		if (fakeSql != null) {
			// 4. fakeSql로 EXPLAIN 실행 → 인덱스 활용 여부, Full Scan 여부 등 파악
			explainInfo = JdbcAnalyzer.getExplainInfo(connection, fakeSql);

			// 5. 사용된 테이블의 메타데이터(컬럼 타입, 인덱스 정보) 추출
			Set<String> tables = JdbcAnalyzer.extractTableMethod(fakeSql);
			metaDataInfo = JdbcAnalyzer.getMetaDataInfo(tables, connection.getMetaData()).toString();
		}

		StringBuilder prompt = new StringBuilder();

		// 페르소나 및 환경 설정: AI가 PG 도메인 DBA 전문가로 동작하도록 역할을 명시
		// 역할 부여 없이 쿼리만 제공하면 일반적인 답변만 나오므로 상세한 컨텍스트 제공
		// "너는 X야" 방식은 AI를 역할 고정시키는 반면,
		// "X 수준의 전문가 견해로 분석해줘" 방식은 AI 본연의 추론 능력을 자연스럽게 활용하도록 유도한다.
		prompt.append("실무 10년 차 DBA 수준을 가진 전문가의 견해를 이용해서 분석해줘.\n");
		prompt.append("우리 시스템은 대용량 트랜잭션이 발생하는 환경이며, MyBatis를 이용해 데이터베이스와 통신하고 있어.\n");
		prompt.append("아래 제공된 쿼리와 실행 계획(Explain), 데이터베이스 메타데이터를 종합적으로 분석해서 정밀한 쿼리 튜닝 리포트를 작성해 줘.\n\n");

		// 실제 데이터 컨텍스트 주입 (AI가 분석에 필요한 모든 정보를 한 번에 받을 수 있도록)
		prompt.append("[Original Query] : ").append(originalQuery).append("\n");
		prompt.append("[Remove Tag Query] : ").append(fakeSql).append("\n");
		prompt.append("[Explain] : ").append(explainInfo).append("\n");
		prompt.append("[MetaData] : ").append(metaDataInfo).append("\n");

		// 출력 형식(Output Format) 지시: 목차 기반으로 구조적 답변 유도
		// 자유 형식 답변보다 목차를 주면 AI가 더 체계적이고 누락 없는 분석을 제공함
		prompt.append("[요청 사항]\n");
		prompt.append("다음 목차에 따라 분석 결과와 개선안을 제시해 줘:\n");
		prompt.append("1. 실행 계획 분석: 병목 구간(예: Full Table Scan, Filesort, 비효율적인 Join 등)과 원인 파악\n");
		prompt.append("2. 인덱스 최적화 제안: 메타데이터를 참고하여, 성능 향상을 위해 추가하거나 수정해야 할 DDL(CREATE INDEX 등)이 있다면 구체적인 이유와 함께 작성\n");
		prompt.append(
				"3. MyBatis 동적 쿼리 리뷰: [Original Query]의 <if>, <foreach> 등의 구조상, 파라미터 조건에 따라 성능 널뛰기나 인덱스 무력화가 발생할 위험이 있는지 점검\n");
		prompt.append("4. 개선된 쿼리 및 매퍼 제안: 최적화가 적용된 최종 SQL과 이를 반영한 MyBatis XML 코드를 작성\n");

		return prompt;
	}
}
