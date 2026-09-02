package io.github.sweetpark.sqlanalyzer.core;

import org.junit.jupiter.api.*;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class PromptGeneratorTest {

	private Connection connection;
	private final String queryId = "findBadPerformancePayments";
	private final Path mapperPath = Path.of("src/test/resources/mapper/TestMapper.xml");
	private final Path mapperPathDir = Path.of("src/test/resources/mapper");

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
	@DisplayName("Private constructor invocation via reflection")
	void privateConstructor() throws Exception {
		Constructor<PromptGenerator> constructor = PromptGenerator.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		PromptGenerator instance = constructor.newInstance();
		assertNotNull(instance);
	}

	@Test
	@DisplayName("Prompt 생성 성공")
	void printPrompt() throws Exception {
		StringBuilder prompt = PromptGenerator.generatePrompt(connection, queryId, mapperPath, mapperPathDir);
		assertNotNull(prompt);
		assertTrue(prompt.toString().contains("[Original Query]"));
		assertTrue(prompt.toString().contains("[Remove Tag Query]"));
		assertTrue(prompt.toString().contains("[Explain]"));
		assertTrue(prompt.toString().contains("[MetaData]"));
	}

	@Test
	@DisplayName("존재하지 않는 queryId로 프롬프트 생성 시 fakeSql이 null인 케이스")
	void generatePrompt_unknownQueryId() throws Exception {
		StringBuilder prompt = PromptGenerator.generatePrompt(connection, "nonExistentQuery", mapperPath,
				mapperPathDir);
		assertNotNull(prompt);
		assertTrue(prompt.toString().contains("[Original Query] : "));
		assertTrue(prompt.toString().contains("[Remove Tag Query] : null"));
	}
}
