package io.github.sweetpark.sqltuner.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.util.TablesNamesFinder;
import java.sql.*;
import java.util.Set;

public class JdbcAnalyzer {

	private static final Logger log = LoggerFactory.getLogger(JdbcAnalyzer.class);

	/** 유틸리티 클래스 — 인스턴스 생성 불가 */
	private JdbcAnalyzer() {
	}

	/**
	 * 쿼리에 사용된 테이블들의 컬럼 정보와 인덱스 정보를 수집하여 텍스트로 반환한다.
	 *
	 * <p>
	 * MariaDB/MySQL의 경우 {@code SHOW VARIABLES}로 트랜잭션 격리 수준과 sql_mode를 함께 출력한다.
	 *
	 * <p>
	 * catalog에 {@code null}을 전달하면 서버의 전체 데이터베이스를 대상으로 조회되어 동일 테이블명이 여러 DB에 존재할 경우
	 * 정보가 중복 출력된다. {@code metaData.getConnection().getCatalog()}로 현재 접속 DB를 명시하여
	 * 범위를 한정한다.
	 *
	 * @param tables
	 *            분석 대상 테이블명 집합 (SQL에서 파싱된 값)
	 * @param metaData
	 *            현재 JDBC 연결의 DatabaseMetaData
	 * @return DATABASE INFO + TABLE INFO(컬럼) + INDEX INFO 를 포함한 텍스트
	 * @throws SQLException
	 *             메타데이터 조회 실패 시
	 */
	public static StringBuilder getMetaDataInfo(Set<String> tables, DatabaseMetaData metaData) throws SQLException {
		StringBuilder result = new StringBuilder();

		// 데이터베이스 정보 저장
		result.append("==============================\n");
		result.append("[DATABASE INFO] : ").append("(ProductName)").append(metaData.getDatabaseProductName())
				.append("\n");
		result.append("(ProductVersion)").append(metaData.getDatabaseProductVersion()).append("\n");
		result.append("(DriverName)").append(metaData.getDriverName()).append("\n");
		result.append("(DriverVersion)").append(metaData.getDriverVersion()).append("\n");

		if (metaData.getDatabaseProductName().equalsIgnoreCase("MariaDB")
				|| metaData.getDatabaseProductName().equalsIgnoreCase("MySQL")) {

			Connection connection = metaData.getConnection();
			try (Statement stmt = connection.createStatement();
					ResultSet rs = stmt.executeQuery(
							"SHOW VARIABLES WHERE Variable_name IN ('tx_isolation', 'transaction_isolation', 'sql_mode')")) {
				while (rs.next()) {
					result.append(rs.getString("Variable_name")).append(rs.getString("Value"));
				}
			}
		}

		// catalog=null 이면 서버의 모든 데이터베이스를 대상으로 조회되어
		// 동일 테이블명이 여러 DB에 존재할 경우 컬럼·인덱스 정보가 중복 출력된다.
		// 현재 접속 중인 DB(catalog)를 명시적으로 지정하여 조회 범위를 한정한다.
		String catalog = metaData.getConnection().getCatalog();

		for (String table : tables) {
			String targetTable = table.toUpperCase();

			result.append("============================\n");
			result.append("[TABLE INFO] : ").append(targetTable).append("\n");

			// 2. 테이블 컬럼 정보 추출 (쿼리 실행 대신 getColumns API 사용)
			// catalog 를 현재 DB로 한정 — null 이면 모든 DB 검색으로 중복 출력됨
			try (ResultSet rs = metaData.getColumns(catalog, null, targetTable, null)) {
				while (rs.next()) {
					String columnName = rs.getString("COLUMN_NAME");
					String typeName = rs.getString("TYPE_NAME");
					String columnSize = rs.getString("COLUMN_SIZE");

					result.append(" - ").append(columnName).append(" (Type: ").append(typeName).append(", Size: ")
							.append(columnSize).append(")\n");
				}
			}

			result.append("\n[INDEX INFO] : ").append(targetTable).append("\n");

			// 3. 테이블 인덱스 정보 추출 (getIndexInfo API 사용)
			// 파라미터: catalog, schema, table, unique(false면 모든 인덱스), approximate
			// catalog 를 현재 DB로 한정 — null 이면 모든 DB 검색으로 중복 출력됨
			try (ResultSet rs = metaData.getIndexInfo(catalog, null, targetTable, false, false)) {
				while (rs.next()) {
					String indexName = rs.getString("INDEX_NAME");

					// 테이블 기본 통계 정보(IndexName이 null)는 건너뜁니다.
					if (indexName == null)
						continue;

					String columnName = rs.getString("COLUMN_NAME");
					boolean isUnique = !rs.getBoolean("NON_UNIQUE");

					result.append(" - Index: ").append(indexName).append(", Column: ").append(columnName)
							.append(", Unique: ").append(isUnique).append("\n");
				}
			}
			result.append("============================\n\n");
		}
		return result;
	}

