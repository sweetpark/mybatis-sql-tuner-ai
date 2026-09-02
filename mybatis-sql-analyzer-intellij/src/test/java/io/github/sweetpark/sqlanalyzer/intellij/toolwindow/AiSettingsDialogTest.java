package io.github.sweetpark.sqlanalyzer.intellij.toolwindow;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import io.github.sweetpark.sqlanalyzer.intellij.config.AiSettingsConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class AiSettingsDialogTest {

	@Test
	@DisplayName("상수 기본값 확인")
	void constants_check() {
		assertEquals("http://localhost:11434/v1", AiSettingsDialog.DEFAULT_BASE_URL);
		assertEquals("qwen2.5-coder:7b", AiSettingsDialog.DEFAULT_MODEL);
		assertEquals("", AiSettingsDialog.DEFAULT_API_KEY);
	}

	@Test
	@DisplayName("loadConfig - 기본값 및 커스텀 설정값 반환 검증")
	void loadConfig_test() {
		TestPropertiesComponent props = new TestPropertiesComponent();
		props.setValue(AiSettingsDialog.KEY_BASE_URL, "https://api.openai.com/v1");
		props.setValue(AiSettingsDialog.KEY_MODEL, "gpt-4o-mini");
		props.setValue(AiSettingsDialog.KEY_API_KEY, "sk-test1234");

		Project mockProject = (Project) Proxy.newProxyInstance(AiSettingsDialogTest.class.getClassLoader(),
				new Class<?>[]{Project.class}, (proxy, method, args) -> {
					if ("getService".equals(method.getName()) && PropertiesComponent.class.equals(args[0])) {
						return props;
					}
					return null;
				});

		AiSettingsConfig config = AiSettingsDialog.loadConfig(mockProject);
		assertEquals("https://api.openai.com/v1", config.getBaseUrl());
		assertEquals("gpt-4o-mini", config.getModel());
		assertEquals("sk-test1234", config.getApiKey());
	}
}
