package io.github.sweetpark.sqltuner.intellij.service;

import io.github.sweetpark.sqltuner.intellij.config.SqlTunerConfig;
import org.junit.jupiter.api.*;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SqlTunerServiceTest {

	private Connection connection;
	private static final String queryId = "findBadPerformancePayments";
	private final Path mapperPath = Path.of("../mybatis-sql-tuner-core/src/test/resources/mapper/TestMapper.xml");
	private final Path mapperPathDir = Path.of("../mybatis-sql-tuner-core/src/test/resources/mapper");

	@BeforeEach
	void set() throws Exception {
		connection = DriverManager.getConnection("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");

		try (Statement stmt = connection.createStatement()) {
			stmt.execute(
					"CREATE TABLE payments (payment_id VARCHAR(50) PRIMARY KEY, order_id VARCHAR(50) NOT NULL, amount DECIMAL(10, 2) NOT NULL, status VARCHAR(20) NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
			stmt.execute(
					"CREATE TABLE orders (order_id VARCHAR(50) PRIMARY KEY, user_id VARCHAR(50) NOT NULL, mid VARCHAR(10) NOT NULL)");
			stmt.execute(
					"CREATE TABLE merchants (mid VARCHAR(10) PRIMARY KEY, merchant_name VARCHAR(100) NOT NULL, status VARCHAR(20) DEFAULT 'ACTIVE', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
			stmt.execute(
					"CREATE TABLE refunds (refund_id VARCHAR(50) PRIMARY KEY, payment_id VARCHAR(50) NOT NULL, refund_amount DECIMAL(10, 2) NOT NULL, reason VARCHAR(255), refunded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

			stmt.execute("CREATE INDEX payments_amount_idx ON payments(amount)");
			stmt.execute("CREATE INDEX orders_user_id_idx ON orders(user_id)");
			stmt.execute("CREATE INDEX idx_merchants_status ON merchants(status)");
			stmt.execute("CREATE INDEX idx_refunds_payment_id ON refunds(payment_id)");
		}
	}

	@AfterEach
	void remove() throws Exception {
		if (connection != null && !connection.isClosed()) {
			try (Statement stmt = connection.createStatement()) {
				stmt.execute("DROP ALL OBJECTS");
			}
			connection.close();
		}
	}

	@Test
	@DisplayName("analyze - 정상 실행 및 프롬프트 결과 반환")
	void analyze_success() throws Exception {
		SqlTunerService service = new SqlTunerService();
		SqlTunerConfig config = new SqlTunerConfig("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");

		String result = service.analyze(config, mapperPath, mapperPathDir, queryId);
		assertNotNull(result);
		assertTrue(result.contains("[Original Query]"));
		assertTrue(result.contains("[Remove Tag Query]"));
		assertTrue(result.contains("[Explain]"));
		assertTrue(result.contains("[MetaData]"));
	}

	@Test
	@DisplayName("findMatchingFiles - queryId를 포함하는 매퍼 파일 검색")
	void findMatchingFiles_success() throws Exception {
		SqlTunerService service = new SqlTunerService();
		List<Path> matchingFiles = service.findMatchingFiles(mapperPathDir, queryId);
		assertFalse(matchingFiles.isEmpty());
	}

	@Test
	@DisplayName("listXmlFiles - XML 파일 목록 재귀 탐색")
	void listXmlFiles_success() throws Exception {
		Path tempDir = Files.createTempDirectory("xml-test");
		try {
			Path subDir = Files.createDirectory(tempDir.resolve("sub"));
			Files.writeString(tempDir.resolve("root.xml"), "<mapper/>");
			Files.writeString(subDir.resolve("child.xml"), "<mapper/>");
			Files.writeString(tempDir.resolve("other.txt"), "text");

			SqlTunerService service = new SqlTunerService();
			List<String> xmls = service.listXmlFiles(tempDir);
			assertEquals(2, xmls.size());

			// Non-directory test
			List<String> emptyXmls = service.listXmlFiles(tempDir.resolve("non_existent"));
			assertTrue(emptyXmls.isEmpty());
		} finally {
			try (Stream<Path> stream = Files.walk(tempDir)) {
				stream.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			}
		}
	}

	@Test
	@DisplayName("loadJdbcDriver - JDBC URL 별 드라이버 로딩 테스트")
	void loadJdbcDriver_allBranches() throws Exception {
		SqlTunerService service = new SqlTunerService();
		Method method = SqlTunerService.class.getDeclaredMethod("loadJdbcDriver", String.class);
		method.setAccessible(true);

		// Test MariaDB
		assertDoesNotThrow(() -> method.invoke(service, "jdbc:mariadb://localhost:3306/db"));
		// Test MySQL
		assertDoesNotThrow(() -> method.invoke(service, "jdbc:mysql://localhost:3306/db"));
		// Test PostgreSQL
		assertDoesNotThrow(() -> method.invoke(service, "jdbc:postgresql://localhost:5432/db"));
		// Test H2
		assertDoesNotThrow(() -> method.invoke(service, "jdbc:h2:mem:test"));
		// Test unknown URL (no exception)
		assertDoesNotThrow(() -> method.invoke(service, "jdbc:oracle:thin:@localhost:1521:xe"));
	}
}