	/**
	 * SQL 문자열을 파싱하여 사용된 테이블명 집합을 반환한다.
	 *
	 * <p>
	 * JSQLParser의 {@link TablesNamesFinder}를 사용하여 FROM, JOIN, 서브쿼리 등 모든 절에서 참조되는
	 * 테이블명을 추출한다.
	 *
	 * @param fakeSql
	 *            파싱할 SQL 문자열 (MyBatis #{}, ${} 바인딩이 '?'로 치환된 fakeSql)
	 * @return SQL에서 참조된 테이블명 집합 (파라미터가 null이거나 빈 경우 빈 집합)
	 * @throws JSQLParserException
	 *             SQL 파싱 실패 시
	 */
	public static Set<String> extractTableMethod(String fakeSql) throws JSQLParserException {
		net.sf.jsqlparser.statement.Statement stmt = null;
		TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();

		if (fakeSql != null) {
			stmt = CCJSqlParserUtil.parse(fakeSql);
			return tablesNamesFinder.getTables(stmt);
		}

		return Set.of();
	}

	/**
	 * 지정된 SQL에 대한 EXPLAIN 실행 결과를 반환한다.
	 *
	 * <p>
	 * EXPLAIN은 DB 실행 계획을 보여주며, 인덱스 활용 여부, Full Table Scan 여부 등 성능 분석에 필요한 정보를 포함한다.
	 * 결과는 '컬럼헤더 | 구분선 | 데이터 행' 형태의 텍스트로 반환한다.
	 *
	 * <p>
	 * 이전 구현의 버그: {@code for (int i=1; rs.next(); i++) { rs.getString(i) }} 형태는 i가 행
	 * 번호인데 컬럼 인덱스로 오용됨. MySQL EXPLAIN처럼 여러 컬럼이 있는 경우 첫 번째 행의 첫 번째 컬럼만 출력되고 이후 데이터가
	 * 모두 누락되었음.
	 *
	 * <p>
	 * 바인드 파라미터 처리 방식: MariaDB에서 {@code LIMIT ?}에 {@code setNull(i, Types.NULL)}을
	 * 바인딩하면 "syntax error near 'null, null'" 오류가 발생한다. EXPLAIN은 실행 계획(인덱스·조인 방식)
	 * 조회가 목적이므로 실제 값은 중요하지 않다. 모든 파라미터에 더미 정수 1을 바인딩하면 MariaDB/MySQL/H2 등 모든 DB에서
	 * 안전하게 동작한다.
	 *
	 * @param connection
	 *            DB 연결 객체 (호출자가 생명주기를 관리해야 함 — try-with-resources 권장)
	 * @param fakeSql
	 *            실행할 SQL (MyBatis #{}, ${} 바인딩이 '?'로 치환된 fakeSql)
	 * @return EXPLAIN 결과 텍스트 (컬럼헤더 + 구분선 + 데이터 행)
	 * @throws SQLException
	 *             EXPLAIN 실행 실패 시
	 */
	public static String getExplainInfo(Connection connection, String fakeSql) throws SQLException {
		// PreparedStatement를 사용해 ?를 JDBC 바인드 파라미터로 처리한다.
		// 문자열 치환(replaceAll) 방식은 SQL 리터럴 내부의 ?까지 변경할 위험이 있고
		// JDBC 표준에서 벗어난다. PreparedStatement는 파서가 ?의 위치를 정확히 파악하므로 안전하다.
		// EXPLAIN은 실행 계획(인덱스·조인 방식)이 목적이므로 더미값 바인딩으로 충분하다.
		try (PreparedStatement pstmt = connection.prepareStatement("EXPLAIN " + fakeSql)) {
			int paramCount = countParams(fakeSql);

			for (int i = 1; i <= paramCount; i++) {
				// setNull(Types.NULL) 은 MariaDB의 LIMIT ? 에 NULL 을 바인딩하여
				// "syntax error near 'null, null'" 오류를 유발한다.
				// EXPLAIN 목적상 실제 값이 불필요하므로 더미 정수 1 로 대체한다.
				pstmt.setObject(i, 1);
			}

			try (ResultSet rs = pstmt.executeQuery()) {
				ResultSetMetaData meta = rs.getMetaData();
				int columnCount = meta.getColumnCount();
				StringBuilder result = new StringBuilder();

				// 헤더 행: 컬럼명을 '|' 구분자로 나열
				for (int col = 1; col <= columnCount; col++) {
					result.append(meta.getColumnName(col));
					if (col < columnCount) {
						result.append(" | ");
					}
				}
				result.append("\n");
				result.append("-".repeat(60)).append("\n");

				// 데이터 행: 각 행의 모든 컬럼 값을 '|' 구분자로 출력
				// null 값은 "NULL" 문자열로 표시 (DB의 NULL과 구분 불가한 빈 문자열 방지)
				while (rs.next()) {
					for (int col = 1; col <= columnCount; col++) {
						String value = rs.getString(col);
						result.append(value != null ? value : "NULL");
						if (col < columnCount) {
							result.append(" | ");
						}
					}
					result.append("\n");
				}

				return result.toString();
			}
		}
	}

	/**
	 * SQL 문자열에서 바인드 파라미터 '?'의 개수를 반환한다.
	 *
	 * <p>
	 * 문자열 리터럴 내부의 ?는 카운트하지 않는 완전한 구현이 이상적이나, buildFakeSql()이 생성하는 fakeSql은 문자열 리터럴이
	 * 없으므로 단순 카운트로 충분하다.
	 */
	private static int countParams(String sql) {
		int count = 0;

		for (char c : sql.toCharArray()) {
			if (c == '?') {
				count++;
			}
		}

		return count;
	}
}
