package io.github.sweetpark.sqlanalyzer.intellij.config;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class SqlAnalyzerConfigTest {

	@Test
	@DisplayName("jdbcUrl이 있으면 isConfigured()가 true를 반환한다")
	void isConfigured_withUrl_returnsTrue() {
		SqlAnalyzerConfig config = new SqlAnalyzerConfig("jdbc:h2:mem:test", "sa", "");
		assertTrue(config.isConfigured());
	}

	@Test
	@DisplayName("jdbcUrl이 null이면 isConfigured()가 false를 반환한다")
	void isConfigured_withNullUrl_returnsFalse() {
		SqlAnalyzerConfig config = new SqlAnalyzerConfig(null, "sa", "");
		assertFalse(config.isConfigured());
	}

	@Test
	@DisplayName("jdbcUrl이 빈 문자열이면 isConfigured()가 false를 반환한다")
	void isConfigured_withBlankUrl_returnsFalse() {
		SqlAnalyzerConfig config = new SqlAnalyzerConfig("   ", "sa", "");
		assertFalse(config.isConfigured());
	}

	@Test
	@DisplayName("생성자로 전달한 값이 getter를 통해 그대로 반환된다")
	void getters_returnConstructorValues() {
		SqlAnalyzerConfig config = new SqlAnalyzerConfig("jdbc:mariadb://localhost:3306/mydb", "root", "secret");

		assertEquals("jdbc:mariadb://localhost:3306/mydb", config.getJdbcUrl());
		assertEquals("root", config.getJdbcUser());
		assertEquals("secret", config.getJdbcPassword());
	}
}
