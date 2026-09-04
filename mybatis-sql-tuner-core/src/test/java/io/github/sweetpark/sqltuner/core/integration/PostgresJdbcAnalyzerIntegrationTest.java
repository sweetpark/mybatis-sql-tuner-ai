package io.github.sweetpark.sqltuner.core.integration;

import io.github.sweetpark.sqltuner.core.JdbcAnalyzer;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
@Testcontainers
class PostgresJdbcAnalyzerIntegrationTest {

	@Container
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
			.withDatabaseName("tuner_test").withUsername("tuner").withPassword("tuner");

	private Connection connection;

	@BeforeEach
	void setUp() throws Exception {
		connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
		try (Statement stmt = connection.createStatement()) {
			stmt.execute("CREATE TABLE payments (payment_id VARCHAR(50) PRIMARY KEY, amount DECIMAL(10,2) NOT NULL)");
			stmt.execute("CREATE INDEX payments_amount_idx ON payments(amount)");
		}
	}

	@AfterEach
	void tearDown() throws Exception {
		if (connection != null && !connection.isClosed()) {
			try (Statement stmt = connection.createStatement()) {
				stmt.execute("DROP TABLE payments");
			}
			connection.close();
		}
	}

	@Test
	@DisplayName("PostgreSQL 실컨테이너 - 소문자로 저장된 테이블의 컬럼/인덱스 메타데이터를 조회해야 함")
	void metaDataInfo_realPostgres() throws Exception {
		DatabaseMetaData metaData = connection.getMetaData();
		StringBuilder result = JdbcAnalyzer.getMetaDataInfo(Set.of("payments"), metaData);

		assertTrue(result.toString().contains("PostgreSQL"));
		assertTrue(result.toString().contains("payment_id"));
		assertTrue(result.toString().contains("payments_amount_idx"));
	}

	@Test
	@DisplayName("PostgreSQL 실컨테이너 - JdbcAnalyzer의 EXPLAIN 접두사가 그대로 동작해야 함")
	void explainInfo_plainExplainWorks() throws Exception {
		String fakeSql = "SELECT payment_id FROM payments WHERE amount > ?";
		String result = JdbcAnalyzer.getExplainInfo(connection, fakeSql);

		assertNotNull(result);
		assertTrue(result.toLowerCase().contains("scan"), "PostgreSQL EXPLAIN 결과에 Scan 계획이 포함되어야 합니다.");
	}

	@Test
	@DisplayName("PostgreSQL 고유 옵션 - EXPLAIN (FORMAT TEXT)를 직접 실행해도 문제없이 동작해야 함(방언 호환성 확인)")
	void explainWithFormatOption_isAccepted() throws Exception {
		try (PreparedStatement pstmt = connection
				.prepareStatement("EXPLAIN (FORMAT TEXT) SELECT payment_id FROM payments WHERE amount > ?")) {
			pstmt.setObject(1, 1);
			try (ResultSet rs = pstmt.executeQuery()) {
				assertTrue(rs.next(), "FORMAT TEXT 옵션을 붙인 EXPLAIN도 정상적으로 결과를 반환해야 합니다.");
			}
		}
	}
}
