package io.github.sweetpark.sqlanalyzer.intellij.toolwindow;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
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
	@DisplayName("loadConfig - 커스텀 설정값 반환 검증")
	void loadConfig_test() {
		TestPropertiesComponent props = new TestPropertiesComponent();
		props.setValue(DbSettingsDialog.KEY_JDBC_URL, "jdbc:mariadb://localhost:3306/testdb");
		props.setValue(DbSettingsDialog.KEY_JDBC_USER, "root");
		props.setValue(DbSettingsDialog.KEY_JDBC_PASSWORD, "pass123");

		Project mockProject = (Project) Proxy.newProxyInstance(DbSettingsDialogTest.class.getClassLoader(),
				new Class<?>[]{Project.class}, (proxy, method, args) -> {
					if ("getService".equals(method.getName()) && PropertiesComponent.class.equals(args[0])) {
						return props;
					}
					return null;
				});

		SqlAnalyzerConfig config = DbSettingsDialog.loadConfig(mockProject);
		assertEquals("jdbc:mariadb://localhost:3306/testdb", config.getJdbcUrl());
		assertEquals("root", config.getJdbcUser());
		assertEquals("pass123", config.getJdbcPassword());
	}
}
