package io.github.sweetpark.sqltuner.intellij.service;

import io.github.sweetpark.sqltuner.core.PromptGenerator;
import io.github.sweetpark.sqltuner.core.SqlExtractor;
import io.github.sweetpark.sqltuner.intellij.config.SqlTunerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * IntelliJ 플러그인의 분석 오케스트레이터.
 *
 * <p>
 * core 모듈의 SqlExtractor, JdbcAnalyzer, PromptGenerator를 순서대로 호출하여 AI 쿼리 튜닝
 * 프롬프트를 생성한다.
 *
 * <p>
 * 이 클래스는 IntelliJ API(Project, ToolWindow 등)에 직접 의존하지 않으므로 순수 JUnit + H2로 단위
 * 테스트가 가능하다. UI 관련 처리(백그라운드 태스크, 다이얼로그)는 SqlTunerPanel이 담당한다.
 */
public class SqlTunerService {

	private static final Logger log = LoggerFactory.getLogger(SqlTunerService.class);

	/**
	 * 지정된 매퍼 파일의 queryId로 AI 프롬프트를 생성한다.
	 *
	 * <p>
	 * DB 연결 정보는 DbSettingsDialog가 IntelliJ PropertiesComponent에 저장한 값을
	 * SqlTunerConfig 값 객체로 전달받는다.
	 *
	 * @param config
	 *            DB 연결 정보 값 객체 (jdbcUrl, jdbcUser, jdbcPassword)
	 * @param selectedMapperFile
	 *            분석할 매퍼 XML 파일 경로
	 * @param mapperBaseDir
	 *            매퍼 XML 파일들의 루트 디렉토리 (상대 경로 해석 기준)
	 * @param queryId
	 *            분석할 MyBatis 쿼리 ID
	 * @return 생성된 AI 프롬프트 문자열
	 * @throws Exception
	 *             DB 연결 실패, 파일 파싱 실패, queryId 미존재 등 모든 분석 예외
	 */
	public String analyze(SqlTunerConfig config, Path selectedMapperFile, Path mapperBaseDir, String queryId)
			throws Exception {
		log.info("SQL 분석 시작 - queryId: {}, mapperFile: {}", queryId, selectedMapperFile);

		// JDBC 연결 생성 후 프롬프트 생성 (try-with-resources로 연결 자동 해제)
		// IntelliJ 플러그인은 별도 classloader를 사용하므로 DriverManager가 드라이버를
		// 자동 탐지하지 못한다. URL 기반으로 드라이버 클래스를 명시적으로 로드한다.
		loadJdbcDriver(config.getJdbcUrl());

		try (Connection connection = DriverManager.getConnection(config.getJdbcUrl(), config.getJdbcUser(),
				config.getJdbcPassword())) {

			return PromptGenerator.generatePrompt(connection, queryId, selectedMapperFile, mapperBaseDir).toString();
		}
	}

	/**
	 * JDBC URL 접두사를 보고 드라이버 클래스를 명시적으로 로드한다.
	 *
	 * <p>
	 * IntelliJ 플러그인의 classloader는 시스템 classloader와 분리되어 있어, DriverManager의
	 * ServiceLoader 기반 드라이버 자동 탐지가 동작하지 않는다. Class.forName()으로 플러그인 classloader에서
	 * 드라이버를 직접 로드하면 DriverManager에 등록되어 정상 연결이 가능해진다.
	 */
	private void loadJdbcDriver(String jdbcUrl) throws ClassNotFoundException {
		if (jdbcUrl.startsWith("jdbc:mariadb:") || jdbcUrl.startsWith("jdbc:mysql:")) {
			Class.forName("org.mariadb.jdbc.Driver");
		} else if (jdbcUrl.startsWith("jdbc:postgresql:")) {
			Class.forName("org.postgresql.Driver");
		} else if (jdbcUrl.startsWith("jdbc:h2:")) {
			Class.forName("org.h2.Driver");
		} else {
			log.warn("알 수 없는 JDBC URL 형식입니다. 드라이버를 수동으로 로드하지 않습니다: {}", jdbcUrl);
		}
	}

	/**
	 * 지정된 매퍼 디렉토리에서 queryId를 포함하는 파일 목록을 반환한다.
	 *
	 * @param mapperBaseDir
	 *            매퍼 XML 파일들의 루트 디렉토리
	 * @param queryId
	 *            검색할 MyBatis 쿼리 ID
	 * @return queryId를 포함하는 파일 경로 목록 (없으면 빈 List)
	 */
	public List<Path> findMatchingFiles(Path mapperBaseDir, String queryId) throws Exception {
		return SqlExtractor.findMapperFiles(mapperBaseDir, queryId);
	}

	/**
	 * 매퍼 베이스 디렉토리 아래의 모든 XML 파일을 재귀 탐색하여 베이스 디렉토리 기준 상대 경로 문자열 목록을 알파벳순으로 반환한다.
	 *
	 * <p>
	 * 단순 1-depth 탐색 방식은 {@code payment/approval/ApprovalMapper.xml} 처럼 여러 depth로
	 * 중첩된 디렉토리 구조를 지원할 수 없다. {@code Files.walk()}로 전체를 재귀 탐색하고, 상대 경로를 표시함으로써
	 * depth에 무관하게 모든 파일을 단일 목록으로 제공한다.
	 *
	 * <p>
	 * 반환 예시 (OS separator 기준):
	 *
	 * <pre>
	 *   payment/approval/ApprovalMapper.xml
	 *   payment/cancel/CancelMapper.xml
	 *   user/UserMapper.xml
	 * </pre>
	 *
	 * @param mapperBaseDir
	 *            매퍼 XML 파일들의 루트 디렉토리
	 * @return 상대 경로 문자열 목록 (알파벳순, 없으면 빈 리스트)
	 */
	public List<String> listXmlFiles(Path mapperBaseDir) throws IOException {
		if (!Files.isDirectory(mapperBaseDir)) {
			return List.of();
		}

		try (Stream<Path> stream = Files.walk(mapperBaseDir)) {
			return stream.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".xml"))
					// 베이스 디렉토리 기준 상대 경로로 변환 — depth에 무관하게 표시
					.map(p -> mapperBaseDir.relativize(p).toString()).sorted().collect(Collectors.toList());
		}
	}
}
