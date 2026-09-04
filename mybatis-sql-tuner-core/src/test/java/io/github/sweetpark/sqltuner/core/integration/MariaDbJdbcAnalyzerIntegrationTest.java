package io.github.sweetpark.sqltuner.core.integration;

import io.github.sweetpark.sqltuner.core.JdbcAnalyzer;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.MariaDBContainer;
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
class MariaDbJdbcAnalyzerIntegrationTest {

	@Container
	static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>("mariadb:11").withDatabaseName("tuner_test")
			.withUsername("tuner").withPassword("tuner");

	private Connection connection;

	@BeforeEach
	void setUp() throws Exception {
		connection = DriverManager.getConnection(MARIADB.getJdbcUrl(), MARIADB.getUsername(), MARIADB.getPassword());
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
	@DisplayName("MariaDB 실컨테이너 - 소문자 테이블의 컬럼/인덱스 메타데이터 및 sql_mode 조회가 동작해야 함")
	void metaDataInfo_realMariaDb() throws Exception {
		DatabaseMetaData metaData = connection.getMetaData();
		StringBuilder result = JdbcAnalyzer.getMetaDataInfo(Set.of("payments"), metaData);

		assertTrue(result.toString().contains("MariaDB"));
		assertTrue(result.toString().contains("payment_id"));
		assertTrue(result.toString().contains("payments_amount_idx"));
		// MariaDB/MySQL 분기: SHOW VARIABLES 결과(sql_mode)가 포함되어야 함
		assertTrue(result.toString().toLowerCase().contains("sql_mode"));
	}

	@Test
	@DisplayName("MariaDB 실컨테이너 - LIMIT 바인드 파라미터가 있는 쿼리의 EXPLAIN이 NULL 바인딩 오류 없이 동작해야 함")
	void explainInfo_limitBindDoesNotThrow() throws Exception {
		String fakeSql = "SELECT payment_id FROM payments WHERE amount > ? LIMIT ?";
		String result = JdbcAnalyzer.getExplainInfo(connection, fakeSql);

		assertNotNull(result);
		assertFalse(result.isBlank());
		assertTrue(result.contains("payments"));
	}
}
