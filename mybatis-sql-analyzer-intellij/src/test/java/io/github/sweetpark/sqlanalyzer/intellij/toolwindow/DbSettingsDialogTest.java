package io.github.sweetpark.sqlanalyzer.intellij.toolwindow;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import io.github.sweetpark.sqlanalyzer.intellij.config.SecretStore;
import io.github.sweetpark.sqlanalyzer.intellij.config.SqlAnalyzerConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class DbSettingsDialogTest {

	@Test
	@DisplayName("상수 키 확인")
	void constants_check() {
		assertEquals("sql-analyzer.jdbc.url", DbSettingsDialog.KEY_JDBC_URL);
		assertEquals("sql-analyzer.jdbc.user", DbSettingsDialog.KEY_JDBC_USER);
		assertEquals("sql-analyzer.jdbc.password", DbSettingsDialog.KEY_JDBC_PASSWORD);
	}

	@Test
	@DisplayName("loadConfig - 커스텀 설정값 반환 검증 (URL/User는 PropertiesComponent, 비밀번호는 SecretStore)")
	void loadConfig_test() {
		TestPropertiesComponent props = new TestPropertiesComponent();
		props.setValue(DbSettingsDialog.KEY_JDBC_URL, "jdbc:mariadb://localhost:3306/testdb");
		props.setValue(DbSettingsDialog.KEY_JDBC_USER, "root");

		TestSecretStore secretStore = new TestSecretStore();
		secretStore.setSecret(DbSettingsDialog.KEY_JDBC_PASSWORD, "pass123");

		Project mockProject = mockProject(props, secretStore);

		SqlAnalyzerConfig config = DbSettingsDialog.loadConfig(mockProject);
		assertEquals("jdbc:mariadb://localhost:3306/testdb", config.getJdbcUrl());
		assertEquals("root", config.getJdbcUser());
		assertEquals("pass123", config.getJdbcPassword());
	}

	@Test
	@DisplayName("loadConfig - 레거시 PropertiesComponent 평문 비밀번호를 SecretStore로 1회 마이그레이션")
	void loadConfig_migratesLegacyPlaintextPassword() {
		TestPropertiesComponent props = new TestPropertiesComponent();
		props.setValue(DbSettingsDialog.KEY_JDBC_URL, "jdbc:mariadb://localhost:3306/testdb");
		props.setValue(DbSettingsDialog.KEY_JDBC_USER, "root");
		// 과거 버전이 남긴 평문 비밀번호
		props.setValue(DbSettingsDialog.KEY_JDBC_PASSWORD, "legacyPass");

		TestSecretStore secretStore = new TestSecretStore();

		Project mockProject = mockProject(props, secretStore);

		SqlAnalyzerConfig config = DbSettingsDialog.loadConfig(mockProject);

		assertEquals("legacyPass", config.getJdbcPassword());
		assertEquals("legacyPass", secretStore.getSecret(DbSettingsDialog.KEY_JDBC_PASSWORD));
		assertFalse(props.isValueSet(DbSettingsDialog.KEY_JDBC_PASSWORD));
	}

	private static Project mockProject(TestPropertiesComponent props, TestSecretStore secretStore) {
		return (Project) Proxy.newProxyInstance(DbSettingsDialogTest.class.getClassLoader(),
				new Class<?>[]{Project.class}, (proxy, method, args) -> {
					if ("getService".equals(method.getName())) {
						if (PropertiesComponent.class.equals(args[0])) {
							return props;
						}
						if (SecretStore.class.equals(args[0])) {
							return secretStore;
						}
					}
					return null;
				});
	}
}
