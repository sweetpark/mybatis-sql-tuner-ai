package io.github.sweetpark.sqltuner.core.integration;

import io.github.sweetpark.sqltuner.core.JdbcAnalyzer;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
@Testcontainers
class MySqlJdbcAnalyzerIntegrationTest {

	@Container
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0").withDatabaseName("tuner_test")
			.withUsername("tuner").withPassword("tuner");

	private Connection connection;

	@BeforeEach
	void setUp() throws Exception {
		connection = DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
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
	@DisplayName("MySQL 실컨테이너 - 소문자로 생성된 테이블의 컬럼/인덱스 메타데이터를 조회해야 함")
	void metaDataInfo_realMySql() throws Exception {
		DatabaseMetaData metaData = connection.getMetaData();
		StringBuilder result = JdbcAnalyzer.getMetaDataInfo(Set.of("payments"), metaData);

		assertTrue(result.toString().contains("MySQL"));
		assertTrue(result.toString().contains("payment_id"));
		assertTrue(result.toString().contains("payments_amount_idx"));
	}

	@Test
	@DisplayName("MySQL 실컨테이너 - EXPLAIN 결과에 표준 컬럼(id, select_type, table, key, rows, Extra)이 파싱되어야 함")
	void explainInfo_parsesStandardMySqlColumns() throws Exception {
		String fakeSql = "SELECT payment_id, amount FROM payments WHERE amount > ?";
		String result = JdbcAnalyzer.getExplainInfo(connection, fakeSql);

		assertNotNull(result);
		assertTrue(result.contains("id"));
		assertTrue(result.contains("select_type"));
		assertTrue(result.contains("table"));
		assertTrue(result.contains("key"));
		assertTrue(result.contains("rows"));
		assertTrue(result.contains("Extra"));
		assertTrue(result.contains("payments"));
	}
}
