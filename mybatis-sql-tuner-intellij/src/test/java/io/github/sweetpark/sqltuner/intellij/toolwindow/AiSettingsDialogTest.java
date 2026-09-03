package io.github.sweetpark.sqltuner.intellij.toolwindow;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import io.github.sweetpark.sqltuner.intellij.config.AiSettingsConfig;
import io.github.sweetpark.sqltuner.intellij.config.SecretStore;
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
	@DisplayName("loadConfig - 커스텀 설정값 반환 검증 (Base URL/Model은 PropertiesComponent, API Key는 SecretStore)")
	void loadConfig_test() {
		TestPropertiesComponent props = new TestPropertiesComponent();
		props.setValue(AiSettingsDialog.KEY_BASE_URL, "https://api.openai.com/v1");
		props.setValue(AiSettingsDialog.KEY_MODEL, "gpt-4o-mini");

		TestSecretStore secretStore = new TestSecretStore();
		secretStore.setSecret(AiSettingsDialog.KEY_API_KEY, "sk-test1234");

		Project mockProject = mockProject(props, secretStore);

		AiSettingsConfig config = AiSettingsDialog.loadConfig(mockProject);
		assertEquals("https://api.openai.com/v1", config.getBaseUrl());
		assertEquals("gpt-4o-mini", config.getModel());
		assertEquals("sk-test1234", config.getApiKey());
	}

	@Test
	@DisplayName("loadConfig - 레거시 PropertiesComponent 평문 API Key를 SecretStore로 1회 마이그레이션")
	void loadConfig_migratesLegacyPlaintextApiKey() {
		TestPropertiesComponent props = new TestPropertiesComponent();
		// 과거 버전이 남긴 평문 API Key
		props.setValue(AiSettingsDialog.KEY_API_KEY, "sk-legacy");

		TestSecretStore secretStore = new TestSecretStore();

		Project mockProject = mockProject(props, secretStore);

		AiSettingsConfig config = AiSettingsDialog.loadConfig(mockProject);

		assertEquals("sk-legacy", config.getApiKey());
		assertEquals("sk-legacy", secretStore.getSecret(AiSettingsDialog.KEY_API_KEY));
		assertFalse(props.isValueSet(AiSettingsDialog.KEY_API_KEY));
	}

	private static Project mockProject(TestPropertiesComponent props, TestSecretStore secretStore) {
		return (Project) Proxy.newProxyInstance(AiSettingsDialogTest.class.getClassLoader(),
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
