package io.github.sweetpark.sqlanalyzer.intellij.config;

/**
 * DB 연결 정보를 담는 값 객체. 저장/로드는 DbSettingsDialog가 담당한다 (URL/User는
 * PropertiesComponent, 비밀번호는 {@link SecretStore}를 통해).
 */
public class SqlAnalyzerConfig {

	private final String jdbcUrl;
	private final String jdbcUser;
	private final String jdbcPassword;

	public SqlAnalyzerConfig(String jdbcUrl, String jdbcUser, String jdbcPassword) {
		this.jdbcUrl = jdbcUrl;
		this.jdbcUser = jdbcUser;
		this.jdbcPassword = jdbcPassword;
	}

	/** DB 설정이 입력된 상태인지 확인 (jdbcUrl이 비어있지 않으면 configured로 판단) */
	public boolean isConfigured() {
		return jdbcUrl != null && !jdbcUrl.isBlank();
	}

	public String getJdbcUrl() {
		return jdbcUrl;
	}
	public String getJdbcUser() {
		return jdbcUser;
	}
	public String getJdbcPassword() {
		return jdbcPassword;
	}
}
